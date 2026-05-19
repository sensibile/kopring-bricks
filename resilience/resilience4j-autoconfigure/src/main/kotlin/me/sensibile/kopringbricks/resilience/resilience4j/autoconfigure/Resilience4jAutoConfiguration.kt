package me.sensibile.kopringbricks.resilience.resilience4j.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "kopring.bricks.resilience4j",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(Resilience4jProperties::class)
class Resilience4jAutoConfiguration
