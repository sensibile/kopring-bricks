package me.sensibile.kopringbricks.auditlog.autoconfigure

import me.sensibile.kopringbricks.support.jdbc.autoconfigure.test.indexExists
import me.sensibile.kopringbricks.support.jdbc.autoconfigure.test.tableExists
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource
import kotlin.test.Test

@Testcontainers
class AuditLogFlywayMigrationTests {
    @Test
    fun `applies bundled PostgreSQL audit log migration`() {
        val dataSource = createDataSource()

        Flyway
            .configure()
            .dataSource(dataSource)
            .locations(AUDIT_LOG_POSTGRESQL_FLYWAY_LOCATION)
            .load()
            .migrate()

        val jdbcClient = JdbcClient.create(dataSource)

        assertThat(jdbcClient.tableExists("audit_log")).isTrue()
        assertThat(jdbcClient.indexExists("audit_log_action_idx")).isTrue()
        assertThat(jdbcClient.indexExists("audit_log_target_idx")).isTrue()
    }

    private fun createDataSource(): DataSource =
        DriverManagerDataSource(
            POSTGRES.jdbcUrl,
            POSTGRES.username,
            POSTGRES.password,
        )

    private companion object {
        @Container
        @JvmStatic
        private val POSTGRES = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
    }
}
