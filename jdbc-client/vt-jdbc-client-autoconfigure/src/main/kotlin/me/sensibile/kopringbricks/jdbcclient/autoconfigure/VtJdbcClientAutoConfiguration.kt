package me.sensibile.kopringbricks.jdbcclient.autoconfigure

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import javax.sql.DataSource

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.simple.JdbcClient

@AutoConfiguration(after = [JdbcTemplateAutoConfiguration::class])
@ConditionalOnClass(JdbcClient::class, DataSource::class)
@ConditionalOnProperty(prefix = "kopring.bricks.jdbc-client", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VtJdbcClientProperties::class)
class VtJdbcClientAutoConfiguration {

    @Bean
    @ConditionalOnBean(NamedParameterJdbcOperations::class)
    @ConditionalOnMissingBean
    fun kopringBricksJdbcClient(
        namedParameterJdbcOperations: NamedParameterJdbcOperations,
    ): JdbcClient =
        JdbcClient.create(namedParameterJdbcOperations)

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = ["kopringBricksJdbcExecutor"])
    @ConditionalOnProperty(
        prefix = "kopring.bricks.jdbc-client.virtual-threads",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun kopringBricksJdbcExecutor(properties: VtJdbcClientProperties): ExecutorService =
        Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name(properties.virtualThreads.threadNamePrefix, 0)
                .factory(),
        )

    @Bean
    @ConditionalOnBean(value = [JdbcClient::class], name = ["kopringBricksJdbcExecutor"])
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "kopring.bricks.jdbc-client",
        name = ["operations-enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun kopringBricksVtJdbcClientOperations(
        jdbcClient: JdbcClient,
        kopringBricksJdbcExecutor: ExecutorService,
    ): VtJdbcClientOperations =
        VtJdbcClientOperations(jdbcClient, kopringBricksJdbcExecutor)
}
