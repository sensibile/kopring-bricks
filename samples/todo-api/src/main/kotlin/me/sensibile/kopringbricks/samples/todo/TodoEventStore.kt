package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventAppendResult
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStore
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStreamVersionConflictException
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.StoredEvent
import org.springframework.stereotype.Component

@Component
class TodoEventStore : EventStore {
    private val eventsByStream = mutableMapOf<String, MutableList<StoredEvent>>()

    override fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult =
        synchronized(eventsByStream) {
            require(streamId.isNotBlank()) { "streamId must not be blank" }
            require(expectedVersion >= 0) { "expectedVersion must be greater than or equal to zero" }
            require(events.isNotEmpty()) { "events must not be empty" }

            val streamEvents = eventsByStream.getOrPut(streamId) { mutableListOf() }
            val actualVersion = streamEvents.lastOrNull()?.streamVersion ?: 0
            if (actualVersion != expectedVersion) {
                throw EventStreamVersionConflictException(streamId, expectedVersion, actualVersion)
            }

            val appended =
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

            streamEvents += appended

            EventAppendResult(
                streamId = streamId,
                previousVersion = expectedVersion,
                currentVersion = streamEvents.last().streamVersion,
                events = appended,
            )
        }

    override fun load(
        streamId: String,
        fromVersion: Long,
    ): List<StoredEvent> =
        synchronized(eventsByStream) {
            require(streamId.isNotBlank()) { "streamId must not be blank" }
            require(fromVersion >= 1) { "fromVersion must be greater than or equal to one" }

            eventsByStream[streamId]
                ?.filter { it.streamVersion >= fromVersion }
                ?.sortedBy { it.streamVersion }
                ?: emptyList()
        }
}
