package me.sensibile.kopringbricks.observability.tracing.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.micrometer-tracing")
class MicrometerTracingProperties {
    var enabled: Boolean = true
    var sampling: Sampling = Sampling()
    var otlp: Otlp = Otlp()
    var contextPropagation: ContextPropagation = ContextPropagation()

    class Sampling {
        var probability: Double = 1.0
    }

    class Otlp {
        var tracesEndpoint: String? = null
        var metricsEndpoint: String? = null
        var logsEndpoint: String? = null
    }

    class ContextPropagation {
        var taskDecoratorEnabled: Boolean = true
    }
}
