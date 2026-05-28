package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test

class OutboxPollingServiceTests {
    @Test
    fun `publishes claimed events and marks them published`() {
        val event = outboxEvent(id = "event-1")
        val repository = StubOutboxEventRepository(eventsToClaim = listOf(event))
        val publisher = RecordingOutboxEventPublisher()
        val service = outboxPollingService(repository, publisher)

        val result = service.poll(NOW)

        assertThat(result).isEqualTo(
            OutboxPollingResult(
                claimed = SINGLE_EVENT_COUNT,
                published = SINGLE_EVENT_COUNT,
                failed = NO_EVENTS_COUNT,
            ),
        )
        assertThat(repository.claims).containsExactly(Claim(DEFAULT_CLAIM_LIMIT, NOW, DEFAULT_CLAIM_TIMEOUT))
        assertThat(publisher.publishedEvents).containsExactly(event)
        assertThat(repository.publishedEventIds).containsExactly(event.id)
        assertThat(repository.failedEvents).isEmpty()
    }

    @Test
    fun `marks failed events with retry delay when publisher fails`() {
        val event = outboxEvent(id = "event-1", retryCount = FIRST_RETRY_COUNT)
        val repository = StubOutboxEventRepository(eventsToClaim = listOf(event))
        val publisher = RecordingOutboxEventPublisher(failingEventIds = setOf(event.id))
        val service = outboxPollingService(repository, publisher)

        val result = service.poll(NOW)

        assertThat(result).isEqualTo(
            OutboxPollingResult(
                claimed = SINGLE_EVENT_COUNT,
                published = NO_EVENTS_COUNT,
                failed = SINGLE_EVENT_COUNT,
            ),
        )
        assertThat(repository.publishedEventIds).isEmpty()
        assertThat(repository.failedEvents).containsExactly(
            FailedEvent(
                eventId = event.id,
                error = PUBLISH_FAILURE_MESSAGE,
                nextAttemptAt = NOW.plus(Duration.ofSeconds(SECOND_RETRY_DELAY_SECONDS)),
            ),
        )
    }

    @Test
    fun `retry policy caps delay at max delay`() {
        val properties =
            OutboxProperties(
                retry =
                    OutboxProperties.Retry(
                        initialDelay = Duration.ofSeconds(INITIAL_RETRY_DELAY_SECONDS),
                        maxDelay = Duration.ofSeconds(MAX_RETRY_DELAY_SECONDS),
                    ),
            )
        val retryPolicy = OutboxRetryPolicy(properties)
        val event = outboxEvent(retryCount = MANY_RETRY_COUNT)

        val nextAttemptAt = retryPolicy.nextAttemptAt(event, NOW)

        assertThat(nextAttemptAt).isEqualTo(NOW.plus(Duration.ofSeconds(MAX_RETRY_DELAY_SECONDS)))
    }

    private fun outboxPollingService(
        repository: OutboxEventRepository,
        publisher: OutboxEventPublisher,
        properties: OutboxProperties = OutboxProperties(),
    ): OutboxPollingService =
        OutboxPollingService(
            repository = repository,
            publisher = publisher,
            retryPolicy = OutboxRetryPolicy(properties),
            properties = properties,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
        )

    private fun outboxEvent(
        id: String = "event-id",
        retryCount: Int = NO_EVENTS_COUNT,
    ): OutboxEvent =
        OutboxEvent(
            id = id,
            aggregateType = "feature-rule",
            aggregateId = "rule-1",
            eventType = "feature-rule.updated",
            payloadJson = """{"enabled":true}""",
            retryCount = retryCount,
        )

    private class StubOutboxEventRepository(
        private val eventsToClaim: List<OutboxEvent>,
    ) : OutboxEventRepository {
        val claims = mutableListOf<Claim>()
        val publishedEventIds = mutableListOf<String>()
        val failedEvents = mutableListOf<FailedEvent>()

        override fun append(event: OutboxEvent): OutboxEvent = event

        override fun claimPending(
            limit: Int,
            now: Instant,
            claimTimeout: Duration,
        ): List<OutboxEvent> {
            claims += Claim(limit, now, claimTimeout)

            return eventsToClaim
        }

        override fun markPublished(
            eventId: String,
            publishedAt: Instant,
        ) {
            publishedEventIds += eventId
        }

        override fun markFailed(
            eventId: String,
            error: String,
            nextAttemptAt: Instant,
        ) {
            failedEvents += FailedEvent(eventId, error, nextAttemptAt)
        }
    }

    private class RecordingOutboxEventPublisher(
        private val failingEventIds: Set<String> = emptySet(),
    ) : OutboxEventPublisher {
        val publishedEvents = mutableListOf<OutboxEvent>()

        override fun publish(event: OutboxEvent) {
            if (event.id in failingEventIds) {
                throw IllegalStateException(PUBLISH_FAILURE_MESSAGE)
            }

            publishedEvents += event
        }
    }

    private data class Claim(
        val limit: Int,
        val now: Instant,
        val claimTimeout: Duration,
    )

    private data class FailedEvent(
        val eventId: String,
        val error: String,
        val nextAttemptAt: Instant,
    )

    private companion object {
        private val NOW: Instant = Instant.parse("2026-05-28T00:00:00Z")
        private val DEFAULT_CLAIM_TIMEOUT: Duration = Duration.ofMinutes(5)
        private const val DEFAULT_CLAIM_LIMIT = 100
        private const val SINGLE_EVENT_COUNT = 1
        private const val NO_EVENTS_COUNT = 0
        private const val FIRST_RETRY_COUNT = 1
        private const val INITIAL_RETRY_DELAY_SECONDS = 5L
        private const val SECOND_RETRY_DELAY_SECONDS = 10L
        private const val MAX_RETRY_DELAY_SECONDS = 30L
        private const val MANY_RETRY_COUNT = 10
        private const val PUBLISH_FAILURE_MESSAGE = "publish failed"
    }
}
