package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.mockito.Mockito.mock
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.simple.JdbcClient
import java.util.function.Supplier
import kotlin.test.Test

class EventSourcingAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(
                EventSourcingAutoConfiguration::class.java,
                EventSourcingFlywayAutoConfiguration::class.java,
            )

    @Test
    fun `does not create event store without jdbc client or custom store`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(EventSourcingProperties::class.java)
            assertThat(context).doesNotHaveBean(EventStore::class.java)
            assertThat(context).doesNotHaveBean(EventSourcingTemplate::class.java)
            assertThat(context).doesNotHaveBean(FlywayConfigurationCustomizer::class.java)
        }
    }

    @Test
    fun `creates jdbc event store when datasource url is postgresql`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues("spring.datasource.url=jdbc:postgresql://localhost:5432/app")
            .run { context ->
                assertThat(context).hasSingleBean(EventStore::class.java)
                assertThat(context).hasSingleBean(JdbcEventStore::class.java)
                assertThat(context).hasSingleBean(EventSourcingTemplate::class.java)
            }
    }

    @Test
    fun `creates jdbc event store when dialect is explicitly postgresql`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues("kopring.bricks.event-sourcing.jdbc.dialect=postgresql")
            .run { context ->
                assertThat(context).hasSingleBean(EventStore::class.java)
                assertThat(context).hasSingleBean(JdbcEventStore::class.java)
                assertThat(context).hasSingleBean(EventSourcingTemplate::class.java)
            }
    }

    @Test
    fun `does not create jdbc event store when datasource url is not postgresql`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues("spring.datasource.url=jdbc:h2:mem:testdb")
            .run { context ->
                assertThat(context).doesNotHaveBean(EventStore::class.java)
                assertThat(context).doesNotHaveBean(JdbcEventStore::class.java)
                assertThat(context).doesNotHaveBean(EventSourcingTemplate::class.java)
            }
    }

    @Test
    fun `creates template when custom event store is registered`() {
        contextRunner
            .withBean(EventStore::class.java, Supplier { StubEventStore() })
            .run { context ->
                assertThat(context).hasSingleBean(EventStore::class.java)
                assertThat(context).hasSingleBean(StubEventStore::class.java)
                assertThat(context).hasSingleBean(EventSourcingTemplate::class.java)
                assertThat(context).doesNotHaveBean(JdbcEventStore::class.java)
            }
    }

    @Test
    fun `backs off when custom template is registered`() {
        contextRunner
            .withBean(EventStore::class.java, Supplier { StubEventStore() })
            .withBean(EventSourcingTemplate::class.java, Supplier { EventSourcingTemplate(StubEventStore()) })
            .run { context ->
                assertThat(context).hasSingleBean(EventSourcingTemplate::class.java)
            }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.event-sourcing.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(EventSourcingProperties::class.java)
                assertThat(context).doesNotHaveBean(EventStore::class.java)
                assertThat(context).doesNotHaveBean(EventSourcingTemplate::class.java)
                assertThat(context).doesNotHaveBean(FlywayConfigurationCustomizer::class.java)
            }
    }

    @Test
    fun `rejects invalid jdbc table name`() {
        contextRunner
            .withBean(JdbcClient::class.java, Supplier { mock(JdbcClient::class.java) })
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://localhost:5432/app",
                "kopring.bricks.event-sourcing.jdbc.table-name=event_store; drop table users",
            ).run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalArgumentException::class.java)
                    .rootCause()
                    .hasMessageContaining("kopring.bricks.event-sourcing.jdbc.tableName")
            }
    }

    @Test
    fun `creates flyway customizer when flyway schema is enabled`() {
        contextRunner
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://localhost:5432/app",
                "kopring.bricks.event-sourcing.jdbc.flyway.enabled=true",
            ).run { context ->
                assertThat(context).hasSingleBean(FlywayConfigurationCustomizer::class.java)

                val configuration = FluentConfiguration().locations("classpath:db/migration")
                context.getBean(FlywayConfigurationCustomizer::class.java).customize(configuration)

                assertThat(configuration.locations.map { it.toString() })
                    .containsExactlyInAnyOrder(
                        "classpath:db/migration",
                        EVENT_SOURCING_POSTGRESQL_FLYWAY_LOCATION,
                    )
            }
    }

    @Test
    fun `does not create flyway customizer when datasource url is not postgresql`() {
        contextRunner
            .withPropertyValues(
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "kopring.bricks.event-sourcing.jdbc.flyway.enabled=true",
            ).run { context ->
                assertThat(context).doesNotHaveBean(FlywayConfigurationCustomizer::class.java)
            }
    }

    @Test
    fun `rejects flyway schema with custom table name`() {
        contextRunner
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://localhost:5432/app",
                "kopring.bricks.event-sourcing.jdbc.flyway.enabled=true",
                "kopring.bricks.event-sourcing.jdbc.table-name=custom_event_store",
            ).run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalArgumentException::class.java)
                    .rootCause()
                    .hasMessageContaining("kopring.bricks.event-sourcing.jdbc.flyway.enabled requires the default")
            }
    }

    private class StubEventStore : EventStore {
        override fun append(
            streamId: String,
            expectedVersion: Long,
            events: List<EventStoreEvent>,
        ): EventAppendResult =
            EventAppendResult(
                streamId = streamId,
                previousVersion = expectedVersion,
                currentVersion = expectedVersion,
                events = emptyList(),
            )

        override fun load(
            streamId: String,
            fromVersion: Long,
        ): List<StoredEvent> = emptyList()
    }
}
