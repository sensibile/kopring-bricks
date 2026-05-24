package me.sensibile.kopringbricks.web.error.autoconfigure

import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException
import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailFactory
import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailsProperties
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class KopringBricksWebMvcExceptionHandler(
    private val problemDetailFactory: ProblemDetailFactory,
    private val problemDetailsProperties: ProblemDetailsProperties,
    private val webMvcErrorProperties: WebMvcErrorProperties,
) : ResponseEntityExceptionHandler() {
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val violations =
            ex.bindingResult.fieldErrors.map {
                mapOf(
                    "field" to it.field,
                    "message" to (it.defaultMessage ?: "Invalid value"),
                    "rejectedValue" to it.rejectedValue,
                )
            }
        val problem =
            problemDetailFactory.create(
                status = status,
                code = webMvcErrorProperties.validationErrorCode,
                detail = "Request validation failed",
                title = "Validation failed",
                extensions = mapOf("violations" to violations),
            )

        enrich(problem)

        return ResponseEntity.status(status).headers(headers).body(problem)
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ProblemDetail> {
        val problem = ex.body
        problem.setProperty(problemDetailsProperties.codePropertyName, ex.code)
        enrich(problem)

        return ResponseEntity.status(ex.statusCode).headers(ex.headers).body(problem)
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(ex: Throwable): ResponseEntity<ProblemDetail> {
        val detail =
            if (webMvcErrorProperties.includeExceptionMessage) {
                ex.message ?: "Unexpected server error"
            } else {
                "Unexpected server error"
            }
        val problem =
            problemDetailFactory.create(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                code = webMvcErrorProperties.internalErrorCode,
                detail = detail,
                title = "Internal server error",
            )
        enrich(problem)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }

    override fun createResponseEntity(
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val responseBody =
            when (body) {
                is ProblemDetail -> body.apply(::enrich)
                is ErrorResponse -> body.body.apply(::enrich)
                else -> body
            }

        return super.createResponseEntity(responseBody, headers, statusCode, request)
    }

    private fun enrich(problem: ProblemDetail) {
        if (problem.properties?.containsKey(problemDetailsProperties.codePropertyName) != true) {
            problem.setProperty(problemDetailsProperties.codePropertyName, resolveCode(problem.status))
        }
        val requestId = MDC.get(webMvcErrorProperties.requestIdMdcKey)
        if (!requestId.isNullOrBlank()) {
            problem.setProperty(problemDetailsProperties.requestIdPropertyName, requestId)
        }
    }

    private fun resolveCode(status: Int): String =
        when (status) {
            in CLIENT_ERROR_STATUS_RANGE -> "HTTP_$status"
            in SERVER_ERROR_STATUS_RANGE -> webMvcErrorProperties.internalErrorCode
            else -> "HTTP_ERROR"
        }

    private companion object {
        private const val CLIENT_ERROR_STATUS_MIN = 400
        private const val CLIENT_ERROR_STATUS_MAX = 499
        private const val SERVER_ERROR_STATUS_MIN = 500
        private const val SERVER_ERROR_STATUS_MAX = 599

        private val CLIENT_ERROR_STATUS_RANGE = CLIENT_ERROR_STATUS_MIN..CLIENT_ERROR_STATUS_MAX
        private val SERVER_ERROR_STATUS_RANGE = SERVER_ERROR_STATUS_MIN..SERVER_ERROR_STATUS_MAX
    }
}
