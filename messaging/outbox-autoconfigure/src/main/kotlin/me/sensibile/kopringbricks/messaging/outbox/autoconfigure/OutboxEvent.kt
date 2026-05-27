package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import java.time.Instant
import java.util.UUID

data class OutboxEvent(
    val id: String = UUID.randomUUID().toString(),
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val eventVersion: Int = 1,
    val payloadJson: String,
    val headersJson: String? = null,
    val status: OutboxEventStatus = OutboxEventStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val availableAt: Instant = createdAt,
    val nextAttemptAt: Instant = availableAt,
    val claimedAt: Instant? = null,
    val publishedAt: Instant? = null,
    val retryCount: Int = 0,
    val lastError: String? = null,
)

enum class OutboxEventStatus {
    PENDING,
    CLAIMED,
    PUBLISHED,
    FAILED,
}
