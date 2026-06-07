package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditEventRepository
import me.sensibile.kopringbricks.auditlog.autoconfigure.JdbcAuditEventRepository
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStore
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.JdbcEventStore
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.JdbcOutboxEventRepository
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource
import kotlin.test.Test

@Testcontainers
@SpringBootTest(
    properties = [
        "samples.todo.event-store.in-memory=false",
        "kopring.bricks.audit-log.jdbc.dialect=postgresql",
        "kopring.bricks.audit-log.jdbc.flyway.enabled=true",
        "kopring.bricks.event-sourcing.jdbc.dialect=postgresql",
        "kopring.bricks.event-sourcing.jdbc.flyway.enabled=true",
        "kopring.bricks.outbox.jdbc.dialect=postgresql",
        "kopring.bricks.outbox.jdbc.flyway.enabled=true",
    ],
)
@AutoConfigureMockMvc
@Import(TodoJdbcBricksApplicationTests.JdbcClientTestConfiguration::class)
class TodoJdbcBricksApplicationTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Autowired
    private lateinit var auditRepository: AuditEventRepository

    @Autowired
    private lateinit var eventStore: EventStore

    @Autowired
    private lateinit var outboxRepository: OutboxEventRepository

    @Test
    fun `starts sample with JDBC-backed bricks and Flyway schemas`() {
        mockMvc
            .post("/todos") {
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"title":"jdbc sample"}"""
            }.andExpect {
                status { isCreated() }
            }

        assertAll(
            { assertThat(auditRepository).isInstanceOf(JdbcAuditEventRepository::class.java) },
            { assertThat(eventStore).isInstanceOf(JdbcEventStore::class.java) },
            { assertThat(outboxRepository).isInstanceOf(JdbcOutboxEventRepository::class.java) },
            { assertThat(jdbcClient.rowCount(ExpectedTable.AUDIT_LOG)).isEqualTo(1) },
            { assertThat(jdbcClient.rowCount(ExpectedTable.EVENT_STORE)).isEqualTo(1) },
            { assertThat(jdbcClient.rowCount(ExpectedTable.OUTBOX_EVENT)).isEqualTo(1) },
        )
    }

    private fun JdbcClient.rowCount(table: ExpectedTable): Long =
        sql("select count(*) from ${table.tableName}")
            .query(Long::class.java)
            .single()

    private enum class ExpectedTable(
        val tableName: String,
    ) {
        AUDIT_LOG("audit_log"),
        EVENT_STORE("event_store"),
        OUTBOX_EVENT("outbox_event"),
    }

    @TestConfiguration(proxyBeanMethods = false)
    class JdbcClientTestConfiguration {
        @Bean
        fun jdbcClient(dataSource: DataSource): JdbcClient = JdbcClient.create(dataSource)
    }

    private companion object {
        @Container
        @JvmStatic
        private val POSTGRES = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))

        @DynamicPropertySource
        @JvmStatic
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl)
            registry.add("spring.datasource.username", POSTGRES::getUsername)
            registry.add("spring.datasource.password", POSTGRES::getPassword)
        }
    }
}
