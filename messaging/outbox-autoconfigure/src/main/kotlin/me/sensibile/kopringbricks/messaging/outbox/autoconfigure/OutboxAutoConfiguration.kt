package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import me.sensibile.kopringbricks.support.jdbc.autoconfigure.requireSimpleSqlIdentifier
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Clock
import javax.sql.DataSource

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "kopring.bricks.outbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(OutboxProperties::class)
class OutboxAutoConfiguration {
    @Bean
    @ConditionalOnClass(DataSource::class, JdbcClient::class)
    @ConditionalOnBean(JdbcClient::class)
    @Conditional(OutboxJdbcCondition::class)
    @ConditionalOnMissingBean(OutboxEventRepository::class)
    fun jdbcOutboxEventRepository(
        jdbcClient: JdbcClient,
        properties: OutboxProperties,
    ): OutboxEventRepository =
        JdbcOutboxEventRepository(
            jdbcClient,
            properties.jdbc.tableName.requireSimpleSqlIdentifier("kopring.bricks.outbox.jdbc.tableName"),
        )

    @Bean
    @ConditionalOnMissingBean(OutboxEventRepository::class)
    fun loggingOutboxEventRepository(): OutboxEventRepository = LoggingOutboxEventRepository()

    @Bean
    @ConditionalOnMissingBean
    fun outboxAppender(repository: OutboxEventRepository): OutboxEventAppender = OutboxEventAppender(repository)

    @Bean
    @ConditionalOnMissingBean
    fun outboxRetryPolicy(properties: OutboxProperties): OutboxRetryPolicy = OutboxRetryPolicy(properties)

    @Bean
    @ConditionalOnBean(OutboxEventPublisher::class)
    @ConditionalOnMissingBean
    fun outboxPollingService(
        repository: OutboxEventRepository,
        publisher: OutboxEventPublisher,
        retryPolicy: OutboxRetryPolicy,
        properties: OutboxProperties,
        clock: ObjectProvider<Clock>,
    ): OutboxPollingService =
        OutboxPollingService(
            repository,
            publisher,
            retryPolicy,
            properties,
            clock.getIfAvailable { Clock.systemUTC() },
        )

    @Bean(OUTBOX_TASK_SCHEDULER_BEAN_NAME)
    @ConditionalOnClass(TaskScheduler::class, ThreadPoolTaskScheduler::class)
    @ConditionalOnBean(OutboxPollingService::class)
    @ConditionalOnProperty(
        prefix = "kopring.bricks.outbox.scheduler",
        name = ["enabled"],
        havingValue = "true",
    )
    @ConditionalOnMissingBean(
        value = [OutboxScheduler::class],
        name = [OUTBOX_TASK_SCHEDULER_BEAN_NAME],
    )
    fun outboxTaskScheduler(properties: OutboxProperties): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = properties.scheduler.poolSize.coerceAtLeast(MIN_SCHEDULER_POOL_SIZE)
            setThreadNamePrefix(properties.scheduler.threadNamePrefix)
        }

    @Bean
    @ConditionalOnClass(TaskScheduler::class)
    @ConditionalOnBean(OutboxPollingService::class)
    @ConditionalOnProperty(
        prefix = "kopring.bricks.outbox.scheduler",
        name = ["enabled"],
        havingValue = "true",
    )
    @ConditionalOnMissingBean(OutboxScheduler::class)
    fun outboxScheduler(
        pollingService: OutboxPollingService,
        @Qualifier(OUTBOX_TASK_SCHEDULER_BEAN_NAME) taskScheduler: TaskScheduler,
        properties: OutboxProperties,
    ): OutboxScheduler = DefaultOutboxScheduler(pollingService, taskScheduler, properties)
}

private const val OUTBOX_TASK_SCHEDULER_BEAN_NAME = "outboxTaskScheduler"
private const val MIN_SCHEDULER_POOL_SIZE = 1
