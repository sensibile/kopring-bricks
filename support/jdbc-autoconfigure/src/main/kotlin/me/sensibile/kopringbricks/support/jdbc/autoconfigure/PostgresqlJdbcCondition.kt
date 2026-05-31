package me.sensibile.kopringbricks.support.jdbc.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionMessage
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.jdbc.DatabaseDriver
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment

class PostgresqlJdbcCondition(
    private val conditionName: String,
    private val dialectProperty: String,
) {
    fun outcome(context: ConditionContext): ConditionOutcome =
        when (context.environment.jdbcDialect()) {
            JdbcDialect.POSTGRESQL -> postgresqlDialectMatch()
            JdbcDialect.AUTO -> context.environment.postgresqlUrlOutcome()
        }

    private fun Environment.jdbcDialect(): JdbcDialect =
        getProperty(dialectProperty)
            ?.let { JdbcDialect.valueOf(it.uppercase().replace("-", "_")) }
            ?: JdbcDialect.AUTO

    private fun postgresqlDialectMatch(): ConditionOutcome =
        ConditionOutcome.match(
            ConditionMessage
                .forCondition(conditionName)
                .because("$dialectProperty is postgresql"),
        )

    private fun Environment.postgresqlUrlOutcome(): ConditionOutcome {
        val jdbcUrl = DATA_SOURCE_URL_PROPERTIES.firstNotNullOfOrNull { getProperty(it) }
        if (jdbcUrl == null) {
            return ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition(conditionName)
                    .because("no datasource JDBC URL was available and $dialectProperty is auto"),
            )
        }

        val databaseDriver = DatabaseDriver.fromJdbcUrl(jdbcUrl)
        return if (databaseDriver == DatabaseDriver.POSTGRESQL) {
            ConditionOutcome.match(
                ConditionMessage
                    .forCondition(conditionName)
                    .because("datasource JDBC URL uses PostgreSQL"),
            )
        } else {
            ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition(conditionName)
                    .because("datasource JDBC URL uses ${databaseDriver.id}"),
            )
        }
    }

    private enum class JdbcDialect {
        AUTO,
        POSTGRESQL,
    }

    private companion object {
        private val DATA_SOURCE_URL_PROPERTIES =
            listOf(
                "spring.datasource.url",
                "spring.datasource.jdbc-url",
                "spring.datasource.hikari.jdbc-url",
            )
    }
}
