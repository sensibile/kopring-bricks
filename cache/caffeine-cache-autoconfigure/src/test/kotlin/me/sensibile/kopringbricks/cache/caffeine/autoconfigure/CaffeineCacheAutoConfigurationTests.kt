package me.sensibile.kopringbricks.cache.caffeine.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.util.function.Supplier
import kotlin.test.Test

class CaffeineCacheAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(CaffeineCacheAutoConfiguration::class.java)

    @Test
    fun `creates caffeine cache manager`() {
        contextRunner
            .withPropertyValues(
                "kopring.bricks.caffeine-cache.cache-names[0]=users",
                "kopring.bricks.caffeine-cache.caches.products.spec=maximumSize=100,expireAfterWrite=1m,recordStats",
            ).run { context ->
                assertThat(context).hasSingleBean(CaffeineCacheProperties::class.java)
                assertThat(context).hasSingleBean(CacheManager::class.java)
                assertThat(context.getBean(CacheManager::class.java))
                    .isInstanceOf(CaffeineCacheManager::class.java)

                val cacheManager = context.getBean(CacheManager::class.java)

                assertThat(cacheManager.getCache("users")).isNotNull
                assertThat(cacheManager.getCache("products")).isNotNull
            }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.caffeine-cache.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(CacheManager::class.java)
            }
    }

    @Test
    fun `backs off when custom cache manager is registered`() {
        contextRunner
            .withBean(CacheManager::class.java, Supplier { ConcurrentMapCacheManager("users") })
            .run { context ->
                assertThat(context).hasSingleBean(CacheManager::class.java)
                assertThat(context.getBean(CacheManager::class.java))
                    .isInstanceOf(ConcurrentMapCacheManager::class.java)
            }
    }
}
