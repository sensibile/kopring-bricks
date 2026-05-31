package me.sensibile.kopringbricks.testsupport.eventsourcing

import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventAppendResult
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStore
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStreamVersionConflictException
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.StoredEvent
import java.util.concurrent.ConcurrentHashMap

class InMemoryEventStore : EventStore {
    private val eventsByStream = ConcurrentHashMap<String, MutableList<StoredEvent>>()

    val events: List<StoredEvent>
        get() =
            eventsByStream.values
                .flatten()
                .sortedWith(compareBy<StoredEvent> { it.streamId }.thenBy { it.streamVersion })

    override fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult {
        require(streamId.isNotBlank()) { "streamId must not be blank" }
        require(expectedVersion >= 0) { "expectedVersion must be greater than or equal to zero" }
        require(events.isNotEmpty()) { "events must not be empty" }

        return synchronized(eventsByStream) {
            val streamEvents = eventsByStream.getOrPut(streamId) { mutableListOf() }
            val actualVersion = streamEvents.lastOrNull()?.streamVersion ?: 0
            if (actualVersion != expectedVersion) {
                throw EventStreamVersionConflictException(streamId, expectedVersion, actualVersion)
            }

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

            streamEvents += storedEvents

            EventAppendResult(
                streamId = streamId,
                previousVersion = expectedVersion,
                currentVersion = streamEvents.last().streamVersion,
                events = storedEvents,
            )
        }
    }

    override fun load(
        streamId: String,
        fromVersion: Long,
    ): List<StoredEvent> {
        require(streamId.isNotBlank()) { "streamId must not be blank" }
        require(fromVersion >= 1) { "fromVersion must be greater than or equal to one" }

        return eventsByStream[streamId]
            ?.filter { it.streamVersion >= fromVersion }
            ?.sortedBy { it.streamVersion }
            ?: emptyList()
    }

    fun clear() {
        eventsByStream.clear()
    }
}
