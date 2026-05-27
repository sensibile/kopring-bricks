package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class LoggingOutboxEventRepository : OutboxEventRepository {
    override fun append(event: OutboxEvent): OutboxEvent {
        logger.info(
            "Outbox event appended. id={}, aggregateType={}, aggregateId={}, eventType={}",
            event.id,
            event.aggregateType,
            event.aggregateId,
            event.eventType,
        )

        return event
    }

    override fun claimPending(
        limit: Int,
        now: Instant,
        claimTimeout: Duration,
    ): List<OutboxEvent> = emptyList()

    override fun markPublished(
        eventId: String,
        publishedAt: Instant,
    ) = Unit

    override fun markFailed(
        eventId: String,
        error: String,
        nextAttemptAt: Instant,
    ) = Unit

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingOutboxEventRepository::class.java)
    }
}
