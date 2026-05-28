package me.sensibile.kopringbricks.testsupport.outbox

import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventStatus
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
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

    @Test
    fun `does not claim the same event concurrently`() {
        val repository = InMemoryOutboxEventRepository()
        val events =
            (1..EVENT_COUNT).map { index ->
                outboxEvent(id = "event-$index")
            }
        events.forEach(repository::append)
        val executor = Executors.newFixedThreadPool(WORKER_COUNT)

        val claimedIds =
            try {
                executor
                    .invokeAll(
                        List(WORKER_COUNT) {
                            Callable {
                                repository
                                    .claimPending(
                                        limit = CLAIM_LIMIT,
                                        now = NOW,
                                        claimTimeout = Duration.ofMinutes(5),
                                    ).map { it.id }
                            }
                        },
                    ).flatMap { future -> future.get() }
            } finally {
                executor.shutdownNow()
            }

        assertThat(claimedIds).doesNotHaveDuplicates()
        assertThat(claimedIds).hasSize(EVENT_COUNT)
    }

    private fun outboxEvent(id: String): OutboxEvent =
        OutboxEvent(
            id = id,
            aggregateType = "todo",
            aggregateId = "1",
            eventType = "todo.created",
            payloadJson = """{"id":1}""",
            createdAt = NOW,
        )

    private companion object {
        private val NOW: Instant = Instant.parse("2026-05-29T00:00:00Z")
        private const val EVENT_COUNT = 20
        private const val WORKER_COUNT = 10
        private const val CLAIM_LIMIT = 2
    }
}
