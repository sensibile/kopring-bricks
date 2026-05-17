package me.sensibile.kopringbricks.cache.caffeine.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.caffeine-cache")
class CaffeineCacheProperties {
    var enabled: Boolean = true
    var spec: String = "maximumSize=10000,expireAfterWrite=10m,recordStats"
    var cacheNames: List<String> = emptyList()
    var allowNullValues: Boolean = false
    var caches: Map<String, Cache> = emptyMap()

    class Cache {
        var spec: String? = null
    }
}
