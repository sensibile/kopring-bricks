package me.sensibile.kopringbricks.testsupport.outbox

import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventPublisher
import java.util.concurrent.CopyOnWriteArrayList

class RecordingOutboxEventPublisher : OutboxEventPublisher {
    private val recordedEvents = CopyOnWriteArrayList<OutboxEvent>()
    private val failingEventIds = CopyOnWriteArrayList<String>()

    val events: List<OutboxEvent>
        get() = recordedEvents.toList()

    override fun publish(event: OutboxEvent) {
        if (event.id in failingEventIds) {
            throw IllegalStateException("outbox publish failed for event ${event.id}")
        }

        recordedEvents += event
    }

    fun fail(eventId: String) {
        failingEventIds += eventId
    }

    fun clear() {
        recordedEvents.clear()
        failingEventIds.clear()
    }
}
