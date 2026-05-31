package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import java.time.Instant
import java.util.UUID

data class EventStoreEvent(
    val id: String = UUID.randomUUID().toString(),
    val eventType: String,
    val eventVersion: Int = 1,
    val payloadJson: String,
    val metadataJson: String? = null,
    val occurredAt: Instant = Instant.now(),
)

data class StoredEvent(
    val id: String,
    val streamId: String,
    val streamVersion: Long,
    val eventType: String,
    val eventVersion: Int = 1,
    val payloadJson: String,
    val metadataJson: String? = null,
    val occurredAt: Instant,
)

data class EventAppendResult(
    val streamId: String,
    val previousVersion: Long,
    val currentVersion: Long,
    val events: List<StoredEvent>,
)
