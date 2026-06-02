package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("kopring.bricks.outbox")
data class OutboxProperties(
    val enabled: Boolean = true,
    val jdbc: Jdbc = Jdbc(),
    val polling: Polling = Polling(),
    val retry: Retry = Retry(),
    val scheduler: Scheduler = Scheduler(),
) {
    data class Jdbc(
        val tableName: String = "outbox_event",
        val dialect: OutboxJdbcDialect = OutboxJdbcDialect.AUTO,
        val flyway: Flyway = Flyway(),
    )

    data class Flyway(
        val enabled: Boolean = false,
    )

    data class Polling(
        val claimLimit: Int = DEFAULT_CLAIM_LIMIT,
        val claimTimeout: Duration = Duration.ofMinutes(DEFAULT_CLAIM_TIMEOUT_MINUTES),
    )

    data class Retry(
        val initialDelay: Duration = Duration.ofSeconds(DEFAULT_INITIAL_RETRY_DELAY_SECONDS),
        val maxDelay: Duration = Duration.ofMinutes(DEFAULT_MAX_RETRY_DELAY_MINUTES),
        val multiplier: Int = DEFAULT_RETRY_MULTIPLIER,
    )

    data class Scheduler(
        val enabled: Boolean = false,
        val initialDelay: Duration = Duration.ZERO,
        val fixedDelay: Duration = Duration.ofSeconds(DEFAULT_SCHEDULER_FIXED_DELAY_SECONDS),
        val poolSize: Int = DEFAULT_SCHEDULER_POOL_SIZE,
        val threadNamePrefix: String = "kopring-bricks-outbox-",
    )
}

private const val DEFAULT_CLAIM_LIMIT = 100
private const val DEFAULT_CLAIM_TIMEOUT_MINUTES = 5L
private const val DEFAULT_INITIAL_RETRY_DELAY_SECONDS = 5L
private const val DEFAULT_MAX_RETRY_DELAY_MINUTES = 5L
private const val DEFAULT_RETRY_MULTIPLIER = 2
private const val DEFAULT_SCHEDULER_FIXED_DELAY_SECONDS = 1L
private const val DEFAULT_SCHEDULER_POOL_SIZE = 1

enum class OutboxJdbcDialect {
    AUTO,
    POSTGRESQL,
}
