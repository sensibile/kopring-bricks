package me.sensibile.kopringbricks.web.error.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class WebMvcErrorStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.web.error.autoconfigure.WebMvcErrorAutoConfiguration"))
            .doesNotThrowAnyException();
    }
}
