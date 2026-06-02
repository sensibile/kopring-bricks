package me.sensibile.kopringbricks.eventsourcing.autoconfigure

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
    prefix = "kopring.bricks.event-sourcing",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(EventSourcingProperties::class)
@Conditional(EventSourcingJdbcCondition::class)
class EventSourcingFlywayAutoConfiguration {
    @Bean
    @ConditionalOnProperty(
        prefix = "kopring.bricks.event-sourcing.jdbc.flyway",
        name = ["enabled"],
        havingValue = "true",
    )
    fun eventSourcingFlywayConfigurationCustomizer(properties: EventSourcingProperties): FlywayConfigurationCustomizer {
        val defaultTableName = EventSourcingProperties.Jdbc().tableName
        require(properties.jdbc.tableName == defaultTableName) {
            "kopring.bricks.event-sourcing.jdbc.flyway.enabled requires the default " +
                "kopring.bricks.event-sourcing.jdbc.table-name value '$defaultTableName'."
        }

        return FlywayConfigurationCustomizer { configuration ->
            configuration.appendFlywayLocation(EVENT_SOURCING_POSTGRESQL_FLYWAY_LOCATION)
        }
    }
}
