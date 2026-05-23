package me.sensibile.kopringbricks.jdbcclient.autoconfigure

import java.util.concurrent.ExecutorService
import java.util.function.Supplier

import kotlin.test.Test

import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.simple.JdbcClient

class VtJdbcClientAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(VtJdbcClientAutoConfiguration::class.java)
        .withBean(
            NamedParameterJdbcOperations::class.java,
            Supplier { mock(NamedParameterJdbcOperations::class.java) },
        )

    @Test
    fun `creates jdbc client and virtual thread operations`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(VtJdbcClientProperties::class.java)
            assertThat(context).hasSingleBean(JdbcClient::class.java)
            assertThat(context).hasSingleBean(ExecutorService::class.java)
            assertThat(context).hasSingleBean(VtJdbcClientOperations::class.java)
        }
    }

    @Test
    fun `can disable virtual thread executor and operations`() {
        contextRunner
            .withPropertyValues(
                "kopring.bricks.jdbc-client.virtual-threads.enabled=false",
                "kopring.bricks.jdbc-client.operations-enabled=false",
            )
            .run { context ->
                assertThat(context).hasSingleBean(JdbcClient::class.java)
                assertThat(context).doesNotHaveBean(ExecutorService::class.java)
                assertThat(context).doesNotHaveBean(VtJdbcClientOperations::class.java)
            }
    }

    @Test
    fun `does not create virtual thread operations when executor is disabled`() {
        contextRunner
            .withPropertyValues("kopring.bricks.jdbc-client.virtual-threads.enabled=false")
            .run { context ->
                assertThat(context).hasSingleBean(JdbcClient::class.java)
                assertThat(context).doesNotHaveBean(ExecutorService::class.java)
                assertThat(context).doesNotHaveBean(VtJdbcClientOperations::class.java)
            }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.jdbc-client.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(JdbcClient::class.java)
                assertThat(context).doesNotHaveBean(ExecutorService::class.java)
                assertThat(context).doesNotHaveBean(VtJdbcClientOperations::class.java)
            }
    }
}
