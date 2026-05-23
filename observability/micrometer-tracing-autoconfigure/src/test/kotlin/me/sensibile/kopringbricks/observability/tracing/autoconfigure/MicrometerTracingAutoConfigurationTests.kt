package me.sensibile.kopringbricks.observability.tracing.autoconfigure

import kotlin.test.Test

import org.slf4j.MDC
import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.task.TaskDecorator
import org.springframework.core.task.support.CompositeTaskDecorator

class MicrometerTracingAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(MicrometerTracingAutoConfiguration::class.java)

    @Test
    fun `creates context propagating task decorator`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(MicrometerTracingProperties::class.java)
            assertThat(context).hasSingleBean(TaskDecorator::class.java)
            assertThat(context.getBean(TaskDecorator::class.java))
                .isInstanceOf(CompositeTaskDecorator::class.java)
        }
    }

    @Test
    fun `composite task decorator propagates mdc context`() {
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

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.micrometer-tracing.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(TaskDecorator::class.java)
            }
    }
}
