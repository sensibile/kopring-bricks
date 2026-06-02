package me.sensibile.kopringbricks.eventsourcing.autoconfigure

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
class EventSourcingFlywayMigrationTests {
    @Test
    fun `applies bundled PostgreSQL event sourcing migration`() {
        val dataSource = createDataSource()

        Flyway
            .configure()
            .dataSource(dataSource)
            .locations(EVENT_SOURCING_POSTGRESQL_FLYWAY_LOCATION)
            .load()
            .migrate()

        val jdbcClient = JdbcClient.create(dataSource)

        assertThat(jdbcClient.tableExists("event_store")).isTrue()
        assertThat(jdbcClient.uniqueConstraintExists("event_store_stream_id_stream_version_key")).isTrue()
        assertThat(jdbcClient.indexExists("idx_event_store_type")).isTrue()
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

private fun JdbcClient.uniqueConstraintExists(constraintName: String): Boolean =
    sql(
        """
        select exists (
            select 1
            from information_schema.table_constraints
            where constraint_schema = 'public'
              and constraint_type = 'UNIQUE'
              and constraint_name = :constraintName
        )
        """.trimIndent(),
    ).param("constraintName", constraintName)
        .query(Boolean::class.java)
        .single()
