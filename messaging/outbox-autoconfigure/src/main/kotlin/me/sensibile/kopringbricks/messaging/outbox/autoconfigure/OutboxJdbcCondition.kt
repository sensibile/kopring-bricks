package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionMessage
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.boot.jdbc.DatabaseDriver
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotatedTypeMetadata

class OutboxJdbcCondition : SpringBootCondition() {
    override fun getMatchOutcome(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): ConditionOutcome =
        when (context.environment.outboxJdbcDialect()) {
            OutboxJdbcDialect.POSTGRESQL -> postgresqlDialectMatch()
            OutboxJdbcDialect.AUTO -> context.environment.postgresqlUrlOutcome()
        }

    private fun Environment.outboxJdbcDialect(): OutboxJdbcDialect =
        getProperty(JDBC_DIALECT_PROPERTY)
            ?.let { OutboxJdbcDialect.valueOf(it.uppercase().replace("-", "_")) }
            ?: OutboxJdbcDialect.AUTO

    private fun postgresqlDialectMatch(): ConditionOutcome =
        ConditionOutcome.match(
            ConditionMessage
                .forCondition("OutboxJdbc")
                .because("$JDBC_DIALECT_PROPERTY is postgresql"),
        )

    private fun Environment.postgresqlUrlOutcome(): ConditionOutcome {
        val jdbcUrl = DATA_SOURCE_URL_PROPERTIES.firstNotNullOfOrNull { getProperty(it) }
        if (jdbcUrl == null) {
            return ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition("OutboxJdbc")
                    .because("no datasource JDBC URL was available and $JDBC_DIALECT_PROPERTY is auto"),
            )
        }

        val databaseDriver = DatabaseDriver.fromJdbcUrl(jdbcUrl)
        return if (databaseDriver == DatabaseDriver.POSTGRESQL) {
            ConditionOutcome.match(
                ConditionMessage
                    .forCondition("OutboxJdbc")
                    .because("datasource JDBC URL uses PostgreSQL"),
            )
        } else {
            ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition("OutboxJdbc")
                    .because("datasource JDBC URL uses ${databaseDriver.id}"),
            )
        }
    }

    private companion object {
        private const val JDBC_DIALECT_PROPERTY = "kopring.bricks.outbox.jdbc.dialect"

        private val DATA_SOURCE_URL_PROPERTIES =
            listOf(
                "spring.datasource.url",
                "spring.datasource.jdbc-url",
                "spring.datasource.hikari.jdbc-url",
            )
    }
}
