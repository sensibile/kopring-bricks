package me.sensibile.kopringbricks.web.error.autoconfigure

import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailsAutoConfiguration

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.boot.webmvc.error.ErrorAttributes

class WebMvcErrorAutoConfigurationTests {

    private val contextRunner = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ProblemDetailsAutoConfiguration::class.java,
                WebMvcErrorAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `creates mvc error infrastructure`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(WebMvcErrorProperties::class.java)
            assertThat(context).hasSingleBean(ErrorAttributes::class.java)
            assertThat(context).hasSingleBean(KopringBricksWebMvcExceptionHandler::class.java)
        }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.webmvc-error.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(ErrorAttributes::class.java)
                assertThat(context).doesNotHaveBean(KopringBricksWebMvcExceptionHandler::class.java)
            }
    }
}
