package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("kopring.bricks.outbox")
data class OutboxProperties(
    val enabled: Boolean = true,
    val jdbc: Jdbc = Jdbc(),
    val polling: Polling = Polling(),
) {
    data class Jdbc(
        val tableName: String = "outbox_event",
        val dialect: OutboxJdbcDialect = OutboxJdbcDialect.AUTO,
    )

    data class Polling(
        val claimLimit: Int = DEFAULT_CLAIM_LIMIT,
        val claimTimeout: Duration = Duration.ofMinutes(DEFAULT_CLAIM_TIMEOUT_MINUTES),
    )
}

private const val DEFAULT_CLAIM_LIMIT = 100
private const val DEFAULT_CLAIM_TIMEOUT_MINUTES = 5L

enum class OutboxJdbcDialect {
    AUTO,
    POSTGRESQL,
}
