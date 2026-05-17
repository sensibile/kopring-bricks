package me.sensibile.kopringbricks.cache.caffeine.autoconfigure

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.CaffeineSpec

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(Caffeine::class, CaffeineCacheManager::class)
@ConditionalOnProperty(
    prefix = "kopring.bricks.caffeine-cache",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableCaching
@EnableConfigurationProperties(CaffeineCacheProperties::class)
class CaffeineCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager::class)
    fun caffeineCacheManager(properties: CaffeineCacheProperties): CaffeineCacheManager {
        val cacheManager = CaffeineCacheManager().apply {
            setAllowNullValues(properties.allowNullValues)
            setCaffeineSpec(CaffeineSpec.parse(properties.spec))
        }

        val cacheNames = (properties.cacheNames + properties.caches.keys).distinct()
        if (cacheNames.isNotEmpty()) {
            cacheManager.setCacheNames(cacheNames)
        }

        properties.caches.forEach { (cacheName, cacheProperties) ->
            val spec = cacheProperties.spec
            if (!spec.isNullOrBlank()) {
                cacheManager.registerCustomCache(
                    cacheName,
                    Caffeine.from(spec).build<Any, Any>(),
                )
            }
        }

        return cacheManager
    }
}
