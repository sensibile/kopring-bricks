package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Instant
import java.util.function.Supplier
import kotlin.test.Test

class OutboxAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(OutboxAutoConfiguration::class.java)

    @Test
    fun `creates logging repository and appender without jdbc client`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(OutboxProperties::class.java)
            assertThat(context).hasSingleBean(OutboxEventRepository::class.java)
            assertThat(context).hasSingleBean(OutboxEventAppender::class.java)
            assertThat(context).hasSingleBean(OutboxRetryPolicy::class.java)
            assertThat(context).hasSingleBean(LoggingOutboxEventRepository::class.java)
            assertThat(context).doesNotHaveBean(OutboxEventPublisher::class.java)
            assertThat(context).doesNotHaveBean(OutboxPollingService::class.java)
        }
    }

    @Test
    fun `creates jdbc repository when datasource url is postgresql`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues("spring.datasource.url=jdbc:postgresql://localhost:5432/app")
            .run { context ->
                assertThat(context).hasSingleBean(OutboxEventRepository::class.java)
                assertThat(context).hasSingleBean(JdbcOutboxEventRepository::class.java)
                assertThat(context).doesNotHaveBean(LoggingOutboxEventRepository::class.java)
            }
    }

    @Test
    fun `creates jdbc repository when dialect is explicitly postgresql`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues("kopring.bricks.outbox.jdbc.dialect=postgresql")
            .run { context ->
                assertThat(context).hasSingleBean(OutboxEventRepository::class.java)
                assertThat(context).hasSingleBean(JdbcOutboxEventRepository::class.java)
                assertThat(context).doesNotHaveBean(LoggingOutboxEventRepository::class.java)
            }
    }

    @Test
    fun `uses logging repository when datasource url is not postgresql`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues("spring.datasource.url=jdbc:h2:mem:testdb")
            .run { context ->
                assertThat(context).hasSingleBean(OutboxEventRepository::class.java)
                assertThat(context).hasSingleBean(LoggingOutboxEventRepository::class.java)
                assertThat(context).doesNotHaveBean(JdbcOutboxEventRepository::class.java)
            }
    }

    @Test
    fun `backs off when custom repository is registered`() {
        contextRunner
            .withBean(OutboxEventRepository::class.java, Supplier { StubOutboxEventRepository() })
            .run { context ->
                assertThat(context).hasSingleBean(OutboxEventRepository::class.java)
                assertThat(context).hasSingleBean(StubOutboxEventRepository::class.java)
                assertThat(context).doesNotHaveBean(LoggingOutboxEventRepository::class.java)
                assertThat(context).doesNotHaveBean(JdbcOutboxEventRepository::class.java)
            }
    }

    @Test
    fun `creates polling service when custom publisher is registered`() {
        contextRunner
            .withBean(OutboxEventPublisher::class.java, Supplier { StubOutboxEventPublisher() })
            .run { context ->
                assertThat(context).hasSingleBean(OutboxEventPublisher::class.java)
                assertThat(context).hasSingleBean(StubOutboxEventPublisher::class.java)
                assertThat(context).hasSingleBean(OutboxPollingService::class.java)
            }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.outbox.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(OutboxProperties::class.java)
                assertThat(context).doesNotHaveBean(OutboxEventRepository::class.java)
                assertThat(context).doesNotHaveBean(OutboxEventAppender::class.java)
                assertThat(context).doesNotHaveBean(OutboxPollingService::class.java)
            }
    }

    @Test
    fun `rejects invalid jdbc table name`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://localhost:5432/app",
                "kopring.bricks.outbox.jdbc.table-name=outbox_event; drop table users",
            ).run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalArgumentException::class.java)
                    .rootCause()
                    .hasMessageContaining("kopring.bricks.outbox.jdbc.tableName")
            }
    }

    @Test
    fun `appender delegates to repository`() {
        val repository = StubOutboxEventRepository()
        val appender = OutboxEventAppender(repository)
        val event =
            OutboxEvent(
                aggregateType = "feature-rule",
                aggregateId = "rule-1",
                eventType = "feature-rule.updated",
                payloadJson = """{"enabled":true}""",
            )

        val appended = appender.append(event)

        assertThat(appended).isEqualTo(event)
        assertThat(repository.events).containsExactly(event)
    }

    private class StubOutboxEventRepository : OutboxEventRepository {
        val events = mutableListOf<OutboxEvent>()

        override fun append(event: OutboxEvent): OutboxEvent {
            events += event

            return event
        }

        override fun claimPending(
            limit: Int,
            now: Instant,
            claimTimeout: java.time.Duration,
        ): List<OutboxEvent> = emptyList()

        override fun markPublished(
            eventId: String,
            publishedAt: Instant,
        ) = Unit

        override fun markFailed(
            eventId: String,
            error: String,
            nextAttemptAt: Instant,
        ) = Unit
    }

    private class StubOutboxEventPublisher : OutboxEventPublisher {
        override fun publish(event: OutboxEvent) = Unit
    }
}
