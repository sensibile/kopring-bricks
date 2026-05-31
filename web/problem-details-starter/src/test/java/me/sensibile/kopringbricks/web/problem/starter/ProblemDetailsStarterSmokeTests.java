package me.sensibile.kopringbricks.web.problem.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ProblemDetailsStarterSmokeTests {

    @Test
    void exposesAutoConfigurationOnClasspath() {
        assertThatCode(() -> Class.forName("me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailsAutoConfiguration"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesWebSupportDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.http.ProblemDetail"))
            .doesNotThrowAnyException();
    }
}
