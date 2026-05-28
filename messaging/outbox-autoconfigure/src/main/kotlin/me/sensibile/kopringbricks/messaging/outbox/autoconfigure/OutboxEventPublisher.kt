package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

fun interface OutboxEventPublisher {
    fun publish(event: OutboxEvent)
}
