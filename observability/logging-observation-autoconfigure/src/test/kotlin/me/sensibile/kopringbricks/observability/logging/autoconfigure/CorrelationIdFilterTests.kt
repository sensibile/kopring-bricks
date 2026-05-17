package me.sensibile.kopringbricks.observability.logging.autoconfigure

import jakarta.servlet.FilterChain

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class CorrelationIdFilterTests {

    @Test
    fun `stores request id in mdc and response header`() {
        val properties = LoggingObservationProperties()
        val filter = CorrelationIdFilter(properties)
        val request = MockHttpServletRequest().apply {
            addHeader("X-Request-Id", "req-1")
        }
        val response = MockHttpServletResponse()
        var valueInsideChain: String? = null
        val chain = FilterChain { _, _ ->
            valueInsideChain = MDC.get("request_id")
        }

        filter.doFilter(request, response, chain)

        assertThat(valueInsideChain).isEqualTo("req-1")
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("req-1")
        assertThat(MDC.get("request_id")).isNull()
    }
}
