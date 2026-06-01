package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.sql.DataSource
import kotlin.test.BeforeTest
import kotlin.test.Test

@Testcontainers
class JdbcEventStoreTests {
    private lateinit var jdbcClient: JdbcClient
    private lateinit var eventStore: JdbcEventStore

    @BeforeTest
    fun setUp() {
        jdbcClient = JdbcClient.create(dataSource())
        eventStore = JdbcEventStore(jdbcClient, "event_store", Clock.fixed(RECORDED_AT, ZoneOffset.UTC))

        jdbcClient.sql(schemaSql()).update()
        jdbcClient.sql("truncate table event_store").update()
    }

    @Test
    fun `appends and loads events in stream version order`() {
        val result =
            eventStore.append(
                streamId = "todo-1",
                expectedVersion = 0,
                events =
                    listOf(
                        event("event-1", "todo.created", metadataJson = """{"actor":"user-1"}"""),
                        event("event-2", "todo.completed"),
                    ),
            )

        assertThat(result.previousVersion).isEqualTo(0)
        assertThat(result.currentVersion).isEqualTo(2)
        assertThat(result.events.map { it.streamVersion }).containsExactly(1L, 2L)

        val loaded = eventStore.load("todo-1")

        assertThat(loaded.map { it.eventType }).containsExactly("todo.created", "todo.completed")
        assertThat(loaded.map { it.streamVersion }).containsExactly(1L, 2L)
        assertThat(loaded.first().payloadJson).contains("\"id\": \"todo-1\"")
        assertThat(loaded.first().metadataJson).contains("\"actor\": \"user-1\"")
        assertThat(loaded.first().occurredAt).isEqualTo(OCCURRED_AT)
        assertThat(loaded.last().metadataJson).isNull()
    }

    @Test
    fun `loads events from requested stream version`() {
        eventStore.append(
            streamId = "todo-1",
            expectedVersion = 0,
            events =
                listOf(
                    event("event-1", "todo.created"),
                    event("event-2", "todo.completed"),
                ),
        )

        assertThat(eventStore.load("todo-1", fromVersion = 2).map { it.eventType })
            .containsExactly("todo.completed")
    }

    @Test
    fun `rejects stale expected version`() {
        eventStore.append("todo-1", 0, listOf(event("event-1", "todo.created")))

        assertThatThrownBy {
            eventStore.append("todo-1", 0, listOf(event("event-2", "todo.completed")))
        }.isInstanceOf(EventStreamVersionConflictException::class.java)
            .hasMessageContaining("streamId=todo-1")
            .hasMessageContaining("expectedVersion=0")
            .hasMessageContaining("actualVersion=1")
    }

    @Test
    fun `rejects duplicate event ids as append conflicts`() {
        eventStore.append("todo-1", 0, listOf(event("event-1", "todo.created")))

        assertThatThrownBy {
            eventStore.append("todo-1", 1, listOf(event("event-1", "todo.completed")))
        }.isInstanceOf(EventStreamVersionConflictException::class.java)
            .hasMessageContaining("streamId=todo-1")
            .hasMessageContaining("actualVersion=unknown")
    }

    @Test
    fun `rejects invalid append inputs`() {
        assertThatThrownBy {
            eventStore.append("", expectedVersion = 0, events = listOf(event("event-1", "todo.created")))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("streamId must not be blank")

        assertThatThrownBy {
            eventStore.append("todo-1", expectedVersion = -1, events = listOf(event("event-1", "todo.created")))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("expectedVersion must be greater than or equal to zero")

        assertThatThrownBy {
            eventStore.append("todo-1", expectedVersion = 0, events = emptyList())
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("events must not be empty")
    }

    @Test
    fun `rejects invalid load inputs`() {
        assertThatThrownBy {
            eventStore.load("", fromVersion = 1)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("streamId must not be blank")

        assertThatThrownBy {
            eventStore.load("todo-1", fromVersion = 0)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("fromVersion must be greater than or equal to one")
    }

    private fun event(
        id: String,
        eventType: String,
        metadataJson: String? = null,
    ): EventStoreEvent =
        EventStoreEvent(
            id = id,
            eventType = eventType,
            payloadJson = """{"id":"todo-1"}""",
            metadataJson = metadataJson,
            occurredAt = OCCURRED_AT,
        )

    private fun dataSource(): DataSource =
        DriverManagerDataSource(
            POSTGRES.jdbcUrl,
            POSTGRES.username,
            POSTGRES.password,
        )

    private fun schemaSql(): String =
        ClassPathResource("META-INF/kopring-bricks/event-sourcing/schema-postgresql.sql")
            .inputStream
            .bufferedReader()
            .use { it.readText() }

    private companion object {
        private val OCCURRED_AT: Instant = Instant.parse("2026-06-01T00:00:00Z")
        private val RECORDED_AT: Instant = Instant.parse("2026-06-01T00:00:01Z")

        @Container
        @JvmStatic
        private val POSTGRES = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
    }
}
