package me.sensibile.kopringbricks.testsupport.outbox

import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventStatus
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import java.time.Instant
import kotlin.test.Test

class InMemoryOutboxEventRepositoryTests {
    @Test
    fun `claims pending events and marks them published`() {
        val repository = InMemoryOutboxEventRepository()
        val event =
            OutboxEvent(
                id = "event-1",
                aggregateType = "todo",
                aggregateId = "1",
                eventType = "todo.created",
                payloadJson = """{"id":1}""",
                createdAt = NOW,
            )

        repository.append(event)

        val claimed =
            repository.claimPending(
                limit = 1,
                now = NOW,
                claimTimeout = Duration.ofMinutes(5),
            )

        assertThat(claimed).hasSize(1)
        assertThat(claimed.single().status).isEqualTo(OutboxEventStatus.CLAIMED)

        repository.markPublished(event.id, NOW)

        assertThat(repository.events.single().status).isEqualTo(OutboxEventStatus.PUBLISHED)
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-05-29T00:00:00Z")
    }
}
