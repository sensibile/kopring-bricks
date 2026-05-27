package me.sensibile.kopringbricks.web.concurrency.autoconfigure

fun interface ETagGenerator {
    fun generate(version: Any): String
}
