package me.sensibile.kopringbricks.support.jdbc.autoconfigure

import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.FluentConfiguration

fun FluentConfiguration.appendFlywayLocation(location: String) {
    val configuredLocations = getLocations().toMutableList()
    val additionalLocation = location.toFlywayLocation()
    if (!configuredLocations.contains(additionalLocation)) {
        configuredLocations += additionalLocation
    }
    locations(*configuredLocations.toTypedArray())
}

private fun String.toFlywayLocation(): Location {
    val prefix = LOCATION_PREFIX_REGEX.find(this)?.value ?: CLASSPATH_LOCATION_PREFIX
    return Location.fromPath(prefix, removePrefix(prefix))
}

private val LOCATION_PREFIX_REGEX = Regex("^[A-Za-z][A-Za-z0-9_-]*:")
private const val CLASSPATH_LOCATION_PREFIX = "classpath:"
