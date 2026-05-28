package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import java.time.Clock
import java.time.Instant

class OutboxPollingService(
    private val repository: OutboxEventRepository,
    private val publisher: OutboxEventPublisher,
    private val retryPolicy: OutboxRetryPolicy,
    private val properties: OutboxProperties,
    private val clock: Clock,
) {
    fun poll(now: Instant = clock.instant()): OutboxPollingResult {
        val events =
            repository.claimPending(
                limit = properties.polling.claimLimit,
                now = now,
                claimTimeout = properties.polling.claimTimeout,
            )

        val results = events.map { event -> publish(event, now) }

        return OutboxPollingResult(
            claimed = events.size,
            published = results.count { it == OutboxPublishResult.PUBLISHED },
            failed = results.count { it == OutboxPublishResult.FAILED },
        )
    }

    private fun publish(
        event: OutboxEvent,
        now: Instant,
    ): OutboxPublishResult =
        runCatching {
            publisher.publish(event)
        }.fold(
            onSuccess = {
                repository.markPublished(event.id, now)
                OutboxPublishResult.PUBLISHED
            },
            onFailure = { exception ->
                repository.markFailed(
                    eventId = event.id,
                    error = exception.message ?: exception::class.java.name,
                    nextAttemptAt = retryPolicy.nextAttemptAt(event, now),
                )
                OutboxPublishResult.FAILED
            },
        )
}

data class OutboxPollingResult(
    val claimed: Int,
    val published: Int,
    val failed: Int,
)

private enum class OutboxPublishResult {
    PUBLISHED,
    FAILED,
}
