package me.sensibile.kopringbricks.web.error.autoconfigure

import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailsAutoConfiguration
import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.boot.webmvc.error.ErrorAttributes
import org.springframework.http.HttpStatus

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

    @Test
    fun `api exception preserves configured code property name`() {
        contextRunner
            .withPropertyValues("kopring.bricks.problem-details.code-property-name=error_code")
            .run { context ->
                val handler = context.getBean(KopringBricksWebMvcExceptionHandler::class.java)
                val response = handler.handleApiException(
                    ApiException(
                        status = HttpStatus.NOT_FOUND,
                        code = "USER_NOT_FOUND",
                        detail = "User was not found",
                    ),
                )

                assertThat(response.body?.properties)
                    .containsEntry("error_code", "USER_NOT_FOUND")
            }
    }
}
