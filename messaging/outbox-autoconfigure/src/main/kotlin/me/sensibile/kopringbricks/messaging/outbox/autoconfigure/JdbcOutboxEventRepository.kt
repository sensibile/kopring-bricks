package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.springframework.jdbc.core.simple.JdbcClient
import java.sql.ResultSet
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class JdbcOutboxEventRepository(
    private val jdbcClient: JdbcClient,
    tableName: String,
) : OutboxEventRepository {
    private val insertSql =
        """
        insert into $tableName (
            id,
            aggregate_type,
            aggregate_id,
            event_type,
            event_version,
            payload,
            headers,
            status,
            created_at,
            available_at,
            next_attempt_at,
            claimed_at,
            published_at,
            retry_count,
            last_error
        ) values (
            :id,
            :aggregateType,
            :aggregateId,
            :eventType,
            :eventVersion,
            cast(:payloadJson as jsonb),
            cast(:headersJson as jsonb),
            :status,
            :createdAt,
            :availableAt,
            :nextAttemptAt,
            :claimedAt,
            :publishedAt,
            :retryCount,
            :lastError
        )
        """.trimIndent()

    private val claimSql =
        """
        with candidate as (
            select id
            from $tableName
            where (
                status in (:pendingStatus, :failedStatus)
                and available_at <= :now
                and next_attempt_at <= :now
            ) or (
                status = :claimedStatus
                and claimed_at <= :claimExpiredBefore
            )
            order by created_at, id
            for update skip locked
            limit :limit
        ), updated as (
            update $tableName
            set status = :claimedStatus,
                claimed_at = :now
            from candidate
            where $tableName.id = candidate.id
            returning $tableName.*
        )
        select *
        from updated
        order by created_at, id
        """.trimIndent()

    private val markPublishedSql =
        """
        update $tableName
        set status = :status,
            published_at = :publishedAt,
            last_error = null
        where id = :id
        """.trimIndent()

    private val markFailedSql =
        """
        update $tableName
        set status = :status,
            retry_count = retry_count + 1,
            next_attempt_at = :nextAttemptAt,
            last_error = :lastError,
            claimed_at = null
        where id = :id
        """.trimIndent()

    override fun append(event: OutboxEvent): OutboxEvent {
        jdbcClient
            .sql(insertSql)
            .param("id", event.id)
            .param("aggregateType", event.aggregateType)
            .param("aggregateId", event.aggregateId)
            .param("eventType", event.eventType)
            .param("eventVersion", event.eventVersion)
            .param("payloadJson", event.payloadJson)
            .param("headersJson", event.headersJson)
            .param("status", event.status.name)
            .param("createdAt", event.createdAt.toOffsetDateTime())
            .param("availableAt", event.availableAt.toOffsetDateTime())
            .param("nextAttemptAt", event.nextAttemptAt.toOffsetDateTime())
            .param("claimedAt", event.claimedAt?.toOffsetDateTime())
            .param("publishedAt", event.publishedAt?.toOffsetDateTime())
            .param("retryCount", event.retryCount)
            .param("lastError", event.lastError)
            .update()

        return event
    }

    override fun claimPending(
        limit: Int,
        now: Instant,
        claimTimeout: Duration,
    ): List<OutboxEvent> {
        require(limit > 0) { "claim limit must be greater than zero" }

        return jdbcClient
            .sql(claimSql)
            .param("claimedStatus", OutboxEventStatus.CLAIMED.name)
            .param("pendingStatus", OutboxEventStatus.PENDING.name)
            .param("failedStatus", OutboxEventStatus.FAILED.name)
            .param("now", now.toOffsetDateTime())
            .param("claimExpiredBefore", now.minus(claimTimeout).toOffsetDateTime())
            .param("limit", limit)
            .query(::mapOutboxEvent)
            .list()
    }

    override fun markPublished(
        eventId: String,
        publishedAt: Instant,
    ) {
        jdbcClient
            .sql(markPublishedSql)
            .param("id", eventId)
            .param("status", OutboxEventStatus.PUBLISHED.name)
            .param("publishedAt", publishedAt.toOffsetDateTime())
            .update()
    }

    override fun markFailed(
        eventId: String,
        error: String,
        nextAttemptAt: Instant,
    ) {
        jdbcClient
            .sql(markFailedSql)
            .param("id", eventId)
            .param("status", OutboxEventStatus.FAILED.name)
            .param("nextAttemptAt", nextAttemptAt.toOffsetDateTime())
            .param("lastError", error)
            .update()
    }

    private fun mapOutboxEvent(
        resultSet: ResultSet,
        @Suppress("UNUSED_PARAMETER") rowNumber: Int,
    ): OutboxEvent =
        OutboxEvent(
            id = resultSet.getString("id"),
            aggregateType = resultSet.getString("aggregate_type"),
            aggregateId = resultSet.getString("aggregate_id"),
            eventType = resultSet.getString("event_type"),
            eventVersion = resultSet.getInt("event_version"),
            payloadJson = resultSet.getString("payload"),
            headersJson = resultSet.getString("headers"),
            status = OutboxEventStatus.valueOf(resultSet.getString("status")),
            createdAt = resultSet.getObject("created_at", OffsetDateTime::class.java).toInstant(),
            availableAt = resultSet.getObject("available_at", OffsetDateTime::class.java).toInstant(),
            nextAttemptAt = resultSet.getObject("next_attempt_at", OffsetDateTime::class.java).toInstant(),
            claimedAt = resultSet.getObject("claimed_at", OffsetDateTime::class.java)?.toInstant(),
            publishedAt = resultSet.getObject("published_at", OffsetDateTime::class.java)?.toInstant(),
            retryCount = resultSet.getInt("retry_count"),
            lastError = resultSet.getString("last_error"),
        )

    private fun Instant.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
}
