package me.sensibile.kopringbricks.jdbcclient.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class VtJdbcClientStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.jdbcclient.autoconfigure.VtJdbcClientAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesBundledJdbcClientDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.jdbc.core.simple.JdbcClient"))
            .doesNotThrowAnyException();
    }
}
