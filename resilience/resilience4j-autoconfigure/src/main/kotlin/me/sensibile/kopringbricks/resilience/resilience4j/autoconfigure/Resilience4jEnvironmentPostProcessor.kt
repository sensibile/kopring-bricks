package me.sensibile.kopringbricks.resilience.resilience4j.autoconfigure

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class Resilience4jEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val properties =
            Binder
                .get(environment)
                .bind(PREFIX, Resilience4jProperties::class.java)
                .orElseGet { Resilience4jProperties() }

        if (!properties.enabled) {
            return
        }

        val defaults = linkedMapOf<String, Any>()
        defaultProperties(properties).forEach { (propertyName, value) ->
            putIfMissing(defaults, environment, propertyName, value)
        }

        if (defaults.isNotEmpty()) {
            environment.propertySources.addLast(
                MapPropertySource("kopringBricksResilience4jDefaults", defaults),
            )
        }
    }

    private fun putIfMissing(
        defaults: MutableMap<String, Any>,
        environment: ConfigurableEnvironment,
        propertyName: String,
        value: Any,
    ) {
        if (!environment.containsProperty(propertyName)) {
            defaults[propertyName] = value
        }
    }

    private companion object {
        private const val PREFIX = "kopring.bricks.resilience4j"
        private const val CIRCUIT_BREAKER = "resilience4j.circuitbreaker.configs.default"
        private const val RETRY = "resilience4j.retry.configs.default"
        private const val TIME_LIMITER = "resilience4j.timelimiter.configs.default"
        private const val BULKHEAD = "resilience4j.bulkhead.configs.default"
        private const val RATE_LIMITER = "resilience4j.ratelimiter.configs.default"
        private const val MANAGEMENT_HEALTH = "management.health"

        private fun defaultProperties(properties: Resilience4jProperties): List<Pair<String, Any>> =
            listOf(
                "$CIRCUIT_BREAKER.sliding-window-type" to properties.circuitBreaker.slidingWindowType,
                "$CIRCUIT_BREAKER.sliding-window-size" to properties.circuitBreaker.slidingWindowSize,
                "$CIRCUIT_BREAKER.minimum-number-of-calls" to properties.circuitBreaker.minimumNumberOfCalls,
                "$CIRCUIT_BREAKER.failure-rate-threshold" to properties.circuitBreaker.failureRateThreshold,
                "$CIRCUIT_BREAKER.slow-call-rate-threshold" to properties.circuitBreaker.slowCallRateThreshold,
                "$CIRCUIT_BREAKER.slow-call-duration-threshold" to
                    properties.circuitBreaker.slowCallDurationThreshold,
                "$CIRCUIT_BREAKER.wait-duration-in-open-state" to
                    properties.circuitBreaker.waitDurationInOpenState,
                "$CIRCUIT_BREAKER.permitted-number-of-calls-in-half-open-state" to
                    properties.circuitBreaker.permittedNumberOfCallsInHalfOpenState,
                "$CIRCUIT_BREAKER.automatic-transition-from-open-to-half-open-enabled" to
                    properties.circuitBreaker.automaticTransitionFromOpenToHalfOpenEnabled,
                "$RETRY.max-attempts" to properties.retry.maxAttempts,
                "$RETRY.wait-duration" to properties.retry.waitDuration,
                "$RETRY.enable-exponential-backoff" to properties.retry.enableExponentialBackoff,
                "$RETRY.exponential-backoff-multiplier" to properties.retry.exponentialBackoffMultiplier,
                "$TIME_LIMITER.timeout-duration" to properties.timeLimiter.timeoutDuration,
                "$TIME_LIMITER.cancel-running-future" to properties.timeLimiter.cancelRunningFuture,
                "$BULKHEAD.max-concurrent-calls" to properties.bulkhead.maxConcurrentCalls,
                "$BULKHEAD.max-wait-duration" to properties.bulkhead.maxWaitDuration,
                "$RATE_LIMITER.limit-for-period" to properties.rateLimiter.limitForPeriod,
                "$RATE_LIMITER.limit-refresh-period" to properties.rateLimiter.limitRefreshPeriod,
                "$RATE_LIMITER.timeout-duration" to properties.rateLimiter.timeoutDuration,
                "$MANAGEMENT_HEALTH.circuitbreakers.enabled" to properties.health.circuitBreakersEnabled,
                "$MANAGEMENT_HEALTH.ratelimiters.enabled" to properties.health.rateLimitersEnabled,
                "$CIRCUIT_BREAKER.register-health-indicator" to properties.health.circuitBreakersEnabled,
                "$RATE_LIMITER.register-health-indicator" to properties.health.rateLimitersEnabled,
            )
    }
}
