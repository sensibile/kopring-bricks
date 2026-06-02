package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.EncodedResource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import javax.sql.DataSource
import kotlin.test.BeforeTest
import kotlin.test.Test

@Testcontainers
class JdbcOutboxEventRepositoryTests {
    private lateinit var dataSource: DataSource
    private lateinit var jdbcClient: JdbcClient
    private lateinit var repository: JdbcOutboxEventRepository

    @BeforeTest
    fun setUp() {
        dataSource = createDataSource()
        jdbcClient = JdbcClient.create(dataSource)
        repository = JdbcOutboxEventRepository(jdbcClient, "outbox_event")

        applySchema()
        jdbcClient.sql("truncate table outbox_event").update()
    }

    @Test
    fun `appends and claims pending events in created order`() {
        repository.append(outboxEvent(id = "event-2", createdAt = NOW.plusSeconds(1)))
        repository.append(outboxEvent(id = "event-1", createdAt = NOW))

        val claimed =
            repository.claimPending(
                limit = 2,
                now = NOW.plusSeconds(10),
                claimTimeout = Duration.ofMinutes(5),
            )

        assertThat(claimed.map { it.id }).containsExactly("event-1", "event-2")
        assertThat(claimed.map { it.status }).containsExactly(OutboxEventStatus.CLAIMED, OutboxEventStatus.CLAIMED)
        assertThat(claimed.map { it.createdAt }).containsExactly(NOW, NOW.plusSeconds(1))
        assertThat(claimed.map { it.claimedAt }).containsExactly(NOW.plusSeconds(10), NOW.plusSeconds(10))
        assertThat(claimed.first().payloadJson).contains("\"id\": \"event-1\"")
        assertThat(claimed.first().headersJson).contains("\"traceId\": \"trace-1\"")
    }

    @Test
    fun `does not claim events before available time`() {
        repository.append(
            outboxEvent(
                id = "event-1",
                availableAt = NOW.plusSeconds(10),
                nextAttemptAt = NOW.plusSeconds(10),
            ),
        )

        val claimed =
            repository.claimPending(
                limit = 1,
                now = NOW,
                claimTimeout = Duration.ofMinutes(5),
            )

        assertThat(claimed).isEmpty()
    }

    @Test
    fun `reclaims expired claimed events`() {
        repository.append(
            outboxEvent(
                id = "event-1",
                status = OutboxEventStatus.CLAIMED,
                claimedAt = NOW.minus(Duration.ofMinutes(10)),
            ),
        )

        val claimed =
            repository.claimPending(
                limit = 1,
                now = NOW,
                claimTimeout = Duration.ofMinutes(5),
            )

        assertThat(claimed.single().id).isEqualTo("event-1")
        assertThat(claimed.single().claimedAt).isEqualTo(NOW)
    }

    @Test
    fun `marks claimed event as published`() {
        repository.append(outboxEvent(id = "event-1"))
        repository.claimPending(limit = 1, now = NOW, claimTimeout = Duration.ofMinutes(5))

        repository.markPublished("event-1", NOW.plusSeconds(5))

        val saved = repository.findEvent("event-1")

        assertThat(saved.status).isEqualTo(OutboxEventStatus.PUBLISHED)
        assertThat(saved.publishedAt).isEqualTo(NOW.plusSeconds(5))
        assertThat(saved.claimedAt).isEqualTo(NOW)
        assertThat(saved.lastError).isNull()
    }

    @Test
    fun `marks claimed event as failed and schedules retry`() {
        repository.append(outboxEvent(id = "event-1"))
        repository.claimPending(limit = 1, now = NOW, claimTimeout = Duration.ofMinutes(5))

        repository.markFailed("event-1", error = "publish failed", nextAttemptAt = NOW.plusSeconds(30))

        val saved = repository.findEvent("event-1")

        assertThat(saved.status).isEqualTo(OutboxEventStatus.FAILED)
        assertThat(saved.retryCount).isEqualTo(1)
        assertThat(saved.nextAttemptAt).isEqualTo(NOW.plusSeconds(30))
        assertThat(saved.lastError).isEqualTo("publish failed")
        assertThat(saved.claimedAt).isNull()
    }

    @Test
    fun `rejects non positive claim limit`() {
        assertThatThrownBy {
            repository.claimPending(limit = 0, now = NOW, claimTimeout = Duration.ofMinutes(5))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("claim limit must be greater than zero")
    }

    private fun JdbcOutboxEventRepository.findEvent(id: String): OutboxEvent =
        claimAllSavedEvents()
            .single { it.id == id }

    private fun claimAllSavedEvents(): List<OutboxEvent> =
        jdbcClient
            .sql(
                """
                select *
                from outbox_event
                order by created_at
                """.trimIndent(),
            ).query { resultSet, _ ->
                OutboxEvent(
                    id = resultSet.getString("id"),
                    aggregateType = resultSet.getString("aggregate_type"),
                    aggregateId = resultSet.getString("aggregate_id"),
                    eventType = resultSet.getString("event_type"),
                    eventVersion = resultSet.getInt("event_version"),
                    payloadJson = resultSet.getString("payload"),
                    headersJson = resultSet.getString("headers"),
                    status = OutboxEventStatus.valueOf(resultSet.getString("status")),
                    createdAt = resultSet.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                    availableAt = resultSet.getObject("available_at", OffsetDateTime::class.java).toInstant(),
                    nextAttemptAt = resultSet.getObject("next_attempt_at", OffsetDateTime::class.java).toInstant(),
                    claimedAt = resultSet.getObject("claimed_at", OffsetDateTime::class.java)?.toInstant(),
                    publishedAt = resultSet.getObject("published_at", OffsetDateTime::class.java)?.toInstant(),
                    retryCount = resultSet.getInt("retry_count"),
                    lastError = resultSet.getString("last_error"),
                )
            }.list()

    private fun outboxEvent(
        id: String,
        status: OutboxEventStatus = OutboxEventStatus.PENDING,
        createdAt: Instant = NOW,
        availableAt: Instant = createdAt,
        nextAttemptAt: Instant = availableAt,
        claimedAt: Instant? = null,
    ): OutboxEvent =
        OutboxEvent(
            id = id,
            aggregateType = "todo",
            aggregateId = "todo-1",
            eventType = "todo.completed",
            payloadJson = """{"id":"$id"}""",
            headersJson = """{"traceId":"trace-1"}""",
            status = status,
            createdAt = createdAt,
            availableAt = availableAt,
            nextAttemptAt = nextAttemptAt,
            claimedAt = claimedAt,
        )

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
                EncodedResource(ClassPathResource("META-INF/kopring-bricks/outbox/schema-postgresql.sql")),
            )
        }
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-06-02T00:00:00Z")

        @Container
        @JvmStatic
        private val POSTGRES = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
    }
}
