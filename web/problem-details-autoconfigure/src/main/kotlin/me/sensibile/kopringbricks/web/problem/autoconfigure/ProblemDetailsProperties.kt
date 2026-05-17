package me.sensibile.kopringbricks.web.problem.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.problem-details")
class ProblemDetailsProperties {
    var enabled: Boolean = true
    var typeBaseUri: String = "https://sensibile.github.io/kopring-bricks/problems"
    var codePropertyName: String = "code"
    var requestIdPropertyName: String = "request_id"
}
