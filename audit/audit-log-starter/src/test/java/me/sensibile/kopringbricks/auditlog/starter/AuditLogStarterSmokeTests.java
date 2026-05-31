package me.sensibile.kopringbricks.auditlog.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class AuditLogStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.auditlog.autoconfigure.AuditLogAutoConfiguration"))
            .doesNotThrowAnyException();
    }
}
