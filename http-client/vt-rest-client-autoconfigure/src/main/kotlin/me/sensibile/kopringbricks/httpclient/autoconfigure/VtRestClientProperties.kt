package me.sensibile.kopringbricks.httpclient.autoconfigure

import java.time.Duration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.http-client")
class VtRestClientProperties {
    var enabled: Boolean = true
    var connectTimeout: Duration = Duration.ofSeconds(3)
    var readTimeout: Duration = Duration.ofSeconds(10)
    var followRedirects: Boolean = false
    var compressionEnabled: Boolean = false
    var virtualThreads: VirtualThreads = VirtualThreads()
    var clients: Map<String, Client> = emptyMap()

    class VirtualThreads {
        var enabled: Boolean = true
        var threadNamePrefix: String = "kopring-bricks-http-"
    }

    class Client {
        var baseUrl: String? = null
        var defaultHeaders: Map<String, List<String>> = emptyMap()
    }
}
