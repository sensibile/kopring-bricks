package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import java.time.Duration
import java.time.Instant

interface OutboxEventRepository {
    fun append(event: OutboxEvent): OutboxEvent

    fun claimPending(
        limit: Int,
        now: Instant = Instant.now(),
        claimTimeout: Duration = Duration.ofMinutes(DEFAULT_CLAIM_TIMEOUT_MINUTES),
    ): List<OutboxEvent>

    fun markPublished(
        eventId: String,
        publishedAt: Instant = Instant.now(),
    )

    fun markFailed(
        eventId: String,
        error: String,
        nextAttemptAt: Instant,
    )
}

private const val DEFAULT_CLAIM_TIMEOUT_MINUTES = 5L
