package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStreamVersionConflictException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.Instant
import kotlin.test.Test

class TodoEventStoreTests {
    @Test
    fun `appends and loads events in stream version order`() {
        val eventStore = TodoEventStore()

        val result =
            eventStore.append(
                streamId = "todo-1",
                expectedVersion = 0,
                events =
                    listOf(
                        event("event-1", "todo.created"),
                        event("event-2", "todo.completed"),
                    ),
            )

        assertThat(result.previousVersion).isEqualTo(0)
        assertThat(result.currentVersion).isEqualTo(2)
        assertThat(eventStore.load("todo-1").map { it.eventType })
            .containsExactly("todo.created", "todo.completed")
        assertThat(eventStore.load("todo-1").map { it.streamVersion })
            .containsExactly(1L, 2L)
    }

    @Test
    fun `loads events from requested stream version`() {
        val eventStore = TodoEventStore()
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
        val eventStore = TodoEventStore()
        eventStore.append("todo-1", 0, listOf(event("event-1", "todo.created")))

        assertThatThrownBy {
            eventStore.append("todo-1", 0, listOf(event("event-2", "todo.completed")))
        }.isInstanceOf(EventStreamVersionConflictException::class.java)
            .hasMessageContaining("expectedVersion=0")
            .hasMessageContaining("actualVersion=1")
    }

    @Test
    fun `rejects invalid append inputs`() {
        val eventStore = TodoEventStore()

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
        val eventStore = TodoEventStore()

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
    ): EventStoreEvent =
        EventStoreEvent(
            id = id,
            eventType = eventType,
            payloadJson = """{"id":"todo-1"}""",
            occurredAt = NOW,
        )

    private companion object {
        private val NOW: Instant = Instant.parse("2026-05-31T00:00:00Z")
    }
}
