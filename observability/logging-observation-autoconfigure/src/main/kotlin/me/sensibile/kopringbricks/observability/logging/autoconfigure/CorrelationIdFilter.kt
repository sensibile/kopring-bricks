package me.sensibile.kopringbricks.observability.logging.autoconfigure

import java.util.UUID

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter

class CorrelationIdFilter(
    private val properties: LoggingObservationProperties,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = request.getHeader(properties.correlation.requestHeaderName)
            ?.takeIf(String::isNotBlank)
            ?: generateRequestId()
        val previousValue = MDC.get(properties.mdc.requestIdKey)

        if (properties.mdc.enabled && requestId != null) {
            MDC.put(properties.mdc.requestIdKey, requestId)
        }
        if (requestId != null && properties.correlation.responseHeaderName.isNotBlank()) {
            response.setHeader(properties.correlation.responseHeaderName, requestId)
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            if (properties.mdc.enabled) {
                if (previousValue == null) {
                    MDC.remove(properties.mdc.requestIdKey)
                } else {
                    MDC.put(properties.mdc.requestIdKey, previousValue)
                }
            }
        }
    }

    private fun generateRequestId(): String? =
        if (properties.correlation.generateIfMissing) {
            UUID.randomUUID().toString()
        } else {
            null
        }
}
