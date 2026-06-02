package me.sensibile.kopringbricks.support.jdbc.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.junit.jupiter.api.Test

class FlywayLocationsTests {
    @Test
    fun `appends classpath location without duplicating existing locations`() {
        val configuration = FluentConfiguration().locations("classpath:db/migration")

        configuration.appendFlywayLocation("classpath:db/migration")
        configuration.appendFlywayLocation("META-INF/kopring-bricks/outbox/flyway/postgresql")

        assertThat(configuration.locations.map { it.toString() })
            .containsExactlyInAnyOrder(
                "classpath:db/migration",
                "classpath:META-INF/kopring-bricks/outbox/flyway/postgresql",
            )
    }

    @Test
    fun `preserves non-classpath location prefix`() {
        val configuration = FluentConfiguration().locations("classpath:db/migration")

        configuration.appendFlywayLocation("filesystem:/opt/app/migrations")

        assertThat(configuration.locations.map { it.toString() })
            .containsExactlyInAnyOrder(
                "classpath:db/migration",
                "filesystem:/opt/app/migrations",
            )
    }
}
