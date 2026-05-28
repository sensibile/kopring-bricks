package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

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
    prefix = "kopring.bricks.outbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(OutboxProperties::class)
class OutboxAutoConfiguration {
    @Bean
    @ConditionalOnClass(DataSource::class, JdbcClient::class)
    @ConditionalOnBean(JdbcClient::class)
    @Conditional(OutboxJdbcCondition::class)
    @ConditionalOnMissingBean(OutboxEventRepository::class)
    fun jdbcOutboxEventRepository(
        jdbcClient: JdbcClient,
        properties: OutboxProperties,
    ): OutboxEventRepository =
        JdbcOutboxEventRepository(
            jdbcClient,
            properties.jdbc.tableName.requireSqlIdentifier("tableName"),
        )

    @Bean
    @ConditionalOnMissingBean(OutboxEventRepository::class)
    fun loggingOutboxEventRepository(): OutboxEventRepository = LoggingOutboxEventRepository()

    @Bean
    @ConditionalOnMissingBean
    fun outboxAppender(repository: OutboxEventRepository): OutboxEventAppender = OutboxEventAppender(repository)
}

private fun String.requireSqlIdentifier(propertyName: String): String {
    require(SQL_IDENTIFIER.matches(this)) {
        "kopring.bricks.outbox.jdbc.$propertyName must be a simple SQL identifier: $this"
    }

    return this
}

private val SQL_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
