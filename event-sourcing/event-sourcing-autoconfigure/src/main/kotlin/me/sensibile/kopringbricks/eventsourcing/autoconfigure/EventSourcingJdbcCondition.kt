package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionMessage
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.boot.jdbc.DatabaseDriver
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotatedTypeMetadata

class EventSourcingJdbcCondition : SpringBootCondition() {
    override fun getMatchOutcome(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): ConditionOutcome =
        when (context.environment.eventSourcingJdbcDialect()) {
            EventSourcingJdbcDialect.POSTGRESQL -> postgresqlDialectMatch()
            EventSourcingJdbcDialect.AUTO -> context.environment.postgresqlUrlOutcome()
        }

    private fun Environment.eventSourcingJdbcDialect(): EventSourcingJdbcDialect =
        getProperty(JDBC_DIALECT_PROPERTY)
            ?.let { EventSourcingJdbcDialect.valueOf(it.uppercase().replace("-", "_")) }
            ?: EventSourcingJdbcDialect.AUTO

    private fun postgresqlDialectMatch(): ConditionOutcome =
        ConditionOutcome.match(
            ConditionMessage
                .forCondition("EventSourcingJdbc")
                .because("$JDBC_DIALECT_PROPERTY is postgresql"),
        )

    private fun Environment.postgresqlUrlOutcome(): ConditionOutcome {
        val jdbcUrl = DATA_SOURCE_URL_PROPERTIES.firstNotNullOfOrNull { getProperty(it) }
        if (jdbcUrl == null) {
            return ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition("EventSourcingJdbc")
                    .because("no datasource JDBC URL was available and $JDBC_DIALECT_PROPERTY is auto"),
            )
        }

        val databaseDriver = DatabaseDriver.fromJdbcUrl(jdbcUrl)
        return if (databaseDriver == DatabaseDriver.POSTGRESQL) {
            ConditionOutcome.match(
                ConditionMessage
                    .forCondition("EventSourcingJdbc")
                    .because("datasource JDBC URL uses PostgreSQL"),
            )
        } else {
            ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition("EventSourcingJdbc")
                    .because("datasource JDBC URL uses ${databaseDriver.id}"),
            )
        }
    }

    private companion object {
        private const val JDBC_DIALECT_PROPERTY = "kopring.bricks.event-sourcing.jdbc.dialect"

        private val DATA_SOURCE_URL_PROPERTIES =
            listOf(
                "spring.datasource.url",
                "spring.datasource.jdbc-url",
                "spring.datasource.hikari.jdbc-url",
            )
    }
}
