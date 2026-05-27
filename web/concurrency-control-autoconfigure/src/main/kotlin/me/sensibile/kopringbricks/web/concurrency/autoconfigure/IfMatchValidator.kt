package me.sensibile.kopringbricks.web.concurrency.autoconfigure

class IfMatchValidator(
    private val etagGenerator: ETagGenerator,
    private val properties: ConcurrencyControlProperties,
) {
    fun requireMatch(
        ifMatchHeader: String?,
        currentVersion: Any,
    ): String {
        if (ifMatchHeader.isNullOrBlank()) {
            throw PreconditionRequiredException(properties.etag.missingIfMatchDetail)
        }

        val currentETag = etagGenerator.generate(currentVersion)
        if (ifMatchHeader.matches(currentETag)) {
            return currentETag
        }

        throw PreconditionFailedException(
            detail = properties.etag.preconditionFailedDetail,
            currentETag = currentETag,
        )
    }

    private fun String.matches(currentETag: String): Boolean =
        split(',')
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .any { it == "*" || it == currentETag }
}
