package me.sensibile.kopringbricks.web.problem.autoconfigure

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpStatus

class ProblemDetailsAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(ProblemDetailsAutoConfiguration::class.java)

    @Test
    fun `creates problem detail factory`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(ProblemDetailsProperties::class.java)
            assertThat(context).hasSingleBean(ProblemDetailFactory::class.java)

            val problem = context.getBean(ProblemDetailFactory::class.java)
                .create(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found")

            assertThat(problem.status).isEqualTo(404)
            assertThat(problem.properties).containsEntry("code", "USER_NOT_FOUND")
            assertThat(problem.type.toString()).endsWith("/user-not-found")
        }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.problem-details.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(ProblemDetailFactory::class.java)
            }
    }
}
