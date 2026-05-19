package me.sensibile.kopringbricks.resilience.resilience4j.autoconfigure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class Resilience4jAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(Resilience4jAutoConfiguration::class.java))

    @Test
    fun `binds default properties`() {
        contextRunner.run { context ->
            val properties = context.getBean(Resilience4jProperties::class.java)

            assertEquals(3, properties.retry.maxAttempts)
            assertEquals(25, properties.bulkhead.maxConcurrentCalls)
            assertNotNull(properties.circuitBreaker.slowCallDurationThreshold)
        }
    }

    @Test
    fun `backs off when disabled`() {
        contextRunner
            .withPropertyValues("kopring.bricks.resilience4j.enabled=false")
            .run { context ->
                assertTrue(context.getBeansOfType(Resilience4jProperties::class.java).isEmpty())
            }
    }
}
