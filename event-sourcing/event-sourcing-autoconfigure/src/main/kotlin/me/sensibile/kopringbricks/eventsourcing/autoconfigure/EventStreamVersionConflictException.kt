package me.sensibile.kopringbricks.eventsourcing.autoconfigure

class EventStreamVersionConflictException(
    val streamId: String,
    val expectedVersion: Long,
    val actualVersion: Long,
    cause: Throwable? = null,
) : RuntimeException(
        "Event stream version conflict. streamId=$streamId, " +
            "expectedVersion=$expectedVersion, actualVersion=$actualVersion",
        cause,
    )
