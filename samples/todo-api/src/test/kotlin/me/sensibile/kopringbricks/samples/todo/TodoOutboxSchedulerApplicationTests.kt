package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.DefaultOutboxScheduler
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventStatus
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxScheduler
import me.sensibile.kopringbricks.testsupport.audit.RecordingAuditEventPublisher
import me.sensibile.kopringbricks.testsupport.outbox.InMemoryOutboxEventRepository
import me.sensibile.kopringbricks.testsupport.outbox.RecordingOutboxEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Duration
import kotlin.test.Test

@SpringBootTest(
    properties = [
        "kopring.bricks.outbox.scheduler.enabled=true",
        "kopring.bricks.outbox.scheduler.fixed-delay=50ms",
    ],
)
@AutoConfigureMockMvc
class TodoOutboxSchedulerApplicationTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: TodoRepository

    @Autowired
    private lateinit var auditEvents: RecordingAuditEventPublisher

    @Autowired
    private lateinit var outboxRepository: InMemoryOutboxEventRepository

    @Autowired
    private lateinit var outboxPublisher: RecordingOutboxEventPublisher

    @Autowired
    private lateinit var outboxScheduler: OutboxScheduler

    @Autowired
    private lateinit var taskScheduler: ThreadPoolTaskScheduler

    @BeforeEach
    fun resetState() {
        repository.clear()
        auditEvents.clear()
        outboxRepository.clear()
        outboxPublisher.clear()
    }

    @Test
    fun `publishes todo outbox events with starter scheduler`() {
        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":"scheduled publish"}"""
            }.andExpect {
                status { isCreated() }
            }

        val publishedEvents = awaitPublishedEvents(count = 1)

        assertAll(
            { assertThat(outboxScheduler).isInstanceOf(DefaultOutboxScheduler::class.java) },
            { assertThat(taskScheduler.threadNamePrefix).isEqualTo("kopring-bricks-outbox-") },
            { assertThat(publishedEvents.map { it.eventType }).containsExactly("todo.created") },
            { assertThat(outboxRepository.events.single().status).isEqualTo(OutboxEventStatus.PUBLISHED) },
        )
    }

    private fun awaitPublishedEvents(count: Int): List<OutboxEvent> {
        val deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos()
        var events = outboxPublisher.events

        while (events.size < count && System.nanoTime() < deadline) {
            Thread.sleep(AWAIT_INTERVAL.toMillis())
            events = outboxPublisher.events
        }

        return events
    }

    @TestConfiguration
    class TestSupportConfiguration {
        @Bean
        @Primary
        fun recordingAuditEventPublisher(): RecordingAuditEventPublisher = RecordingAuditEventPublisher()

        @Bean
        @Primary
        fun inMemoryOutboxEventRepository(): InMemoryOutboxEventRepository = InMemoryOutboxEventRepository()

        @Bean
        @Primary
        fun recordingOutboxEventPublisher(): RecordingOutboxEventPublisher = RecordingOutboxEventPublisher()
    }

    private companion object {
        private val AWAIT_TIMEOUT = Duration.ofSeconds(5)
        private val AWAIT_INTERVAL = Duration.ofMillis(50)
    }
}
