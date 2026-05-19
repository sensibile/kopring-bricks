package me.sensibile.kopringbricks.resilience.resilience4j.autoconfigure

import java.time.Duration

import org.springframework.boot.context.properties.ConfigurationProperties

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
        val slidingWindowSize: Int = 100,
        val minimumNumberOfCalls: Int = 20,
        val failureRateThreshold: Float = 50.0f,
        val slowCallRateThreshold: Float = 50.0f,
        val slowCallDurationThreshold: Duration = Duration.ofSeconds(2),
        val waitDurationInOpenState: Duration = Duration.ofSeconds(30),
        val permittedNumberOfCallsInHalfOpenState: Int = 10,
        val automaticTransitionFromOpenToHalfOpenEnabled: Boolean = true,
    )

    data class Retry(
        val maxAttempts: Int = 3,
        val waitDuration: Duration = Duration.ofMillis(200),
        val enableExponentialBackoff: Boolean = true,
        val exponentialBackoffMultiplier: Double = 2.0,
    )

    data class TimeLimiter(
        val timeoutDuration: Duration = Duration.ofSeconds(2),
        val cancelRunningFuture: Boolean = true,
    )

    data class Bulkhead(
        val maxConcurrentCalls: Int = 25,
        val maxWaitDuration: Duration = Duration.ZERO,
    )

    data class RateLimiter(
        val limitForPeriod: Int = 100,
        val limitRefreshPeriod: Duration = Duration.ofSeconds(1),
        val timeoutDuration: Duration = Duration.ZERO,
    )

    data class Health(
        val circuitBreakersEnabled: Boolean = true,
        val rateLimitersEnabled: Boolean = true,
    )
}
