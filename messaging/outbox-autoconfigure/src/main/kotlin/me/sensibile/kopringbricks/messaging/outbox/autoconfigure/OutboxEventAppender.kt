package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

class OutboxEventAppender(
    private val repository: OutboxEventRepository,
) {
    fun append(event: OutboxEvent): OutboxEvent = repository.append(event)
}
