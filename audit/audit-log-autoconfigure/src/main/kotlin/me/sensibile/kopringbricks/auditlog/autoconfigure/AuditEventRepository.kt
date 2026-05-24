package me.sensibile.kopringbricks.auditlog.autoconfigure

interface AuditEventRepository {
    fun save(event: AuditEvent)
}
