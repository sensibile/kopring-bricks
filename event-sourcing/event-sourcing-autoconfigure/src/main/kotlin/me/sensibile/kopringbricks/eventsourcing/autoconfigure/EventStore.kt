package me.sensibile.kopringbricks.eventsourcing.autoconfigure

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
