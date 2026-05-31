package me.sensibile.kopringbricks.testsupport.eventsourcing

import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStreamVersionConflictException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.Test

class InMemoryEventStoreTests {
    @Test
    fun `appends events with sequential stream versions`() {
        val eventStore = InMemoryEventStore()

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
        assertThat(eventStore.load("todo-1")).extracting<Long> { it.streamVersion }.containsExactly(1, 2)
    }

    @Test
    fun `rejects stale expected version`() {
        val eventStore = InMemoryEventStore()
        eventStore.append("todo-1", 0, listOf(event("event-1", "todo.created")))

        assertThatThrownBy {
            eventStore.append("todo-1", 0, listOf(event("event-2", "todo.completed")))
        }.isInstanceOf(EventStreamVersionConflictException::class.java)
            .hasMessageContaining("expectedVersion=0")
            .hasMessageContaining("actualVersion=1")
    }

    @Test
    fun `loads from requested version`() {
        val eventStore = InMemoryEventStore()
        eventStore.append(
            streamId = "todo-1",
            expectedVersion = 0,
            events =
                listOf(
                    event("event-1", "todo.created"),
                    event("event-2", "todo.completed"),
                ),
        )

        assertThat(eventStore.load("todo-1", fromVersion = 2))
            .extracting<String> { it.eventType }
            .containsExactly("todo.completed")
    }

    @Test
    fun `supports concurrent reads writes and clears`() {
        val eventStore = InMemoryEventStore()
        val executor = Executors.newFixedThreadPool(WORKER_COUNT)
        val tasks: List<Callable<Unit>> =
            List(EVENT_COUNT) { index ->
                Callable<Unit> {
                    eventStore.append(
                        streamId = "todo-$index",
                        expectedVersion = 0,
                        events = listOf(event("event-$index", "todo.created")),
                    )
                    Unit
                }
            } +
                List(EVENT_COUNT) { index ->
                    Callable<Unit> {
                        eventStore.load("todo-$index")
                        eventStore.events
                        Unit
                    }
                } +
                List(CLEAR_COUNT) {
                    Callable<Unit> {
                        eventStore.clear()
                        Unit
                    }
                }

        try {
            executor.invokeAll(tasks).forEach { future -> future.get() }
        } finally {
            executor.shutdownNow()
        }
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
        private val NOW: Instant = Instant.parse("2026-05-29T00:00:00Z")
        private const val EVENT_COUNT = 50
        private const val CLEAR_COUNT = 5
        private const val WORKER_COUNT = 8
    }
}
