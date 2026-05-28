package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import java.time.Duration
import java.time.Instant

class OutboxRetryPolicy(
    private val properties: OutboxProperties,
) {
    fun nextAttemptAt(
        event: OutboxEvent,
        now: Instant,
    ): Instant = now.plus(delay(event.retryCount + 1))

    private fun delay(attempt: Int): Duration {
        val multiplier = properties.retry.multiplier.coerceAtLeast(MIN_RETRY_MULTIPLIER)
        val exponent = (attempt - 1).coerceAtLeast(0)
        val factor = multiplier.pow(exponent)
        val delayMillis =
            properties.retry.initialDelay
                .toMillis()
                .saturatingMultiply(factor)

        return Duration.ofMillis(delayMillis).coerceAtMost(properties.retry.maxDelay)
    }

    private fun Int.pow(exponent: Int): Long {
        var result = 1L
        repeat(exponent) {
            result = result.saturatingMultiply(this.toLong())
        }

        return result
    }

    private fun Long.saturatingMultiply(multiplier: Long): Long =
        when {
            this == 0L || multiplier == 0L -> 0L
            this > Long.MAX_VALUE / multiplier -> Long.MAX_VALUE
            else -> this * multiplier
        }

    private companion object {
        private const val MIN_RETRY_MULTIPLIER = 1
    }
}
