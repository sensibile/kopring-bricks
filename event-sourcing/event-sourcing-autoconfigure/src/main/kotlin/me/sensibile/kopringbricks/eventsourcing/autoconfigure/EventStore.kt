package me.sensibile.kopringbricks.eventsourcing.autoconfigure

/**
 * Append-only event stream storage.
 *
 * Implementations must reject blank stream IDs, negative expected versions, empty append batches,
 * and load requests with a version lower than one. A successful append stores events in stream
 * version order starting at expectedVersion + 1. If the current stream version differs from
 * expectedVersion, implementations must throw [EventStreamVersionConflictException].
 */
interface EventStore {
    fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult

    fun load(
        streamId: String,
        fromVersion: Long = 1,
    ): List<StoredEvent>
}
