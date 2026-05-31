package me.sensibile.kopringbricks.auditlog.autoconfigure

import me.sensibile.kopringbricks.support.jdbc.autoconfigure.requireSimpleSqlIdentifier
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
            properties.jdbc.tableName.requireSimpleSqlIdentifier("kopring.bricks.audit-log.jdbc.tableName"),
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
