package me.sensibile.kopringbricks.resilience.resilience4j.autoconfigure

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class Resilience4jEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val properties = Binder.get(environment)
            .bind(PREFIX, Resilience4jProperties::class.java)
            .orElseGet { Resilience4jProperties() }

        if (!properties.enabled) {
            return
        }

        val defaults = linkedMapOf<String, Any>()
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.sliding-window-type", properties.circuitBreaker.slidingWindowType)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.sliding-window-size", properties.circuitBreaker.slidingWindowSize)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls", properties.circuitBreaker.minimumNumberOfCalls)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.failure-rate-threshold", properties.circuitBreaker.failureRateThreshold)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.slow-call-rate-threshold", properties.circuitBreaker.slowCallRateThreshold)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.slow-call-duration-threshold", properties.circuitBreaker.slowCallDurationThreshold)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state", properties.circuitBreaker.waitDurationInOpenState)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state", properties.circuitBreaker.permittedNumberOfCallsInHalfOpenState)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.automatic-transition-from-open-to-half-open-enabled", properties.circuitBreaker.automaticTransitionFromOpenToHalfOpenEnabled)
        putIfMissing(defaults, environment, "resilience4j.retry.configs.default.max-attempts", properties.retry.maxAttempts)
        putIfMissing(defaults, environment, "resilience4j.retry.configs.default.wait-duration", properties.retry.waitDuration)
        putIfMissing(defaults, environment, "resilience4j.retry.configs.default.enable-exponential-backoff", properties.retry.enableExponentialBackoff)
        putIfMissing(defaults, environment, "resilience4j.retry.configs.default.exponential-backoff-multiplier", properties.retry.exponentialBackoffMultiplier)
        putIfMissing(defaults, environment, "resilience4j.timelimiter.configs.default.timeout-duration", properties.timeLimiter.timeoutDuration)
        putIfMissing(defaults, environment, "resilience4j.timelimiter.configs.default.cancel-running-future", properties.timeLimiter.cancelRunningFuture)
        putIfMissing(defaults, environment, "resilience4j.bulkhead.configs.default.max-concurrent-calls", properties.bulkhead.maxConcurrentCalls)
        putIfMissing(defaults, environment, "resilience4j.bulkhead.configs.default.max-wait-duration", properties.bulkhead.maxWaitDuration)
        putIfMissing(defaults, environment, "resilience4j.ratelimiter.configs.default.limit-for-period", properties.rateLimiter.limitForPeriod)
        putIfMissing(defaults, environment, "resilience4j.ratelimiter.configs.default.limit-refresh-period", properties.rateLimiter.limitRefreshPeriod)
        putIfMissing(defaults, environment, "resilience4j.ratelimiter.configs.default.timeout-duration", properties.rateLimiter.timeoutDuration)
        putIfMissing(defaults, environment, "management.health.circuitbreakers.enabled", properties.health.circuitBreakersEnabled)
        putIfMissing(defaults, environment, "management.health.ratelimiters.enabled", properties.health.rateLimitersEnabled)
        putIfMissing(defaults, environment, "resilience4j.circuitbreaker.configs.default.register-health-indicator", properties.health.circuitBreakersEnabled)
        putIfMissing(defaults, environment, "resilience4j.ratelimiter.configs.default.register-health-indicator", properties.health.rateLimitersEnabled)

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
    }
}
