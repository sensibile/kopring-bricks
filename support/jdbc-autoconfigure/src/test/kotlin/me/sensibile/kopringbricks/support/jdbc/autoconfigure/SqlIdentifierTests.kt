package me.sensibile.kopringbricks.support.jdbc.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class SqlIdentifierTests {
    @Test
    fun `returns valid simple SQL identifier`() {
        assertThat("audit_log".requireSimpleSqlIdentifier(PROPERTY_PATH)).isEqualTo("audit_log")
    }

    @Test
    fun `rejects unsafe SQL identifier`() {
        assertThatIllegalArgumentException()
            .isThrownBy { "audit-log".requireSimpleSqlIdentifier(PROPERTY_PATH) }
            .withMessage("$PROPERTY_PATH must be a simple SQL identifier: audit-log")
    }

    private companion object {
        private const val PROPERTY_PATH = "kopring.bricks.test.jdbc.tableName"
    }
}
