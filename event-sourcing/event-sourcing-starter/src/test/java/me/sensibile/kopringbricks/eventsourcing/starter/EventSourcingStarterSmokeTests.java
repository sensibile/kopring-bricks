package me.sensibile.kopringbricks.eventsourcing.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class EventSourcingStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventSourcingAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesProjectionApiOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventProjection"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesJdbcSupportDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.jdbc.core.simple.JdbcClient"))
            .doesNotThrowAnyException();
    }
}
