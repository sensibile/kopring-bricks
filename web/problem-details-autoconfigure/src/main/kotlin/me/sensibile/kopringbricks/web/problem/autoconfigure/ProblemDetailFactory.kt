package me.sensibile.kopringbricks.web.problem.autoconfigure

import java.net.URI

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail

class ProblemDetailFactory(
    private val properties: ProblemDetailsProperties,
) {

    fun create(
        status: HttpStatusCode,
        code: String,
        detail: String,
        title: String? = null,
        instance: URI? = null,
        extensions: Map<String, Any> = emptyMap(),
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            setTitle(title ?: resolveTitle(status))
            setType(resolveType(code))
            instance?.let(::setInstance)
            setProperty(this@ProblemDetailFactory.properties.codePropertyName, code)
            extensions.forEach(::setProperty)
        }

    private fun resolveType(code: String): URI =
        URI.create("${properties.typeBaseUri.trimEnd('/')}/${code.lowercase().replace('_', '-')}")

    private fun resolveTitle(status: HttpStatusCode): String =
        HttpStatus.resolve(status.value())?.reasonPhrase ?: "HTTP ${status.value()}"
}
