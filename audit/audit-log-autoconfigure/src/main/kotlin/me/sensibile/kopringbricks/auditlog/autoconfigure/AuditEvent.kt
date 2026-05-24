package me.sensibile.kopringbricks.auditlog.autoconfigure

import java.time.Instant
import java.util.UUID

data class AuditEvent(
    val id: String = UUID.randomUUID().toString(),
    val occurredAt: Instant = Instant.now(),
    val actor: AuditActor,
    val action: String,
    val target: AuditTarget,
    val outcome: AuditOutcome = AuditOutcome.SUCCESS,
    val traceId: String? = null,
    val requestId: String? = null,
    val reason: String? = null,
    val metadataJson: String? = null,
    val beforeStateJson: String? = null,
    val afterStateJson: String? = null,
)

data class AuditActor(
    val type: String,
    val id: String,
    val name: String? = null,
)

data class AuditTarget(
    val type: String,
    val id: String,
    val name: String? = null,
)

enum class AuditOutcome {
    SUCCESS,
    FAILURE,
    DENIED,
}
