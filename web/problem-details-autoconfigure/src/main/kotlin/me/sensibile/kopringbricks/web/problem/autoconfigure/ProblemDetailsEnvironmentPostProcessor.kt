package me.sensibile.kopringbricks.web.problem.autoconfigure

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class ProblemDetailsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val enabled = environment.getProperty("$PREFIX.enabled", Boolean::class.java, true)
        if (!enabled || environment.containsProperty("spring.mvc.problemdetails.enabled")) {
            return
        }

        environment.propertySources.addLast(
            MapPropertySource(
                "kopringBricksProblemDetailsDefaults",
                mapOf("spring.mvc.problemdetails.enabled" to true),
            ),
        )
    }

    private companion object {
        private const val PREFIX = "kopring.bricks.problem-details"
    }
}
