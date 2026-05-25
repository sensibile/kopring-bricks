package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.slf4j.LoggerFactory

class LoggingAuditEventRepository : AuditEventRepository {
    override fun save(event: AuditEvent) {
        logger.info(
            "Audit event. id={}, actorType={}, actorId={}, action={}, targetType={}, targetId={}, outcome={}",
            event.id,
            event.actor.type,
            event.actor.id,
            event.action,
            event.target.type,
            event.target.id,
            event.outcome,
        )
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingAuditEventRepository::class.java)
    }
}
