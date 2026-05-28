package me.sensibile.kopringbricks.testsupport.audit

import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditEvent
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditEventPublisher
import java.util.concurrent.CopyOnWriteArrayList

class RecordingAuditEventPublisher : AuditEventPublisher {
    private val recordedEvents = CopyOnWriteArrayList<AuditEvent>()

    val events: List<AuditEvent>
        get() = recordedEvents.toList()

    override fun publish(event: AuditEvent) {
        recordedEvents += event
    }

    fun clear() {
        recordedEvents.clear()
    }
}
