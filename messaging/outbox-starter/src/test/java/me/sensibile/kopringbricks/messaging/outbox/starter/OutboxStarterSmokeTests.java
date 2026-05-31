package me.sensibile.kopringbricks.messaging.outbox.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class OutboxStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesJdbcSupportDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.jdbc.core.simple.JdbcClient"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesLoggingApiDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.slf4j.Logger"))
            .doesNotThrowAnyException();
    }
}
