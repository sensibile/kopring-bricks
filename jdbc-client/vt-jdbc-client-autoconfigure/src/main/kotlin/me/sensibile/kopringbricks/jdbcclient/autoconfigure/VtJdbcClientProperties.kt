package me.sensibile.kopringbricks.jdbcclient.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kopring.bricks.jdbc-client")
class VtJdbcClientProperties {
    var enabled: Boolean = true
    var operationsEnabled: Boolean = true
    var virtualThreads: VirtualThreads = VirtualThreads()

    class VirtualThreads {
        var enabled: Boolean = true
        var threadNamePrefix: String = "kopring-bricks-jdbc-"
    }
}
