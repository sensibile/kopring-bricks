package me.sensibile.kopringbricks.resilience.resilience4j.autoconfigure

import org.springframework.boot.SpringApplication
import org.springframework.mock.env.MockEnvironment
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Resilience4jEnvironmentPostProcessorTests {
    private val postProcessor = Resilience4jEnvironmentPostProcessor()

    @Test
    fun `adds resilience4j defaults`() {
        val environment = MockEnvironment()

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        val circuitBreakerPrefix = "resilience4j.circuitbreaker.configs.default"

        assertEquals(100, environment.getProperty("$circuitBreakerPrefix.sliding-window-size", Int::class.java))
        assertEquals(3, environment.getProperty("resilience4j.retry.configs.default.max-attempts", Int::class.java))
        assertEquals(
            Duration.ofSeconds(2),
            environment.getProperty("resilience4j.timelimiter.configs.default.timeout-duration", Duration::class.java),
        )
        assertEquals(true, environment.getProperty("management.health.circuitbreakers.enabled", Boolean::class.java))
        assertEquals(
            true,
            environment.getProperty("$circuitBreakerPrefix.register-health-indicator", Boolean::class.java),
        )
    }

    @Test
    fun `uses kopring property overrides for generated defaults`() {
        val environment =
            MockEnvironment()
                .withProperty("kopring.bricks.resilience4j.retry.max-attempts", "5")
                .withProperty("kopring.bricks.resilience4j.bulkhead.max-concurrent-calls", "50")

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertEquals(5, environment.getProperty("resilience4j.retry.configs.default.max-attempts", Int::class.java))
        assertEquals(
            50,
            environment.getProperty("resilience4j.bulkhead.configs.default.max-concurrent-calls", Int::class.java),
        )
    }

    @Test
    fun `does not override explicit resilience4j properties`() {
        val environment =
            MockEnvironment()
                .withProperty("resilience4j.retry.configs.default.max-attempts", "7")

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertEquals(7, environment.getProperty("resilience4j.retry.configs.default.max-attempts", Int::class.java))
    }

    @Test
    fun `does not add defaults when disabled`() {
        val environment =
            MockEnvironment()
                .withProperty("kopring.bricks.resilience4j.enabled", "false")

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertNull(environment.getProperty("resilience4j.retry.configs.default.max-attempts"))
    }
}
