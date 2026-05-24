package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.slf4j.LoggerFactory

class DefaultAuditEventPublisher(
    private val repository: AuditEventRepository,
    private val properties: AuditLogProperties,
) : AuditEventPublisher {
    override fun publish(event: AuditEvent) {
        runCatching {
            repository.save(event)
        }.onFailure { exception ->
            if (properties.publisher.failOnError) {
                throw exception
            }

            logger.warn(
                "Failed to publish audit event. action={}, targetType={}, targetId={}, auditEventId={}",
                event.action,
                event.target.type,
                event.target.id,
                event.id,
                exception,
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DefaultAuditEventPublisher::class.java)
    }
}
