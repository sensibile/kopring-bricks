package me.sensibile.kopringbricks.jdbcclient.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.simple.JdbcClient
import java.util.concurrent.ExecutorService
import java.util.function.Supplier
import kotlin.test.Test

class VtJdbcClientAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
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
            ).run { context ->
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

    @Test
    fun `does not create jdbc client without named parameter jdbc operations`() {
        ApplicationContextRunner()
            .withUserConfiguration(VtJdbcClientAutoConfiguration::class.java)
            .run { context ->
                assertThat(context).doesNotHaveBean(JdbcClient::class.java)
                assertThat(context).doesNotHaveBean(VtJdbcClientOperations::class.java)
            }
    }

    @Test
    fun `backs off when custom jdbc client is registered`() {
        val customJdbcClient = mock(JdbcClient::class.java)

        contextRunner
            .withBean(JdbcClient::class.java, Supplier { customJdbcClient })
            .run { context ->
                assertThat(context).hasSingleBean(JdbcClient::class.java)
                assertThat(context.getBean(JdbcClient::class.java)).isSameAs(customJdbcClient)
            }
    }

    @Test
    fun `backs off when custom virtual thread operations are registered`() {
        val customOperations =
            VtJdbcClientOperations(
                mock(JdbcClient::class.java),
                Runnable::run,
            )

        contextRunner
            .withBean(VtJdbcClientOperations::class.java, Supplier { customOperations })
            .run { context ->
                assertThat(context).hasSingleBean(VtJdbcClientOperations::class.java)
                assertThat(context.getBean(VtJdbcClientOperations::class.java)).isSameAs(customOperations)
            }
    }
}
