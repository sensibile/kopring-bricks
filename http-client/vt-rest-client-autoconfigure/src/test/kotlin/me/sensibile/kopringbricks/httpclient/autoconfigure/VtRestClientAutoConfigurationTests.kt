package me.sensibile.kopringbricks.httpclient.autoconfigure

import java.net.http.HttpClient
import java.util.concurrent.ExecutorService

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

class VtRestClientAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(VtRestClientAutoConfiguration::class.java)

    @Test
    fun `creates jdk http client based rest client infrastructure`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(VtRestClientProperties::class.java)
            assertThat(context).hasSingleBean(HttpClient::class.java)
            assertThat(context).hasSingleBean(ClientHttpRequestFactory::class.java)
            assertThat(context).hasSingleBean(RestClientCustomizer::class.java)
            assertThat(context.getBean(ClientHttpRequestFactory::class.java))
                .isInstanceOf(JdkClientHttpRequestFactory::class.java)
        }
    }

    @Test
    fun `can disable virtual thread executor`() {
        contextRunner
            .withPropertyValues("kopring.bricks.http-client.virtual-threads.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(ExecutorService::class.java)
                assertThat(context).hasSingleBean(HttpClient::class.java)
            }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.http-client.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(HttpClient::class.java)
                assertThat(context).doesNotHaveBean(ClientHttpRequestFactory::class.java)
                assertThat(context).doesNotHaveBean(RestClientCustomizer::class.java)
            }
    }

    @Test
    fun `creates named rest client factory when rest client builder exists`() {
        contextRunner
            .withBean(RestClient.Builder::class.java, RestClient::builder)
            .withPropertyValues(
                "kopring.bricks.http-client.clients.github.base-url=https://api.github.com",
                "kopring.bricks.http-client.clients.github.default-headers.accept[0]=application/vnd.github+json",
            )
            .run { context ->
                assertThat(context).hasSingleBean(VtRestClientFactory::class.java)

                val factory = context.getBean(VtRestClientFactory::class.java)

                assertThat(factory.restClient("github")).isNotNull
            }
    }
}
