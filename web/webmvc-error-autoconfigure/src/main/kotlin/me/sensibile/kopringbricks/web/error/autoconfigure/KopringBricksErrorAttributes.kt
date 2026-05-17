package me.sensibile.kopringbricks.web.error.autoconfigure

import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException
import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailsProperties

import org.slf4j.MDC
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.webmvc.error.DefaultErrorAttributes
import org.springframework.web.context.request.WebRequest

class KopringBricksErrorAttributes(
    private val problemDetailsProperties: ProblemDetailsProperties,
    private val webMvcErrorProperties: WebMvcErrorProperties,
) : DefaultErrorAttributes() {

    override fun getErrorAttributes(
        webRequest: WebRequest,
        options: ErrorAttributeOptions,
    ): Map<String, Any> {
        val attributes = linkedMapOf<String, Any>()
        super.getErrorAttributes(webRequest, options).forEach { (key, value) ->
            if (value != null) {
                attributes[key] = value
            }
        }
        val error = getError(webRequest)

        if (error is ApiException) {
            attributes[problemDetailsProperties.codePropertyName] = error.code
        } else if (!attributes.containsKey(problemDetailsProperties.codePropertyName)) {
            attributes[problemDetailsProperties.codePropertyName] = resolveCode(attributes)
        }

        val requestId = MDC.get(webMvcErrorProperties.requestIdMdcKey)
        if (!requestId.isNullOrBlank()) {
            attributes[problemDetailsProperties.requestIdPropertyName] = requestId
        }

        return attributes
    }

    private fun resolveCode(attributes: Map<String, Any>): String {
        val status = attributes["status"] as? Int
        return when (status) {
            in 400..499 -> "HTTP_$status"
            in 500..599 -> webMvcErrorProperties.internalErrorCode
            else -> "HTTP_ERROR"
        }
    }
}
