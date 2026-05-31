package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.event-sourcing")
data class EventSourcingProperties(
    val enabled: Boolean = true,
    val jdbc: Jdbc = Jdbc(),
) {
    data class Jdbc(
        val tableName: String = "event_store",
        val dialect: EventSourcingJdbcDialect = EventSourcingJdbcDialect.AUTO,
    )
}

enum class EventSourcingJdbcDialect {
    AUTO,
    POSTGRESQL,
}
