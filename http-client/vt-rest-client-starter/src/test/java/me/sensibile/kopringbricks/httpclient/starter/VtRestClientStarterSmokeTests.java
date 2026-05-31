package me.sensibile.kopringbricks.httpclient.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class VtRestClientStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.httpclient.autoconfigure.VtRestClientAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesBundledRestClientDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.web.client.RestClient"))
            .doesNotThrowAnyException();
    }
}
