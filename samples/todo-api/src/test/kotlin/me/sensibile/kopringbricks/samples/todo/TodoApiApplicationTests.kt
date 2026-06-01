package me.sensibile.kopringbricks.samples.todo

import com.fasterxml.jackson.databind.ObjectMapper
import me.sensibile.kopringbricks.httpclient.autoconfigure.VtRestClientFactory
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxPollingService
import me.sensibile.kopringbricks.testsupport.audit.RecordingAuditEventPublisher
import me.sensibile.kopringbricks.testsupport.eventsourcing.InMemoryEventStore
import me.sensibile.kopringbricks.testsupport.outbox.InMemoryOutboxEventRepository
import me.sensibile.kopringbricks.testsupport.outbox.RecordingOutboxEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.web.client.RestClient
import kotlin.test.Test
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@Import(TodoSampleTestSupportConfiguration::class)
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
    private lateinit var eventStore: InMemoryEventStore

    @Autowired
    private lateinit var outboxRepository: InMemoryOutboxEventRepository

    @Autowired
    private lateinit var outboxPublisher: RecordingOutboxEventPublisher

    @Autowired
    private lateinit var outboxPollingService: OutboxPollingService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun resetState() {
        repository.clear()
        auditEvents.clear()
        eventStore.clear()
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

        val etag = todoEtag(id = 1)
        assertThat(etag).isEqualTo("\"1\"")

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
    fun `records event sourcing audit and outbox events for todo changes`() {
        createTodo("publish sample")

        val etag = todoEtag(id = 1)

        mockMvc
            .patch("/todos/1/complete") {
                header(HttpHeaders.IF_MATCH, etag)
            }.andExpect {
                status { isOk() }
            }

        val pollingResult = outboxPollingService.poll()

        assertAll(
            {
                assertThat(eventStore.load("todo-1").map { it.eventType })
                    .containsExactly("todo.created", "todo.completed")
            },
            {
                assertThat(eventStore.load("todo-1").map { it.streamVersion })
                    .containsExactly(1L, 2L)
            },
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
    fun `serializes audit and outbox payloads with json escaping`() {
        val title = "quoted \"todo\" with newline\nand backspace\b"

        createTodo(title)

        val auditMetadata = requireNotNull(auditEvents.events.single().metadataJson)
        val eventStorePayload = eventStore.load("todo-1").single().payloadJson
        val outboxPayload = outboxRepository.events.single().payloadJson

        assertAll(
            { assertThat(auditMetadata).contains("""\"todo\"""") },
            { assertThat(auditMetadata).contains("""\n""") },
            { assertThat(auditMetadata).contains("""\b""") },
            { assertThat(eventStorePayload).isEqualTo(auditMetadata) },
            { assertThat(outboxPayload).isEqualTo(auditMetadata) },
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

    private fun createTodo(title: String): MvcResult =
        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("title" to title))
            }.andExpect {
                status { isCreated() }
            }.andReturn()

    private fun todoEtag(id: Long): String =
        mockMvc
            .get("/todos/$id")
            .andExpect {
                status { isOk() }
                header { exists(HttpHeaders.ETAG) }
            }.andReturn()
            .response
            .getHeader(HttpHeaders.ETAG)
            .let(::requireNotNull)
}
