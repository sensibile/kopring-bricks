package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionMessage
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.boot.jdbc.DatabaseDriver
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class AuditLogJdbcCondition : SpringBootCondition() {
    override fun getMatchOutcome(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): ConditionOutcome =
        when (val dialect = context.environment.auditLogJdbcDialect()) {
            AuditLogJdbcDialect.POSTGRESQL -> postgresqlDialectMatch()
            AuditLogJdbcDialect.AUTO -> context.environment.auditLogJdbcUrl().postgresqlUrlOutcome()
        }

    private fun org.springframework.core.env.Environment.auditLogJdbcDialect(): AuditLogJdbcDialect =
        getProperty(JDBC_DIALECT_PROPERTY)
            ?.let { AuditLogJdbcDialect.valueOf(it.uppercase().replace("-", "_")) }
            ?: AuditLogJdbcDialect.AUTO

    private fun org.springframework.core.env.Environment.auditLogJdbcUrl(): String? =
        DATA_SOURCE_URL_PROPERTIES.firstNotNullOfOrNull { getProperty(it) }

    private fun postgresqlDialectMatch(): ConditionOutcome =
        ConditionOutcome.match(
            ConditionMessage
                .forCondition("AuditLogJdbc")
                .because("$JDBC_DIALECT_PROPERTY is postgresql"),
        )

    private fun String?.postgresqlUrlOutcome(): ConditionOutcome {
        if (this == null) {
            return ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition("AuditLogJdbc")
                    .because("no datasource JDBC URL was available and $JDBC_DIALECT_PROPERTY is auto"),
            )
        }

        val databaseDriver = DatabaseDriver.fromJdbcUrl(this)
        return if (databaseDriver == DatabaseDriver.POSTGRESQL) {
            ConditionOutcome.match(
                ConditionMessage
                    .forCondition("AuditLogJdbc")
                    .because("datasource JDBC URL uses PostgreSQL"),
            )
        } else {
            ConditionOutcome.noMatch(
                ConditionMessage
                    .forCondition("AuditLogJdbc")
                    .because("datasource JDBC URL uses ${databaseDriver.id}"),
            )
        }
    }

    private companion object {
        private const val JDBC_DIALECT_PROPERTY = "kopring.bricks.audit-log.jdbc.dialect"

        private val DATA_SOURCE_URL_PROPERTIES =
            listOf(
                "spring.datasource.url",
                "spring.datasource.jdbc-url",
                "spring.datasource.hikari.jdbc-url",
            )
    }
}
