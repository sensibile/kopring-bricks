package me.sensibile.kopringbricks.web.error.autoconfigure

import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailFactory
import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailsAutoConfiguration
import me.sensibile.kopringbricks.web.problem.autoconfigure.ProblemDetailsProperties

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.error.ErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.DispatcherServlet

@AutoConfiguration(after = [ProblemDetailsAutoConfiguration::class])
@ConditionalOnClass(DispatcherServlet::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "kopring.bricks.webmvc-error",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(WebMvcErrorProperties::class)
class WebMvcErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun kopringBricksErrorAttributes(
        problemDetailsProperties: ProblemDetailsProperties,
        webMvcErrorProperties: WebMvcErrorProperties,
    ): ErrorAttributes =
        KopringBricksErrorAttributes(problemDetailsProperties, webMvcErrorProperties)

    @Bean
    @ConditionalOnBean(ProblemDetailFactory::class)
    @ConditionalOnMissingBean(KopringBricksWebMvcExceptionHandler::class)
    fun kopringBricksWebMvcExceptionHandler(
        problemDetailFactory: ProblemDetailFactory,
        problemDetailsProperties: ProblemDetailsProperties,
        webMvcErrorProperties: WebMvcErrorProperties,
    ): KopringBricksWebMvcExceptionHandler =
        KopringBricksWebMvcExceptionHandler(
            problemDetailFactory,
            problemDetailsProperties,
            webMvcErrorProperties,
        )
}
