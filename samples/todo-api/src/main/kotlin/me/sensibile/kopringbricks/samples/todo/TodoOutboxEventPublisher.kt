package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TodoOutboxEventPublisher : OutboxEventPublisher {
    override fun publish(event: OutboxEvent) {
        logger.info(
            "Published todo outbox event. id={}, aggregateId={}, eventType={}",
            event.id,
            event.aggregateId,
            event.eventType,
        )
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(TodoOutboxEventPublisher::class.java)
    }
}
