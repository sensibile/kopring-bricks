package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.springframework.transaction.annotation.Transactional

open class EventSourcingTemplate(
    private val eventStore: EventStore,
    private val projections: List<EventProjection> = emptyList(),
) {
    constructor(eventStore: EventStore) : this(eventStore, emptyList())

    @Transactional
    open fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult = appendAndProject(streamId, expectedVersion, events, projections)

    @Transactional
    open fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
        projections: List<EventProjection>,
    ): EventAppendResult =
        appendAndProject(
            streamId = streamId,
            expectedVersion = expectedVersion,
            events = events,
            projections = (this.projections + projections).distinct(),
        )

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

    private fun appendAndProject(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
        projections: List<EventProjection>,
    ): EventAppendResult {
        val result = eventStore.append(streamId, expectedVersion, events)
        result.events.forEach { event ->
            projections
                .filter { projection -> projection.supports(event) }
                .forEach { projection -> projection.project(event) }
        }
        return result
    }
}
