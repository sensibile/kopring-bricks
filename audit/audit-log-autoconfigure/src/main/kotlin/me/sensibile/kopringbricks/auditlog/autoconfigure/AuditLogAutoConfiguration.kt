package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.jdbc.core.simple.JdbcClient
import javax.sql.DataSource

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "kopring.bricks.audit-log",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(AuditLogProperties::class)
class AuditLogAutoConfiguration {
    @Bean
    @ConditionalOnClass(DataSource::class, JdbcClient::class)
    @ConditionalOnBean(JdbcClient::class)
    @Conditional(AuditLogJdbcCondition::class)
    @ConditionalOnMissingBean(AuditEventRepository::class)
    fun jdbcAuditEventRepository(
        jdbcClient: JdbcClient,
        properties: AuditLogProperties,
    ): AuditEventRepository =
        JdbcAuditEventRepository(
            jdbcClient,
            properties.jdbc.tableName.requireSqlIdentifier("tableName"),
        )

    @Bean
    @ConditionalOnMissingBean(AuditEventRepository::class)
    fun loggingAuditEventRepository(): AuditEventRepository = LoggingAuditEventRepository()

    @Bean
    @ConditionalOnMissingBean(AuditEventPublisher::class)
    fun auditEventPublisher(
        repository: AuditEventRepository,
        properties: AuditLogProperties,
    ): AuditEventPublisher = DefaultAuditEventPublisher(repository, properties)
}

private fun String.requireSqlIdentifier(propertyName: String): String {
    require(SQL_IDENTIFIER.matches(this)) {
        "kopring.bricks.audit-log.jdbc.$propertyName must be a simple SQL identifier: $this"
    }

    return this
}

private val SQL_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
