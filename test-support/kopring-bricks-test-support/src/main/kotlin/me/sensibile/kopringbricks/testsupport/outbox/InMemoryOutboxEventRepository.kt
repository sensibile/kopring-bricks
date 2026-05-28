package me.sensibile.kopringbricks.testsupport.outbox

import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventRepository
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventStatus
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryOutboxEventRepository : OutboxEventRepository {
    private val eventsById = ConcurrentHashMap<String, OutboxEvent>()

    val events: List<OutboxEvent>
        get() = eventsById.values.sortedBy { it.createdAt }

    override fun append(event: OutboxEvent): OutboxEvent {
        eventsById[event.id] = event

        return event
    }

    override fun claimPending(
        limit: Int,
        now: Instant,
        claimTimeout: Duration,
    ): List<OutboxEvent> {
        require(limit > 0) { "claim limit must be greater than zero" }

        val claimExpiredBefore = now.minus(claimTimeout)

        return events
            .asSequence()
            .filter { it.isClaimable(now, claimExpiredBefore) }
            .take(limit)
            .map { event ->
                event.copy(
                    status = OutboxEventStatus.CLAIMED,
                    claimedAt = now,
                )
            }.onEach { claimedEvent ->
                eventsById[claimedEvent.id] = claimedEvent
            }.toList()
    }

    override fun markPublished(
        eventId: String,
        publishedAt: Instant,
    ) {
        eventsById.computeIfPresent(eventId) { _, event ->
            event.copy(
                status = OutboxEventStatus.PUBLISHED,
                publishedAt = publishedAt,
                lastError = null,
            )
        }
    }

    override fun markFailed(
        eventId: String,
        error: String,
        nextAttemptAt: Instant,
    ) {
        eventsById.computeIfPresent(eventId) { _, event ->
            event.copy(
                status = OutboxEventStatus.FAILED,
                nextAttemptAt = nextAttemptAt,
                claimedAt = null,
                retryCount = event.retryCount + 1,
                lastError = error,
            )
        }
    }

    fun clear() {
        eventsById.clear()
    }

    private fun OutboxEvent.isClaimable(
        now: Instant,
        claimExpiredBefore: Instant,
    ): Boolean =
        when (status) {
            OutboxEventStatus.PENDING,
            OutboxEventStatus.FAILED,
            -> availableAt <= now && nextAttemptAt <= now

            OutboxEventStatus.CLAIMED -> claimedAt?.let { it <= claimExpiredBefore } ?: false

            OutboxEventStatus.PUBLISHED -> false
        }
}
