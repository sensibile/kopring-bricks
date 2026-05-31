package me.sensibile.kopringbricks.messaging.outbox.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class OutboxStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxAutoConfiguration"))
            .doesNotThrowAnyException();
    }
}
