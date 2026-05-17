package me.sensibile.kopringbricks.observability.tracing.autoconfigure

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class MicrometerTracingEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val enabled = environment.getProperty("$PREFIX.enabled", Boolean::class.java, true)
        if (!enabled) {
            return
        }

        val defaults = linkedMapOf<String, Any>()
        putIfMissing(environment, defaults, "management.tracing.enabled", true)
        putIfMissing(
            environment,
            defaults,
            "management.tracing.sampling.probability",
            environment.getProperty("$PREFIX.sampling.probability", Double::class.java, 1.0),
        )
        putEndpointIfConfigured(
            environment,
            defaults,
            "$PREFIX.otlp.traces-endpoint",
            "management.opentelemetry.tracing.export.otlp.endpoint",
        )
        putEndpointIfConfigured(
            environment,
            defaults,
            "$PREFIX.otlp.metrics-endpoint",
            "management.otlp.metrics.export.url",
        )
        putEndpointIfConfigured(
            environment,
            defaults,
            "$PREFIX.otlp.logs-endpoint",
            "management.opentelemetry.logging.export.otlp.endpoint",
        )

        if (defaults.isNotEmpty()) {
            environment.propertySources.addLast(
                MapPropertySource("kopringBricksMicrometerTracingDefaults", defaults),
            )
        }
    }

    private fun putIfMissing(
        environment: ConfigurableEnvironment,
        defaults: MutableMap<String, Any>,
        targetProperty: String,
        value: Any,
    ) {
        if (!environment.containsProperty(targetProperty)) {
            defaults[targetProperty] = value
        }
    }

    private fun putEndpointIfConfigured(
        environment: ConfigurableEnvironment,
        defaults: MutableMap<String, Any>,
        sourceProperty: String,
        targetProperty: String,
    ) {
        val endpoint = environment.getProperty(sourceProperty)
        if (!endpoint.isNullOrBlank() && !environment.containsProperty(targetProperty)) {
            defaults[targetProperty] = endpoint
        }
    }

    private companion object {
        private const val PREFIX = "kopring.bricks.micrometer-tracing"
    }
}
