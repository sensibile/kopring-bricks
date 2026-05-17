package me.sensibile.kopringbricks.web.problem.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.ProblemDetail

@AutoConfiguration
@ConditionalOnClass(ProblemDetail::class)
@ConditionalOnProperty(
    prefix = "kopring.bricks.problem-details",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(ProblemDetailsProperties::class)
class ProblemDetailsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun kopringBricksProblemDetailFactory(
        properties: ProblemDetailsProperties,
    ): ProblemDetailFactory =
        ProblemDetailFactory(properties)
}
