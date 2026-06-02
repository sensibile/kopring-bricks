package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class JdbcEventStore(
    private val jdbcClient: JdbcClient,
    tableName: String,
    private val clock: Clock = Clock.systemUTC(),
) : EventStore {
    private val currentVersionSql =
        """
        select coalesce(max(stream_version), 0)
        from $tableName
        where stream_id = :streamId
        """.trimIndent()

    private val insertSql =
        """
        insert into $tableName (
            id,
            stream_id,
            stream_version,
            event_type,
            event_version,
            payload,
            metadata,
            occurred_at,
            recorded_at
        ) values (
            :id,
            :streamId,
            :streamVersion,
            :eventType,
            :eventVersion,
            cast(:payloadJson as jsonb),
            cast(:metadataJson as jsonb),
            :occurredAt,
            :recordedAt
        )
        """.trimIndent()

    private val loadSql =
        """
        select *
        from $tableName
        where stream_id = :streamId
          and stream_version >= :fromVersion
        order by stream_version
        """.trimIndent()

    @Transactional
    override fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult {
        require(streamId.isNotBlank()) { "streamId must not be blank" }
        require(expectedVersion >= 0) { "expectedVersion must be greater than or equal to zero" }
        require(events.isNotEmpty()) { "events must not be empty" }

        val actualVersion = currentVersion(streamId)
        if (actualVersion != expectedVersion) {
            throw EventStreamVersionConflictException(streamId, expectedVersion, actualVersion)
        }

        val recordedAt = clock.instant()
        val storedEvents =
            events.mapIndexed { index, event ->
                event.toStoredEvent(
                    streamId = streamId,
                    streamVersion = expectedVersion + index + 1,
                )
            }

        try {
            storedEvents.forEach { event -> insert(event, recordedAt) }
        } catch (exception: DataIntegrityViolationException) {
            throw EventStreamVersionConflictException(
                streamId = streamId,
                expectedVersion = expectedVersion,
                actualVersion = null,
                cause = exception,
            )
        }

        return EventAppendResult(
            streamId = streamId,
            previousVersion = expectedVersion,
            currentVersion = storedEvents.last().streamVersion,
            events = storedEvents,
        )
    }

    override fun load(
        streamId: String,
        fromVersion: Long,
    ): List<StoredEvent> {
        require(streamId.isNotBlank()) { "streamId must not be blank" }
        require(fromVersion >= 1) { "fromVersion must be greater than or equal to one" }

        return jdbcClient
            .sql(loadSql)
            .param("streamId", streamId)
            .param("fromVersion", fromVersion)
            .query(::mapStoredEvent)
            .list()
    }

    private fun currentVersion(streamId: String): Long =
        jdbcClient
            .sql(currentVersionSql)
            .param("streamId", streamId)
            .query(Long::class.java)
            .single()

    private fun insert(
        event: StoredEvent,
        recordedAt: Instant,
    ) {
        jdbcClient
            .sql(insertSql)
            .param("id", event.id)
            .param("streamId", event.streamId)
            .param("streamVersion", event.streamVersion)
            .param("eventType", event.eventType)
            .param("eventVersion", event.eventVersion)
            .param("payloadJson", event.payloadJson)
            .param("metadataJson", event.metadataJson)
            .param("occurredAt", event.occurredAt.toOffsetDateTime())
            .param("recordedAt", recordedAt.toOffsetDateTime())
            .update()
    }

    private fun EventStoreEvent.toStoredEvent(
        streamId: String,
        streamVersion: Long,
    ): StoredEvent =
        StoredEvent(
            id = id,
            streamId = streamId,
            streamVersion = streamVersion,
            eventType = eventType,
            eventVersion = eventVersion,
            payloadJson = payloadJson,
            metadataJson = metadataJson,
            occurredAt = occurredAt,
        )

    private fun mapStoredEvent(
        resultSet: ResultSet,
        @Suppress("UNUSED_PARAMETER") rowNumber: Int,
    ): StoredEvent =
        StoredEvent(
            id = resultSet.getString("id"),
            streamId = resultSet.getString("stream_id"),
            streamVersion = resultSet.getLong("stream_version"),
            eventType = resultSet.getString("event_type"),
            eventVersion = resultSet.getInt("event_version"),
            payloadJson = resultSet.getString("payload"),
            metadataJson = resultSet.getString("metadata"),
            occurredAt = resultSet.getObject("occurred_at", OffsetDateTime::class.java).toInstant(),
        )

    private fun Instant.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
}
