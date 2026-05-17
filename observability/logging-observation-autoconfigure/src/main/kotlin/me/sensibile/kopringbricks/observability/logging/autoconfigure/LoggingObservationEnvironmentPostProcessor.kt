package me.sensibile.kopringbricks.observability.logging.autoconfigure

import org.springframework.boot.SpringApplication
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class LoggingObservationEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val enabled = environment.getProperty("$PREFIX.enabled", Boolean::class.java, true)
        val jsonEnabled = environment.getProperty("$PREFIX.json.enabled", Boolean::class.java, true)

        if (!enabled || !jsonEnabled || environment.containsProperty(CONSOLE_FORMAT_PROPERTY)) {
            return
        }

        val consoleFormat = environment.getProperty("$PREFIX.json.console-format", "ecs")
        environment.propertySources.addLast(
            MapPropertySource(
                "kopringBricksLoggingObservationDefaults",
                mapOf(CONSOLE_FORMAT_PROPERTY to consoleFormat),
            ),
        )
    }

    private companion object {
        private const val PREFIX = "kopring.bricks.logging-observation"
        private const val CONSOLE_FORMAT_PROPERTY = "logging.structured.format.console"
    }
}
