package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.simple.JdbcClient
import java.util.function.Supplier
import kotlin.test.Test

class AuditLogAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(AuditLogAutoConfiguration::class.java)

    @Test
    fun `creates logging repository and publisher without jdbc client`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(AuditLogProperties::class.java)
            assertThat(context).hasSingleBean(AuditEventRepository::class.java)
            assertThat(context).hasSingleBean(AuditEventPublisher::class.java)
            assertThat(context).hasSingleBean(LoggingAuditEventRepository::class.java)
        }
    }

    @Test
    fun `creates jdbc repository when jdbc client is available`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .run { context ->
                assertThat(context).hasSingleBean(AuditEventRepository::class.java)
                assertThat(context).hasSingleBean(JdbcAuditEventRepository::class.java)
                assertThat(context).doesNotHaveBean(LoggingAuditEventRepository::class.java)
            }
    }

    @Test
    fun `backs off when custom repository is registered`() {
        contextRunner
            .withBean(AuditEventRepository::class.java, Supplier { StubAuditEventRepository() })
            .run { context ->
                assertThat(context).hasSingleBean(AuditEventRepository::class.java)
                assertThat(context).hasSingleBean(StubAuditEventRepository::class.java)
                assertThat(context).doesNotHaveBean(LoggingAuditEventRepository::class.java)
                assertThat(context).doesNotHaveBean(JdbcAuditEventRepository::class.java)
            }
    }

    @Test
    fun `backs off when custom publisher is registered`() {
        contextRunner
            .withBean(AuditEventPublisher::class.java, Supplier { StubAuditEventPublisher() })
            .run { context ->
                assertThat(context).hasSingleBean(AuditEventPublisher::class.java)
                assertThat(context).hasSingleBean(StubAuditEventPublisher::class.java)
            }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.audit-log.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(AuditLogProperties::class.java)
                assertThat(context).doesNotHaveBean(AuditEventRepository::class.java)
                assertThat(context).doesNotHaveBean(AuditEventPublisher::class.java)
            }
    }

    @Test
    fun `publisher ignores repository failure by default`() {
        val publisher =
            DefaultAuditEventPublisher(
                FailingAuditEventRepository(),
                AuditLogProperties(),
            )

        publisher.publish(auditEvent())
    }

    @Test
    fun `publisher can rethrow repository failure`() {
        val publisher =
            DefaultAuditEventPublisher(
                FailingAuditEventRepository(),
                AuditLogProperties(publisher = AuditLogProperties.Publisher(failOnError = true)),
            )

        assertThatThrownBy { publisher.publish(auditEvent()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("audit repository failure")
    }

    private fun auditEvent(): AuditEvent =
        AuditEvent(
            actor = AuditActor(type = "user", id = "user-1"),
            action = "todo.created",
            target = AuditTarget(type = "todo", id = "todo-1"),
        )

    private class StubAuditEventRepository : AuditEventRepository {
        override fun save(event: AuditEvent) = Unit
    }

    private class StubAuditEventPublisher : AuditEventPublisher {
        override fun publish(event: AuditEvent) = Unit
    }

    private class FailingAuditEventRepository : AuditEventRepository {
        override fun save(event: AuditEvent): Unit = throw IllegalStateException("audit repository failure")
    }
}
