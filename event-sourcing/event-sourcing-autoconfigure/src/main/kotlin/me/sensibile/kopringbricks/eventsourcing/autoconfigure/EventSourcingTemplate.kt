package me.sensibile.kopringbricks.eventsourcing.autoconfigure

class EventSourcingTemplate(
    private val eventStore: EventStore,
) {
    fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult = eventStore.append(streamId, expectedVersion, events)

    fun load(
        streamId: String,
        fromVersion: Long = 1,
    ): List<StoredEvent> = eventStore.load(streamId, fromVersion)

    fun <S> fold(
        streamId: String,
        initialState: S,
        fromVersion: Long = 1,
        apply: (S, StoredEvent) -> S,
    ): S =
        load(streamId, fromVersion)
            .fold(initialState, apply)
}
