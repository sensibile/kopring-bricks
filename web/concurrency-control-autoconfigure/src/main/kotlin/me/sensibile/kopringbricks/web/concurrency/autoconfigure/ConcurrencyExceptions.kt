package me.sensibile.kopringbricks.web.concurrency.autoconfigure

import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException
import org.springframework.http.HttpStatus

class VersionConflictException(
    detail: String = "Resource was modified by another request",
    currentVersion: Any? = null,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "VERSION_CONFLICT",
        detail = detail,
        title = "Version conflict",
        properties = currentVersion?.let { mapOf("currentVersion" to it) } ?: emptyMap(),
    )

class PreconditionRequiredException(
    detail: String = "Missing required If-Match header",
) : ApiException(
        status = HttpStatus.PRECONDITION_REQUIRED,
        code = "PRECONDITION_REQUIRED",
        detail = detail,
        title = "Precondition required",
    )

class PreconditionFailedException(
    detail: String = "Resource version does not match the If-Match header",
    currentETag: String? = null,
) : ApiException(
        status = HttpStatus.PRECONDITION_FAILED,
        code = "PRECONDITION_FAILED",
        detail = detail,
        title = "Precondition failed",
        properties = currentETag?.let { mapOf("currentETag" to it) } ?: emptyMap(),
    )

class IdempotencyConflictException(
    idempotencyKey: String,
    detail: String = "Idempotency key is already associated with a different request",
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "IDEMPOTENCY_CONFLICT",
        detail = detail,
        title = "Idempotency conflict",
        properties = mapOf("idempotencyKey" to idempotencyKey),
    )
