package me.sensibile.kopringbricks.auditlog.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.audit-log")
data class AuditLogProperties(
    val enabled: Boolean = true,
    val publisher: Publisher = Publisher(),
    val jdbc: Jdbc = Jdbc(),
) {
    data class Publisher(
        val failOnError: Boolean = false,
    )

    data class Jdbc(
        val tableName: String = "audit_log",
        val dialect: AuditLogJdbcDialect = AuditLogJdbcDialect.AUTO,
        val flyway: Flyway = Flyway(),
    )

    data class Flyway(
        val enabled: Boolean = false,
    )
}

enum class AuditLogJdbcDialect {
    AUTO,
    POSTGRESQL,
}
