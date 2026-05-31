package me.sensibile.kopringbricks.web.concurrency.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConcurrencyControlStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.web.concurrency.autoconfigure.ConcurrencyControlAutoConfiguration"))
            .doesNotThrowAnyException();
    }
}
