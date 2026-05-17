package me.sensibile.kopringbricks.observability.tracing.autoconfigure

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.env.MockEnvironment

class MicrometerTracingEnvironmentPostProcessorTests {

    @Test
    fun `sets tracing defaults`() {
        val environment = MockEnvironment()
        val application = SpringApplication(Any::class.java)

        MicrometerTracingEnvironmentPostProcessor().postProcessEnvironment(environment, application)

        assertThat(environment.getProperty("management.tracing.enabled")).isEqualTo("true")
        assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("1.0")
        assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint")).isNull()
    }

    @Test
    fun `maps configured otlp endpoints`() {
        val environment = MockEnvironment()
            .withProperty(
                "kopring.bricks.micrometer-tracing.otlp.traces-endpoint",
                "http://localhost:4318/v1/traces",
            )
            .withProperty(
                "kopring.bricks.micrometer-tracing.otlp.metrics-endpoint",
                "http://localhost:4318/v1/metrics",
            )
        val application = SpringApplication(Any::class.java)

        MicrometerTracingEnvironmentPostProcessor().postProcessEnvironment(environment, application)

        assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
            .isEqualTo("http://localhost:4318/v1/traces")
        assertThat(environment.getProperty("management.otlp.metrics.export.url"))
            .isEqualTo("http://localhost:4318/v1/metrics")
    }

    @Test
    fun `does not override explicit management property`() {
        val environment = MockEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "test",
                mapOf("management.tracing.sampling.probability" to "0.25"),
            ),
        )
        val application = SpringApplication(Any::class.java)

        MicrometerTracingEnvironmentPostProcessor().postProcessEnvironment(environment, application)

        assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("0.25")
    }
}
