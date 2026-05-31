package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Clock
import javax.sql.DataSource

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "kopring.bricks.event-sourcing",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(EventSourcingProperties::class)
class EventSourcingAutoConfiguration {
    @Bean
    @ConditionalOnClass(DataSource::class, JdbcClient::class)
    @ConditionalOnBean(JdbcClient::class)
    @Conditional(EventSourcingJdbcCondition::class)
    @ConditionalOnMissingBean(EventStore::class)
    fun jdbcEventStore(
        jdbcClient: JdbcClient,
        properties: EventSourcingProperties,
        clock: ObjectProvider<Clock>,
    ): EventStore =
        JdbcEventStore(
            jdbcClient,
            properties.jdbc.tableName.requireSqlIdentifier("tableName"),
            clock.getIfAvailable { Clock.systemUTC() },
        )

    @Bean
    @ConditionalOnBean(EventStore::class)
    @ConditionalOnMissingBean
    fun eventSourcingTemplate(eventStore: EventStore): EventSourcingTemplate = EventSourcingTemplate(eventStore)
}

private fun String.requireSqlIdentifier(propertyName: String): String {
    require(SQL_IDENTIFIER.matches(this)) {
        "kopring.bricks.event-sourcing.jdbc.$propertyName must be a simple SQL identifier: $this"
    }

    return this
}

private val SQL_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
