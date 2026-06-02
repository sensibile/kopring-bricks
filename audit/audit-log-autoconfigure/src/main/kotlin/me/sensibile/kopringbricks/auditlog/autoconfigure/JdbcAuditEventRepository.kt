package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class JdbcAuditEventRepository(
    private val jdbcClient: JdbcClient,
    tableName: String,
) : AuditEventRepository {
    private val insertSql =
        """
        insert into $tableName (
            id,
            occurred_at,
            actor_type,
            actor_id,
            actor_name,
            action,
            target_type,
            target_id,
            target_name,
            outcome,
            trace_id,
            request_id,
            reason,
            metadata,
            before_state,
            after_state
        ) values (
            :id,
            :occurredAt,
            :actorType,
            :actorId,
            :actorName,
            :action,
            :targetType,
            :targetId,
            :targetName,
            :outcome,
            :traceId,
            :requestId,
            :reason,
            cast(:metadataJson as jsonb),
            cast(:beforeStateJson as jsonb),
            cast(:afterStateJson as jsonb)
        )
        """.trimIndent()

    override fun save(event: AuditEvent) {
        jdbcClient
            .sql(insertSql)
            .param("id", event.id)
            .param("occurredAt", event.occurredAt.toOffsetDateTime())
            .param("actorType", event.actor.type)
            .param("actorId", event.actor.id)
            .param("actorName", event.actor.name)
            .param("action", event.action)
            .param("targetType", event.target.type)
            .param("targetId", event.target.id)
            .param("targetName", event.target.name)
            .param("outcome", event.outcome.name)
            .param("traceId", event.traceId)
            .param("requestId", event.requestId)
            .param("reason", event.reason)
            .param("metadataJson", event.metadataJson)
            .param("beforeStateJson", event.beforeStateJson)
            .param("afterStateJson", event.afterStateJson)
            .update()
    }

    private fun Instant.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
}
