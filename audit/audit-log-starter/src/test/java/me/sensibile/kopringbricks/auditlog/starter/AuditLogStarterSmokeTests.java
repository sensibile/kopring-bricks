package me.sensibile.kopringbricks.auditlog.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class AuditLogStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.auditlog.autoconfigure.AuditLogAutoConfiguration"))
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
