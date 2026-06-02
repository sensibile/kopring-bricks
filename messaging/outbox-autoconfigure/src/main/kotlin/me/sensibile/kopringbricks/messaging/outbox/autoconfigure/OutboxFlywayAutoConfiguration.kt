package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import me.sensibile.kopringbricks.support.jdbc.autoconfigure.appendFlywayLocation
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional

@AutoConfiguration
@ConditionalOnClass(
    name = [
        "org.flywaydb.core.api.configuration.FluentConfiguration",
        "org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer",
    ],
)
@ConditionalOnProperty(
    prefix = "kopring.bricks.outbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(OutboxProperties::class)
@Conditional(OutboxJdbcCondition::class)
class OutboxFlywayAutoConfiguration {
    @Bean
    @ConditionalOnProperty(
        prefix = "kopring.bricks.outbox.jdbc.flyway",
        name = ["enabled"],
        havingValue = "true",
    )
    fun outboxFlywayConfigurationCustomizer(properties: OutboxProperties): FlywayConfigurationCustomizer {
        val defaultTableName = OutboxProperties.Jdbc().tableName
        require(properties.jdbc.tableName == defaultTableName) {
            "kopring.bricks.outbox.jdbc.flyway.enabled requires the default " +
                "kopring.bricks.outbox.jdbc.table-name value '$defaultTableName'."
        }

        return FlywayConfigurationCustomizer { configuration ->
            configuration.appendFlywayLocation(OUTBOX_POSTGRESQL_FLYWAY_LOCATION)
        }
    }
}
