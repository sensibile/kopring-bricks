package me.sensibile.kopringbricks.httpclient.autoconfigure

import org.springframework.web.client.RestClient

class VtRestClientFactory(
    private val builder: RestClient.Builder,
    private val properties: VtRestClientProperties,
) {

    fun builder(name: String): RestClient.Builder {
        val client = properties.clients[name]
            ?: throw IllegalArgumentException("No RestClient named '$name' is configured.")

        val namedBuilder = builder.clone()
        client.baseUrl?.let(namedBuilder::baseUrl)
        client.defaultHeaders.forEach { (name, values) ->
            namedBuilder.defaultHeader(name, *values.toTypedArray())
        }

        return namedBuilder
    }

    fun restClient(name: String): RestClient =
        builder(name).build()
}
