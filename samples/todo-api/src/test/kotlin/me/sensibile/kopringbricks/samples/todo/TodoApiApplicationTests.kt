package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.httpclient.autoconfigure.VtRestClientFactory
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxPollingService
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
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.web.client.RestClient
import kotlin.test.Test
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
class TodoApiApplicationTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var restClientBuilder: RestClient.Builder

    @Autowired
    private lateinit var restClientFactory: VtRestClientFactory

    @Autowired
    private lateinit var repository: TodoRepository

    @Autowired
    private lateinit var auditEvents: RecordingAuditEventPublisher

    @Autowired
    private lateinit var outboxRepository: InMemoryOutboxEventRepository

    @Autowired
    private lateinit var outboxPublisher: RecordingOutboxEventPublisher

    @Autowired
    private lateinit var outboxPollingService: OutboxPollingService

    @BeforeEach
    fun resetState() {
        repository.clear()
        auditEvents.clear()
        outboxRepository.clear()
        outboxPublisher.clear()
    }

    @Test
    fun `starts with kopring bricks starters`() {
        assertAll(
            { assertNotNull(cacheManager.getCache("todos")) },
            { assertNotNull(restClientBuilder) },
            { assertNotNull(restClientFactory) },
        )
    }

    @Test
    fun `creates completes and lists todos`() {
        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":"write sample"}"""
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.title") { value("write sample") }
                jsonPath("$.completed") { value(false) }
                jsonPath("$.version") { value(1) }
            }

        val etag =
            mockMvc
                .get("/todos/1")
                .andExpect {
                    status { isOk() }
                    header { string(HttpHeaders.ETAG, "\"1\"") }
                }.andReturn()
                .response
                .getHeader(HttpHeaders.ETAG)
                .let(::requireNotNull)

        mockMvc
            .patch("/todos/1/complete") {
                header(HttpHeaders.IF_MATCH, etag)
            }.andExpect {
                status { isOk() }
                jsonPath("$.completed") { value(true) }
                jsonPath("$.version") { value(2) }
                header { string(HttpHeaders.ETAG, "\"2\"") }
            }

        mockMvc
            .get("/todos")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].title") { value("write sample") }
                jsonPath("$[0].completed") { value(true) }
                jsonPath("$[0].version") { value(2) }
            }
    }

    @Test
    fun `records audit and outbox events for todo changes`() {
        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":"publish sample"}"""
            }.andExpect {
                status { isCreated() }
            }

        val etag =
            mockMvc
                .get("/todos/1")
                .andReturn()
                .response
                .getHeader(HttpHeaders.ETAG)
                .let(::requireNotNull)

        mockMvc
            .patch("/todos/1/complete") {
                header(HttpHeaders.IF_MATCH, etag)
            }.andExpect {
                status { isOk() }
            }

        val pollingResult = outboxPollingService.poll()

        assertAll(
            { assertThat(auditEvents.events.map { it.action }).containsExactly("todo.created", "todo.completed") },
            { assertThat(outboxRepository.events).hasSize(2) },
            { assertThat(pollingResult.published).isEqualTo(2) },
            {
                assertThat(outboxPublisher.events.map { it.eventType })
                    .containsExactly("todo.created", "todo.completed")
            },
        )
    }

    @Test
    fun `requires matching etag to complete todo`() {
        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":"guarded sample"}"""
            }.andExpect {
                status { isCreated() }
            }

        mockMvc
            .patch("/todos/1/complete")
            .andExpect {
                status { isPreconditionRequired() }
                jsonPath("$.code") { value("PRECONDITION_REQUIRED") }
            }

        mockMvc
            .patch("/todos/1/complete") {
                header(HttpHeaders.IF_MATCH, "\"999\"")
            }.andExpect {
                status { isPreconditionFailed() }
                jsonPath("$.code") { value("PRECONDITION_FAILED") }
            }
    }

    @Test
    fun `returns problem detail for missing todo`() {
        mockMvc
            .get("/todos/404")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("TODO_NOT_FOUND") }
                jsonPath("$.title") { value("Todo not found") }
            }
    }

    @Test
    fun `returns validation problem details`() {
        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title":""}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("VALIDATION_FAILED") }
                jsonPath("$.violations[0].field") { value("title") }
            }
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
}
