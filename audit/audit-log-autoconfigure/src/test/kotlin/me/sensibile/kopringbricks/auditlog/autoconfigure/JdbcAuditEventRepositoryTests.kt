package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.EncodedResource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.time.OffsetDateTime
import javax.sql.DataSource
import kotlin.test.BeforeTest
import kotlin.test.Test

@Testcontainers
class JdbcAuditEventRepositoryTests {
    private lateinit var dataSource: DataSource
    private lateinit var jdbcClient: JdbcClient
    private lateinit var repository: JdbcAuditEventRepository

    @BeforeTest
    fun setUp() {
        dataSource = createDataSource()
        jdbcClient = JdbcClient.create(dataSource)
        repository = JdbcAuditEventRepository(jdbcClient, "audit_log")

        applySchema()
        jdbcClient.sql("truncate table audit_log").update()
    }

    @Test
    fun `saves audit event with json fields and timestamptz`() {
        repository.save(
            AuditEvent(
                id = "audit-1",
                occurredAt = OCCURRED_AT,
                actor = AuditActor(type = "user", id = "user-1", name = "Tonton"),
                action = "todo.completed",
                target = AuditTarget(type = "todo", id = "todo-1", name = "Sample todo"),
                outcome = AuditOutcome.SUCCESS,
                traceId = "trace-1",
                requestId = "request-1",
                reason = "user request",
                metadataJson = """{"ip":"127.0.0.1"}""",
                beforeStateJson = """{"done":false}""",
                afterStateJson = """{"done":true}""",
            ),
        )

        val saved =
            jdbcClient
                .sql("select * from audit_log where id = :id")
                .param("id", "audit-1")
                .query { resultSet, _ ->
                    SavedAuditEvent(
                        occurredAt = resultSet.getObject("occurred_at", OffsetDateTime::class.java).toInstant(),
                        actorType = resultSet.getString("actor_type"),
                        actorId = resultSet.getString("actor_id"),
                        actorName = resultSet.getString("actor_name"),
                        action = resultSet.getString("action"),
                        targetType = resultSet.getString("target_type"),
                        targetId = resultSet.getString("target_id"),
                        targetName = resultSet.getString("target_name"),
                        outcome = resultSet.getString("outcome"),
                        traceId = resultSet.getString("trace_id"),
                        requestId = resultSet.getString("request_id"),
                        reason = resultSet.getString("reason"),
                        metadataJson = resultSet.getString("metadata"),
                        beforeStateJson = resultSet.getString("before_state"),
                        afterStateJson = resultSet.getString("after_state"),
                    )
                }.single()

        assertThat(saved.occurredAt).isEqualTo(OCCURRED_AT)
        assertThat(saved.actorType).isEqualTo("user")
        assertThat(saved.actorId).isEqualTo("user-1")
        assertThat(saved.actorName).isEqualTo("Tonton")
        assertThat(saved.action).isEqualTo("todo.completed")
        assertThat(saved.targetType).isEqualTo("todo")
        assertThat(saved.targetId).isEqualTo("todo-1")
        assertThat(saved.targetName).isEqualTo("Sample todo")
        assertThat(saved.outcome).isEqualTo("SUCCESS")
        assertThat(saved.traceId).isEqualTo("trace-1")
        assertThat(saved.requestId).isEqualTo("request-1")
        assertThat(saved.reason).isEqualTo("user request")
        assertThat(saved.metadataJson).contains("\"ip\": \"127.0.0.1\"")
        assertThat(saved.beforeStateJson).contains("\"done\": false")
        assertThat(saved.afterStateJson).contains("\"done\": true")
    }

    @Test
    fun `saves audit event with nullable optional fields`() {
        repository.save(
            AuditEvent(
                id = "audit-1",
                occurredAt = OCCURRED_AT,
                actor = AuditActor(type = "system", id = "scheduler"),
                action = "todo.reconciled",
                target = AuditTarget(type = "todo", id = "todo-1"),
            ),
        )

        val saved =
            jdbcClient
                .sql("select actor_name, target_name, metadata from audit_log where id = :id")
                .param("id", "audit-1")
                .query { resultSet, _ ->
                    listOf(
                        resultSet.getString("actor_name"),
                        resultSet.getString("target_name"),
                        resultSet.getString("metadata"),
                    )
                }.single()

        assertThat(saved).containsOnlyNulls()
    }

    private fun createDataSource(): DataSource =
        DriverManagerDataSource(
            POSTGRES.jdbcUrl,
            POSTGRES.username,
            POSTGRES.password,
        )

    private fun applySchema() {
        dataSource.connection.use { connection ->
            ScriptUtils.executeSqlScript(
                connection,
                EncodedResource(ClassPathResource("META-INF/kopring-bricks/audit-log/schema-postgresql.sql")),
            )
        }
    }

    private data class SavedAuditEvent(
        val occurredAt: Instant,
        val actorType: String,
        val actorId: String,
        val actorName: String?,
        val action: String,
        val targetType: String,
        val targetId: String,
        val targetName: String?,
        val outcome: String,
        val traceId: String?,
        val requestId: String?,
        val reason: String?,
        val metadataJson: String?,
        val beforeStateJson: String?,
        val afterStateJson: String?,
    )

    private companion object {
        private val OCCURRED_AT: Instant = Instant.parse("2026-06-02T00:00:00Z")

        @Container
        @JvmStatic
        private val POSTGRES = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
    }
}
