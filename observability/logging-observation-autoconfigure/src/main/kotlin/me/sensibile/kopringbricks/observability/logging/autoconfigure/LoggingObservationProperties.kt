package me.sensibile.kopringbricks.observability.logging.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.logging-observation")
class LoggingObservationProperties {
    var enabled: Boolean = true
    var json: Json = Json()
    var correlation: Correlation = Correlation()
    var mdc: Mdc = Mdc()
    var restClient: RestClient = RestClient()
    var taskDecorator: TaskDecorator = TaskDecorator()

    class Json {
        var enabled: Boolean = true
        var consoleFormat: String = "ecs"
    }

    class Correlation {
        var enabled: Boolean = true
        var requestHeaderName: String = "X-Request-Id"
        var responseHeaderName: String = "X-Request-Id"
        var generateIfMissing: Boolean = true
    }

    class Mdc {
        var enabled: Boolean = true
        var requestIdKey: String = "request_id"
    }

    class RestClient {
        var propagationEnabled: Boolean = true
    }

    class TaskDecorator {
        var enabled: Boolean = true
    }
}
