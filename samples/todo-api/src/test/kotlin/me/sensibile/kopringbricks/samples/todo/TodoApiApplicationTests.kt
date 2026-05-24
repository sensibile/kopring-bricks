package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.httpclient.autoconfigure.VtRestClientFactory
import org.junit.jupiter.api.Assertions.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
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
            }

        mockMvc
            .patch("/todos/1/complete")
            .andExpect {
                status { isOk() }
                jsonPath("$.completed") { value(true) }
            }

        mockMvc
            .get("/todos")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].title") { value("write sample") }
                jsonPath("$[0].completed") { value(true) }
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
}
