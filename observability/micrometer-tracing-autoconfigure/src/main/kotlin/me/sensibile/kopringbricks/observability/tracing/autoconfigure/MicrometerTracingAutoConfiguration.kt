package me.sensibile.kopringbricks.observability.tracing.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.task.TaskDecorator
import org.springframework.core.task.support.ContextPropagatingTaskDecorator

@AutoConfiguration(
    beforeName = ["me.sensibile.kopringbricks.observability.logging.autoconfigure.LoggingObservationAutoConfiguration"],
)
@ConditionalOnProperty(
    prefix = "kopring.bricks.micrometer-tracing",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(MicrometerTracingProperties::class)
class MicrometerTracingAutoConfiguration {

    @Bean
    @ConditionalOnClass(ContextPropagatingTaskDecorator::class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "kopring.bricks.micrometer-tracing.context-propagation",
        name = ["task-decorator-enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun kopringBricksContextPropagatingTaskDecorator(): TaskDecorator =
        ContextPropagatingTaskDecorator()
}
