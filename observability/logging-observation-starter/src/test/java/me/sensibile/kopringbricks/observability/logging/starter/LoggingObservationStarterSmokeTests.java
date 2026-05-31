package me.sensibile.kopringbricks.observability.logging.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingObservationStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.observability.logging.autoconfigure.LoggingObservationAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesBundledLoggingDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("ch.qos.logback.classic.Logger"))
            .doesNotThrowAnyException();
    }
}
