package me.sensibile.kopringbricks.observability.logging.autoconfigure

import org.slf4j.MDC
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.web.client.RestClient

class CorrelationIdRestClientCustomizer(
    private val properties: LoggingObservationProperties,
) : RestClientCustomizer {

    override fun customize(builder: RestClient.Builder) {
        builder.requestInterceptor { request, body, execution ->
            val requestId = MDC.get(properties.mdc.requestIdKey)
            val headerName = properties.correlation.requestHeaderName

            if (!requestId.isNullOrBlank() && headerName.isNotBlank() && request.headers[headerName].isNullOrEmpty()) {
                request.headers.add(headerName, requestId)
            }

            execution.execute(request, body)
        }
    }
}
