package me.sensibile.kopringbricks.resilience.resilience4j.starter;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(
    classes = Resilience4jStarterSmokeTests.TestApplication.class,
    properties = {
        "spring.main.web-application-type=none",
        "kopring.bricks.resilience4j.retry.max-attempts=2"
    }
)
class Resilience4jStarterSmokeTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Test
    void startsWithResilience4jAutoConfigurations() {
        assertThat(context.getBeanProvider(CircuitBreakerRegistry.class).getIfAvailable()).isNotNull();
        assertThat(context.getBeanProvider(RetryRegistry.class).getIfAvailable()).isNotNull();
        assertThat(environment.getProperty("resilience4j.retry.configs.default.max-attempts", Integer.class))
            .isEqualTo(2);
    }

    @Test
    void exposesBundledIntegrationDependenciesOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.health.contributor.HealthIndicator"))
            .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("org.aspectj.lang.ProceedingJoinPoint"))
            .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics"))
            .doesNotThrowAnyException();
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
