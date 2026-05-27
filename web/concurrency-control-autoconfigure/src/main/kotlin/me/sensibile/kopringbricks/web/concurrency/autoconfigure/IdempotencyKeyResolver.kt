package me.sensibile.kopringbricks.web.concurrency.autoconfigure

import org.springframework.http.HttpHeaders

class IdempotencyKeyResolver(
    private val properties: ConcurrencyControlProperties,
) {
    fun resolve(headers: HttpHeaders): String? =
        headers
            .getFirst(properties.idempotency.headerName)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    fun require(headers: HttpHeaders): String =
        resolve(headers)
            ?: throw PreconditionRequiredException("Missing required ${properties.idempotency.headerName} header")
}
