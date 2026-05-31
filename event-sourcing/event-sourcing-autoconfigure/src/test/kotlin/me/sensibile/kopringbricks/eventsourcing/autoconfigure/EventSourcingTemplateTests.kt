package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import java.time.Instant
import kotlin.test.Test

class EventSourcingTemplateTests {
    @Test
    fun `folds loaded events into state`() {
        val eventStore = StubEventStore()
        val template = EventSourcingTemplate(eventStore)
        eventStore.storedEvents +=
            StoredEvent(
                id = "event-1",
                streamId = "todo-1",
                streamVersion = 1,
                eventType = "todo.created",
                payloadJson = """{"title":"Ship"}""",
                occurredAt = NOW,
            )
        eventStore.storedEvents +=
            StoredEvent(
                id = "event-2",
                streamId = "todo-1",
                streamVersion = 2,
                eventType = "todo.completed",
                payloadJson = """{"completed":true}""",
                occurredAt = NOW,
            )

        val state =
            template.fold("todo-1", emptyList<String>()) { applied, event ->
                applied + event.eventType
            }

        assertThat(state).containsExactly("todo.created", "todo.completed")
    }

    private class StubEventStore : EventStore {
        val storedEvents = mutableListOf<StoredEvent>()

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
        ): List<StoredEvent> =
            storedEvents
                .filter { it.streamId == streamId && it.streamVersion >= fromVersion }
                .sortedBy { it.streamVersion }
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-05-29T00:00:00Z")
    }
}
