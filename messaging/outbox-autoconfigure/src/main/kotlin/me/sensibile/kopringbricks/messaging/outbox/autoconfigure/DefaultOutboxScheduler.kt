package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.scheduling.TaskScheduler
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicBoolean

class DefaultOutboxScheduler(
    private val pollingService: OutboxPollingService,
    private val taskScheduler: TaskScheduler,
    private val properties: OutboxProperties,
) : OutboxScheduler,
    SmartLifecycle {
    private val running = AtomicBoolean(false)
    private var scheduledFuture: ScheduledFuture<*>? = null

    override fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }

        scheduledFuture =
            taskScheduler.scheduleWithFixedDelay(
                ::poll,
                Instant.now().plus(properties.scheduler.initialDelay),
                properties.scheduler.fixedDelay,
            )
    }

    override fun stop() {
        scheduledFuture?.cancel(false)
        scheduledFuture = null
        running.set(false)
    }

    override fun isRunning(): Boolean = running.get()

    private fun poll() {
        runCatching {
            pollingService.poll()
        }.onSuccess { result ->
            logger.debug(
                "Outbox polling completed. claimed={}, published={}, failed={}",
                result.claimed,
                result.published,
                result.failed,
            )
        }.onFailure { exception ->
            logger.warn("Outbox polling failed.", exception)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DefaultOutboxScheduler::class.java)
    }
}
