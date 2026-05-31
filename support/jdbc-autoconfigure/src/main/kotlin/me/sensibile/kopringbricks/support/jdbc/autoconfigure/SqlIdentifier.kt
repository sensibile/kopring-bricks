package me.sensibile.kopringbricks.support.jdbc.autoconfigure

fun String.requireSimpleSqlIdentifier(propertyPath: String): String {
    require(SQL_IDENTIFIER.matches(this)) {
        "$propertyPath must be a simple SQL identifier: $this"
    }

    return this
}

private val SQL_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
