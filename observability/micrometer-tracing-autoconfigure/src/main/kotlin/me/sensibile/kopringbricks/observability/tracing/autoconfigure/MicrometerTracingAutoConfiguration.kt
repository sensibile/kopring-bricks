package me.sensibile.kopringbricks.observability.tracing.autoconfigure

import me.sensibile.kopringbricks.observability.logging.autoconfigure.MdcTaskDecorator

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.task.TaskDecorator
import org.springframework.core.task.support.CompositeTaskDecorator
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

    @AutoConfiguration
    @ConditionalOnClass(ContextPropagatingTaskDecorator::class)
    @ConditionalOnMissingClass("me.sensibile.kopringbricks.observability.logging.autoconfigure.MdcTaskDecorator")
    class ContextPropagationConfiguration {

        @Bean
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

    @AutoConfiguration
    @ConditionalOnClass(
        ContextPropagatingTaskDecorator::class,
        MdcTaskDecorator::class,
        CompositeTaskDecorator::class,
    )
    class CompositeContextPropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(
            prefix = "kopring.bricks.micrometer-tracing.context-propagation",
            name = ["task-decorator-enabled"],
            havingValue = "true",
            matchIfMissing = true,
        )
        fun kopringBricksCompositeTaskDecorator(): TaskDecorator =
            CompositeTaskDecorator(
                listOf(
                    ContextPropagatingTaskDecorator(),
                    MdcTaskDecorator(),
                ),
            )
    }
}
