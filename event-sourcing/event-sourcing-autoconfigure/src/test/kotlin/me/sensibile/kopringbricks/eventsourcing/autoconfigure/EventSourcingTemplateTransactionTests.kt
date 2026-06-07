package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.EncodedResource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.function.Supplier
import javax.sql.DataSource
import kotlin.test.BeforeTest
import kotlin.test.Test

@Testcontainers
class EventSourcingTemplateTransactionTests {
    private lateinit var dataSource: DataSource
    private lateinit var jdbcClient: JdbcClient
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventSourcingAutoConfiguration::class.java))
            .withUserConfiguration(TestConfiguration::class.java)
            .withPropertyValues("kopring.bricks.event-sourcing.jdbc.dialect=postgresql")

    @BeforeTest
    fun setUp() {
        dataSource = createDataSource()
        jdbcClient = JdbcClient.create(dataSource)

        applySchema()
        jdbcClient.sql("drop table if exists projected_events").update()
        jdbcClient
            .sql(
                """
                create table projected_events (
                    id text primary key,
                    event_type text not null
                )
                """.trimIndent(),
            ).update()
        jdbcClient.sql("truncate table event_store").update()
    }

    @Test
    fun `projection failure rolls back appended events`() {
        contextRunner
            .withBean(DataSource::class.java, Supplier { dataSource })
            .withBean(JdbcClient::class.java, Supplier { jdbcClient })
            .withBean(DataSourceTransactionManager::class.java, Supplier { DataSourceTransactionManager(dataSource) })
            .run { context ->
                val template = context.getBean(EventSourcingTemplate::class.java)

                assertThatThrownBy {
                    template.append(
                        streamId = "todo-1",
                        expectedVersion = 0,
                        events =
                            listOf(
                                EventStoreEvent(
                                    id = "event-1",
                                    eventType = "todo.created",
                                    payloadJson = """{"title":"Ship"}""",
                                    occurredAt = OCCURRED_AT,
                                ),
                            ),
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("projection failed")

                assertThat(countRows("event_store")).isZero()
                assertThat(countRows("projected_events")).isZero()
            }
    }

    @Test
    fun `registered projection beans run after append`() {
        contextRunner
            .withBean(DataSource::class.java, Supplier { dataSource })
            .withBean(JdbcClient::class.java, Supplier { jdbcClient })
            .withBean(DataSourceTransactionManager::class.java, Supplier { DataSourceTransactionManager(dataSource) })
            .withBean(
                "failingProjectionDisabled",
                Boolean::class.java,
                Supplier { true },
            ).run { context ->
                val template = context.getBean(EventSourcingTemplate::class.java)

                template.append(
                    streamId = "todo-1",
                    expectedVersion = 0,
                    events =
                        listOf(
                            EventStoreEvent(
                                id = "event-1",
                                eventType = "todo.created",
                                payloadJson = """{"title":"Ship"}""",
                                occurredAt = OCCURRED_AT,
                            ),
                        ),
                )

                assertThat(countRows("event_store")).isEqualTo(1)
                assertThat(countRows("projected_events")).isEqualTo(1)
            }
    }

    private fun countRows(tableName: String): Long =
        jdbcClient
            .sql("select count(*) from $tableName")
            .query(Long::class.java)
            .single()

    private fun applySchema() {
        dataSource.connection.use { connection ->
            ScriptUtils.executeSqlScript(
                connection,
                EncodedResource(ClassPathResource("META-INF/kopring-bricks/event-sourcing/schema-postgresql.sql")),
            )
        }
    }

    private fun createDataSource(): DataSource =
        DriverManagerDataSource(
            POSTGRES.jdbcUrl,
            POSTGRES.username,
            POSTGRES.password,
        )

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    private class TestConfiguration {
        @Bean
        fun projection(
            jdbcClient: JdbcClient,
            failingProjectionDisabled: ObjectProvider<Boolean>,
        ): EventProjection =
            if (failingProjectionDisabled.getIfAvailable { false }) {
                RecordingProjection(jdbcClient)
            } else {
                FailingProjection(jdbcClient)
            }
    }

    private abstract class JdbcProjection(
        private val jdbcClient: JdbcClient,
    ) : EventProjection {
        override fun supports(event: StoredEvent): Boolean = event.eventType == "todo.created"

        protected fun record(event: StoredEvent) {
            jdbcClient
                .sql("insert into projected_events (id, event_type) values (:id, :eventType)")
                .param("id", event.id)
                .param("eventType", event.eventType)
                .update()
        }
    }

    private class RecordingProjection(
        jdbcClient: JdbcClient,
    ) : JdbcProjection(jdbcClient) {
        override fun project(event: StoredEvent) {
            record(event)
        }
    }

    private class FailingProjection(
        jdbcClient: JdbcClient,
    ) : JdbcProjection(jdbcClient) {
        override fun project(event: StoredEvent) {
            record(event)
            error("projection failed")
        }
    }

    private companion object {
        private val OCCURRED_AT: Instant = Instant.parse("2026-06-01T00:00:00Z")

        @Container
        @JvmStatic
        private val POSTGRES = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
    }
}
