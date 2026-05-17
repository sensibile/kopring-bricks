package me.sensibile.kopringbricks.observability.logging.autoconfigure

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.slf4j.MDC
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.task.TaskDecorator

class LoggingObservationAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(LoggingObservationAutoConfiguration::class.java)

    @Test
    fun `creates task decorator and rest client customizer`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(LoggingObservationProperties::class.java)
            assertThat(context).hasSingleBean(TaskDecorator::class.java)
            assertThat(context).hasBean("kopringBricksCorrelationIdRestClientCustomizer")
            assertThat(context.getBean("kopringBricksCorrelationIdRestClientCustomizer"))
                .isInstanceOf(RestClientCustomizer::class.java)
        }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.logging-observation.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(TaskDecorator::class.java)
                assertThat(context).doesNotHaveBean(RestClientCustomizer::class.java)
            }
    }

    @Test
    fun `task decorator propagates mdc context`() {
        contextRunner.run { context ->
            val decorator = context.getBean(TaskDecorator::class.java)

            MDC.put("request_id", "req-1")

            var decoratedValue: String? = null
            val decorated = decorator.decorate {
                decoratedValue = MDC.get("request_id")
            }

            MDC.clear()
            decorated.run()

            assertThat(decoratedValue).isEqualTo("req-1")
            assertThat(MDC.get("request_id")).isNull()
        }
    }
}
