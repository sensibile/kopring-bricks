package me.sensibile.kopringbricks.web.error.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.webmvc-error")
class WebMvcErrorProperties {
    var enabled: Boolean = true
    var includeExceptionMessage: Boolean = false
    var internalErrorCode: String = "INTERNAL_SERVER_ERROR"
    var validationErrorCode: String = "VALIDATION_FAILED"
    var requestIdMdcKey: String = "request_id"
}
