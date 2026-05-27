package me.sensibile.kopringbricks.web.concurrency.autoconfigure

class DefaultETagGenerator(
    private val properties: ConcurrencyControlProperties,
) : ETagGenerator {
    override fun generate(version: Any): String {
        val escapedVersion =
            version
                .toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        val tag = "\"$escapedVersion\""

        return if (properties.etag.strong) tag else "W/$tag"
    }
}
