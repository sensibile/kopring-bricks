package me.sensibile.kopringbricks.cache.caffeine.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class CaffeineCacheStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.cache.caffeine.autoconfigure.CaffeineCacheAutoConfiguration"))
            .doesNotThrowAnyException();
    }
}
