package me.sensibile.kopringbricks.auditlog.autoconfigure

interface AuditEventPublisher {
    fun publish(event: AuditEvent)
}
