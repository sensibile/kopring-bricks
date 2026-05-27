package me.sensibile.kopringbricks.web.concurrency.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.concurrency-control")
data class ConcurrencyControlProperties(
    val enabled: Boolean = true,
    val etag: ETag = ETag(),
    val idempotency: Idempotency = Idempotency(),
) {
    data class ETag(
        val strong: Boolean = true,
        val missingIfMatchDetail: String = "Missing required If-Match header",
        val preconditionFailedDetail: String = "Resource version does not match the If-Match header",
    )

    data class Idempotency(
        val headerName: String = "Idempotency-Key",
        val conflictDetail: String = "Idempotency key is already associated with a different request",
    )
}
