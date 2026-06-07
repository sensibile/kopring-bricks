package me.sensibile.kopringbricks.eventsourcing.autoconfigure

/**
 * Synchronous projection hook for events appended through [EventSourcingTemplate].
 *
 * Implementations own their read model schema and persistence. Exceptions thrown from
 * [project] propagate to the caller and roll back the surrounding append transaction.
 */
interface EventProjection {
    fun supports(event: StoredEvent): Boolean = true

    fun project(event: StoredEvent)
}
