package me.sensibile.kopringbricks.observability.logging.autoconfigure

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.env.MockEnvironment

class LoggingObservationEnvironmentPostProcessorTests {

    @Test
    fun `sets default structured console logging format`() {
        val environment = MockEnvironment()
        val application = SpringApplication(Any::class.java)

        LoggingObservationEnvironmentPostProcessor().postProcessEnvironment(environment, application)

        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("ecs")
    }

    @Test
    fun `does not override user structured console logging format`() {
        val environment = MockEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "test",
                mapOf("logging.structured.format.console" to "logstash"),
            ),
        )
        val application = SpringApplication(Any::class.java)

        LoggingObservationEnvironmentPostProcessor().postProcessEnvironment(environment, application)

        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("logstash")
    }
}
