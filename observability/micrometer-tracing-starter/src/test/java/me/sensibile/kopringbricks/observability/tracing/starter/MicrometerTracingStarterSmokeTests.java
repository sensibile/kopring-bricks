package me.sensibile.kopringbricks.observability.tracing.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class MicrometerTracingStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.observability.tracing.autoconfigure.MicrometerTracingAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesBundledTracingDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("io.opentelemetry.api.OpenTelemetry"))
            .doesNotThrowAnyException();
    }
}
