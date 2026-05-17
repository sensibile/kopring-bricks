package me.sensibile.kopringbricks.observability.tracing.autoconfigure

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.task.TaskDecorator

class MicrometerTracingAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(MicrometerTracingAutoConfiguration::class.java)

    @Test
    fun `creates context propagating task decorator`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(MicrometerTracingProperties::class.java)
            assertThat(context).hasSingleBean(TaskDecorator::class.java)
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
