package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.Instant
import kotlin.test.Test

class EventSourcingTemplateTests {
    @Test
    fun `append runs matching configured projections after events are stored`() {
        val eventStore = StubEventStore()
        val projectedEvents = mutableListOf<StoredEvent>()
        val template =
            EventSourcingTemplate(
                eventStore,
                projections =
                    listOf(
                        RecordingProjection("todo.created", projectedEvents),
                        RecordingProjection("todo.completed", projectedEvents),
                    ),
            )

        val result =
            template.append(
                streamId = "todo-1",
                expectedVersion = 0,
                events =
                    listOf(
                        EventStoreEvent(
                            id = "event-1",
                            eventType = "todo.created",
                            payloadJson = """{"title":"Ship"}""",
                            occurredAt = NOW,
                        ),
                    ),
            )

        assertThat(result.events).hasSize(1)
        assertThat(projectedEvents.map { it.eventType }).containsExactly("todo.created")
    }

    @Test
    fun `append runs call scoped projections in addition to configured projections`() {
        val eventStore = StubEventStore()
        val projectedEvents = mutableListOf<String>()
        val template =
            EventSourcingTemplate(
                eventStore,
                projections =
                    listOf(
                        NamedProjection("configured", projectedEvents),
                    ),
            )

        template.append(
            streamId = "todo-1",
            expectedVersion = 0,
            events =
                listOf(
                    EventStoreEvent(
                        id = "event-1",
                        eventType = "todo.created",
                        payloadJson = """{"title":"Ship"}""",
                        occurredAt = NOW,
                    ),
                ),
            projections =
                listOf(
                    NamedProjection("call-scoped", projectedEvents),
                ),
        )

        assertThat(projectedEvents).containsExactly("configured:todo.created", "call-scoped:todo.created")
    }

    @Test
    fun `append propagates projection failures`() {
        val eventStore = StubEventStore()
        val template =
            EventSourcingTemplate(
                eventStore,
                projections =
                    listOf(
                        FailingProjection(),
                    ),
            )

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
                            occurredAt = NOW,
                        ),
                    ),
            )
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessage("projection failed")
    }

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
        ): EventAppendResult {
            val storedEvents =
                events.mapIndexed { index, event ->
                    StoredEvent(
                        id = event.id,
                        streamId = streamId,
                        streamVersion = expectedVersion + index + 1,
                        eventType = event.eventType,
                        eventVersion = event.eventVersion,
                        payloadJson = event.payloadJson,
                        metadataJson = event.metadataJson,
                        occurredAt = event.occurredAt,
                    )
                }
            this.storedEvents += storedEvents
            return EventAppendResult(
                streamId = streamId,
                previousVersion = expectedVersion,
                currentVersion = storedEvents.last().streamVersion,
                events = storedEvents,
            )
        }

        override fun load(
            streamId: String,
            fromVersion: Long,
        ): List<StoredEvent> =
            storedEvents
                .filter { it.streamId == streamId && it.streamVersion >= fromVersion }
                .sortedBy { it.streamVersion }
    }

    private class RecordingProjection(
        private val eventType: String,
        private val events: MutableList<StoredEvent>,
    ) : EventProjection {
        override fun supports(event: StoredEvent): Boolean = event.eventType == eventType

        override fun project(event: StoredEvent) {
            events += event
        }
    }

    private class NamedProjection(
        private val name: String,
        private val events: MutableList<String>,
    ) : EventProjection {
        override fun project(event: StoredEvent) {
            events += "$name:${event.eventType}"
        }
    }

    private class FailingProjection : EventProjection {
        override fun project(event: StoredEvent) {
            error("projection failed")
        }
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-05-29T00:00:00Z")
    }
}
