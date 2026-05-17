package me.sensibile.kopringbricks.observability.logging.autoconfigure

import jakarta.servlet.Filter

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.core.task.TaskDecorator
import org.springframework.web.client.RestClient
import org.springframework.web.filter.OncePerRequestFilter

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "kopring.bricks.logging-observation",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(LoggingObservationProperties::class)
class LoggingObservationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "kopring.bricks.logging-observation.task-decorator",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun kopringBricksMdcTaskDecorator(): TaskDecorator =
        MdcTaskDecorator()

    @AutoConfiguration
    @ConditionalOnClass(Filter::class, OncePerRequestFilter::class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    class ServletConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = ["kopringBricksCorrelationIdFilter"])
        @ConditionalOnProperty(
            prefix = "kopring.bricks.logging-observation.correlation",
            name = ["enabled"],
            havingValue = "true",
            matchIfMissing = true,
        )
        fun kopringBricksCorrelationIdFilter(
            properties: LoggingObservationProperties,
        ): CorrelationIdFilter =
            CorrelationIdFilter(properties)
    }

    @AutoConfiguration
    @ConditionalOnClass(RestClient::class, RestClientCustomizer::class)
    class RestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = ["kopringBricksCorrelationIdRestClientCustomizer"])
        @ConditionalOnProperty(
            prefix = "kopring.bricks.logging-observation.rest-client",
            name = ["propagation-enabled"],
            havingValue = "true",
            matchIfMissing = true,
        )
        fun kopringBricksCorrelationIdRestClientCustomizer(
            properties: LoggingObservationProperties,
        ): RestClientCustomizer =
            CorrelationIdRestClientCustomizer(properties)
    }
}
