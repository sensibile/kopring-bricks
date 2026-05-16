package me.sensibile.kopringbricks.httpclient.autoconfigure

import java.net.http.HttpClient
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

@AutoConfiguration
@ConditionalOnClass(RestClient::class, HttpClient::class, JdkClientHttpRequestFactory::class)
@ConditionalOnProperty(prefix = "kopring.bricks.http-client", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VtRestClientProperties::class)
class VtRestClientAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = ["kopringBricksHttpClientExecutor"])
    @ConditionalOnProperty(
        prefix = "kopring.bricks.http-client.virtual-threads",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun kopringBricksHttpClientExecutor(properties: VtRestClientProperties): ExecutorService =
        Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name(properties.virtualThreads.threadNamePrefix, 0)
                .factory(),
        )

    @Bean
    @ConditionalOnMissingBean
    fun kopringBricksJdkHttpClient(
        properties: VtRestClientProperties,
        @Qualifier("kopringBricksHttpClientExecutor") executor: ObjectProvider<ExecutorService>,
    ): HttpClient {
        val builder = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout)
            .followRedirects(
                if (properties.followRedirects) {
                    HttpClient.Redirect.NORMAL
                } else {
                    HttpClient.Redirect.NEVER
                },
            )

        executor.ifAvailable(builder::executor)

        return builder.build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun kopringBricksClientHttpRequestFactory(
        httpClient: HttpClient,
        properties: VtRestClientProperties,
    ): ClientHttpRequestFactory =
        JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(properties.readTimeout)
            enableCompression(properties.compressionEnabled)
        }

    @Bean
    fun kopringBricksRestClientCustomizer(
        requestFactory: ClientHttpRequestFactory,
    ): RestClientCustomizer =
        RestClientCustomizer { builder ->
            builder.requestFactory(requestFactory)
        }

    @Bean
    @ConditionalOnBean(RestClient.Builder::class)
    @ConditionalOnMissingBean
    fun kopringBricksRestClientFactory(
        builder: RestClient.Builder,
        properties: VtRestClientProperties,
    ): VtRestClientFactory =
        VtRestClientFactory(builder, properties)
}
