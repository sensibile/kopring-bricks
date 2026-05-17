package me.sensibile.kopringbricks.web.problem.autoconfigure

import java.net.URI

import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

open class ApiException(
    status: HttpStatusCode,
    val code: String,
    detail: String,
    title: String? = null,
    type: URI? = null,
    cause: Throwable? = null,
    properties: Map<String, Any> = emptyMap(),
) : ErrorResponseException(
    status,
    ProblemDetail.forStatusAndDetail(status, detail).apply {
        title?.let(::setTitle)
        type?.let(::setType)
        setProperty("code", code)
        properties.forEach(::setProperty)
    },
    cause,
)
