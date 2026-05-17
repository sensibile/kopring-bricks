package me.sensibile.kopringbricks.cache.caffeine.autoconfigure

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.env.MockEnvironment

class CaffeineCacheEnvironmentPostProcessorTests {

    @Test
    fun `sets caffeine cache type by default`() {
        val environment = MockEnvironment()

        CaffeineCacheEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication(Any::class.java))

        assertThat(environment.getProperty("spring.cache.type")).isEqualTo("caffeine")
    }

    @Test
    fun `does not override explicit cache type`() {
        val environment = MockEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "test",
                mapOf("spring.cache.type" to "simple"),
            ),
        )

        CaffeineCacheEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication(Any::class.java))

        assertThat(environment.getProperty("spring.cache.type")).isEqualTo("simple")
    }
}
