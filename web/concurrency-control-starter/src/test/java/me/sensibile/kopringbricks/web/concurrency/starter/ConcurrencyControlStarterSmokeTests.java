package me.sensibile.kopringbricks.web.concurrency.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConcurrencyControlStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.web.concurrency.autoconfigure.ConcurrencyControlAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesWebSupportDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.web.context.request.NativeWebRequest"))
            .doesNotThrowAnyException();
    }
}
