package me.sensibile.kopringbricks.web.concurrency.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders

@AutoConfiguration
@ConditionalOnClass(HttpHeaders::class)
@ConditionalOnProperty(
    prefix = "kopring.bricks.concurrency-control",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(ConcurrencyControlProperties::class)
class ConcurrencyControlAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun etagGenerator(properties: ConcurrencyControlProperties): ETagGenerator = DefaultETagGenerator(properties)

    @Bean
    @ConditionalOnMissingBean
    fun kopringBricksIfMatchValidator(
        etagGenerator: ETagGenerator,
        properties: ConcurrencyControlProperties,
    ): IfMatchValidator = IfMatchValidator(etagGenerator, properties)

    @Bean
    @ConditionalOnMissingBean
    fun kopringBricksIdempotencyKeyResolver(properties: ConcurrencyControlProperties): IdempotencyKeyResolver =
        IdempotencyKeyResolver(properties)
}
