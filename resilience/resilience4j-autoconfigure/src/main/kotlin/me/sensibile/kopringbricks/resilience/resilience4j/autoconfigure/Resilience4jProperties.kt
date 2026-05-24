package me.sensibile.kopringbricks.resilience.resilience4j.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_SLIDING_WINDOW_SIZE = 100
private const val DEFAULT_MINIMUM_NUMBER_OF_CALLS = 20
private const val DEFAULT_FAILURE_RATE_THRESHOLD = 50.0f
private const val DEFAULT_SLOW_CALL_RATE_THRESHOLD = 50.0f
private const val DEFAULT_SLOW_CALL_DURATION_SECONDS = 2L
private const val DEFAULT_WAIT_DURATION_IN_OPEN_STATE_SECONDS = 30L
private const val DEFAULT_HALF_OPEN_CALLS = 10
private const val DEFAULT_RETRY_ATTEMPTS = 3
private const val DEFAULT_RETRY_WAIT_MILLIS = 200L
private const val DEFAULT_EXPONENTIAL_BACKOFF_MULTIPLIER = 2.0
private const val DEFAULT_TIMEOUT_SECONDS = 2L
private const val DEFAULT_MAX_CONCURRENT_CALLS = 25
private const val DEFAULT_RATE_LIMIT_FOR_PERIOD = 100
private const val DEFAULT_RATE_LIMIT_REFRESH_SECONDS = 1L

@ConfigurationProperties("kopring.bricks.resilience4j")
data class Resilience4jProperties(
    val enabled: Boolean = true,
    val circuitBreaker: CircuitBreaker = CircuitBreaker(),
    val retry: Retry = Retry(),
    val timeLimiter: TimeLimiter = TimeLimiter(),
    val bulkhead: Bulkhead = Bulkhead(),
    val rateLimiter: RateLimiter = RateLimiter(),
    val health: Health = Health(),
) {
    data class CircuitBreaker(
        val slidingWindowType: String = "COUNT_BASED",
        val slidingWindowSize: Int = DEFAULT_SLIDING_WINDOW_SIZE,
        val minimumNumberOfCalls: Int = DEFAULT_MINIMUM_NUMBER_OF_CALLS,
        val failureRateThreshold: Float = DEFAULT_FAILURE_RATE_THRESHOLD,
        val slowCallRateThreshold: Float = DEFAULT_SLOW_CALL_RATE_THRESHOLD,
        val slowCallDurationThreshold: Duration = Duration.ofSeconds(DEFAULT_SLOW_CALL_DURATION_SECONDS),
        val waitDurationInOpenState: Duration = Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE_SECONDS),
        val permittedNumberOfCallsInHalfOpenState: Int = DEFAULT_HALF_OPEN_CALLS,
        val automaticTransitionFromOpenToHalfOpenEnabled: Boolean = true,
    )

    data class Retry(
        val maxAttempts: Int = DEFAULT_RETRY_ATTEMPTS,
        val waitDuration: Duration = Duration.ofMillis(DEFAULT_RETRY_WAIT_MILLIS),
        val enableExponentialBackoff: Boolean = true,
        val exponentialBackoffMultiplier: Double = DEFAULT_EXPONENTIAL_BACKOFF_MULTIPLIER,
    )

    data class TimeLimiter(
        val timeoutDuration: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS),
        val cancelRunningFuture: Boolean = true,
    )

    data class Bulkhead(
        val maxConcurrentCalls: Int = DEFAULT_MAX_CONCURRENT_CALLS,
        val maxWaitDuration: Duration = Duration.ZERO,
    )

    data class RateLimiter(
        val limitForPeriod: Int = DEFAULT_RATE_LIMIT_FOR_PERIOD,
        val limitRefreshPeriod: Duration = Duration.ofSeconds(DEFAULT_RATE_LIMIT_REFRESH_SECONDS),
        val timeoutDuration: Duration = Duration.ZERO,
    )

    data class Health(
        val circuitBreakersEnabled: Boolean = true,
        val rateLimitersEnabled: Boolean = true,
    )
}
