package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

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
class OutboxFlywayMigrationTests {
    @Test
    fun `applies bundled PostgreSQL outbox migration`() {
        val dataSource = createDataSource()

        Flyway
            .configure()
            .dataSource(dataSource)
            .locations(OUTBOX_POSTGRESQL_FLYWAY_LOCATION)
            .load()
            .migrate()

        val jdbcClient = JdbcClient.create(dataSource)

        assertThat(jdbcClient.tableExists("outbox_event")).isTrue()
        assertThat(jdbcClient.indexExists("outbox_event_publish_idx")).isTrue()
        assertThat(jdbcClient.indexExists("outbox_event_aggregate_idx")).isTrue()
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

private fun JdbcClient.tableExists(tableName: String): Boolean =
    sql(
        """
        select exists (
            select 1
            from information_schema.tables
            where table_schema = 'public'
              and table_name = :tableName
        )
        """.trimIndent(),
    ).param("tableName", tableName)
        .query(Boolean::class.java)
        .single()

private fun JdbcClient.indexExists(indexName: String): Boolean =
    sql(
        """
        select exists (
            select 1
            from pg_indexes
            where schemaname = 'public'
              and indexname = :indexName
        )
        """.trimIndent(),
    ).param("indexName", indexName)
        .query(Boolean::class.java)
        .single()
