package me.sensibile.kopringbricks.cache.caffeine.autoconfigure

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class CaffeineCacheEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val enabled = environment.getProperty("$PREFIX.enabled", Boolean::class.java, true)
        if (!enabled || environment.containsProperty("spring.cache.type")) {
            return
        }

        environment.propertySources.addLast(
            MapPropertySource(
                "kopringBricksCaffeineCacheDefaults",
                mapOf("spring.cache.type" to "caffeine"),
            ),
        )
    }

    private companion object {
        private const val PREFIX = "kopring.bricks.caffeine-cache"
    }
}
