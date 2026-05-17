package me.sensibile.kopringbricks.web.problem.autoconfigure

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.env.MockEnvironment

class ProblemDetailsEnvironmentPostProcessorTests {

    @Test
    fun `enables spring mvc problem details by default`() {
        val environment = MockEnvironment()

        ProblemDetailsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication(Any::class.java))

        assertThat(environment.getProperty("spring.mvc.problemdetails.enabled")).isEqualTo("true")
    }

    @Test
    fun `does not override explicit spring mvc problem details setting`() {
        val environment = MockEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "test",
                mapOf("spring.mvc.problemdetails.enabled" to "false"),
            ),
        )

        ProblemDetailsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication(Any::class.java))

        assertThat(environment.getProperty("spring.mvc.problemdetails.enabled")).isEqualTo("false")
    }
}
