package me.sensibile.kopringbricks.auditlog.autoconfigure

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
    prefix = "kopring.bricks.audit-log",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(AuditLogProperties::class)
@Conditional(AuditLogJdbcCondition::class)
class AuditLogFlywayAutoConfiguration {
    @Bean
    @ConditionalOnProperty(
        prefix = "kopring.bricks.audit-log.jdbc.flyway",
        name = ["enabled"],
        havingValue = "true",
    )
    fun auditLogFlywayConfigurationCustomizer(properties: AuditLogProperties): FlywayConfigurationCustomizer {
        val defaultTableName = AuditLogProperties.Jdbc().tableName
        require(properties.jdbc.tableName == defaultTableName) {
            "kopring.bricks.audit-log.jdbc.flyway.enabled requires the default " +
                "kopring.bricks.audit-log.jdbc.table-name value '$defaultTableName'."
        }

        return FlywayConfigurationCustomizer { configuration ->
            configuration.appendFlywayLocation(AUDIT_LOG_POSTGRESQL_FLYWAY_LOCATION)
        }
    }
}
