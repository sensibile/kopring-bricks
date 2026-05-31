package me.sensibile.kopringbricks.support.jdbc.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.ResourceLoader

class PostgresqlJdbcConditionTests {
    private val condition =
        PostgresqlJdbcCondition(
            conditionName = "TestJdbc",
            dialectProperty = "kopring.bricks.test.jdbc.dialect",
        )

    @Test
    fun `matches explicit postgresql dialect`() {
        val outcome =
            condition.outcome(
                conditionContext("kopring.bricks.test.jdbc.dialect" to "postgresql"),
            )

        outcome.isMatchWithMessage("TestJdbc $DIALECT_MATCH_MESSAGE")
    }

    @Test
    fun `matches postgresql datasource URL when dialect is auto`() {
        val outcome =
            condition.outcome(
                conditionContext("spring.datasource.url" to "jdbc:postgresql://localhost/app"),
            )

        outcome.isMatchWithMessage("TestJdbc datasource JDBC URL uses PostgreSQL")
    }

    @Test
    fun `does not match when no JDBC URL is available`() {
        val outcome = condition.outcome(conditionContext())

        outcome.isNoMatchWithMessage("TestJdbc no datasource JDBC URL was available and $AUTO_MESSAGE_SUFFIX")
    }

    @Test
    fun `does not match non postgresql JDBC URL`() {
        val outcome =
            condition.outcome(
                conditionContext("spring.datasource.url" to "jdbc:h2:mem:test"),
            )

        outcome.isNoMatchWithMessage("TestJdbc datasource JDBC URL uses h2")
    }

    private fun conditionContext(vararg properties: Pair<String, String>): ConditionContext {
        val environment =
            StandardEnvironment().apply {
                propertySources.addFirst(MapPropertySource("test", properties.toMap()))
            }

        return StubConditionContext(environment)
    }

    private fun ConditionOutcome.isMatchWithMessage(message: String) {
        assertThat(isMatch).isTrue()
        assertThat(this.message.toString()).isEqualTo(message)
    }

    private fun ConditionOutcome.isNoMatchWithMessage(message: String) {
        assertThat(isMatch).isFalse()
        assertThat(this.message.toString()).isEqualTo(message)
    }

    private class StubConditionContext(
        private val environment: Environment,
    ) : ConditionContext {
        override fun getRegistry(): BeanDefinitionRegistry = SimpleBeanDefinitionRegistry()

        override fun getBeanFactory(): ConfigurableListableBeanFactory? = null

        override fun getEnvironment(): Environment = environment

        override fun getResourceLoader(): ResourceLoader = DefaultResourceLoader()

        override fun getClassLoader(): ClassLoader = javaClass.classLoader
    }

    private companion object {
        private const val DIALECT_MATCH_MESSAGE = "kopring.bricks.test.jdbc.dialect is postgresql"
        private const val AUTO_MESSAGE_SUFFIX = "kopring.bricks.test.jdbc.dialect is auto"
    }
}
