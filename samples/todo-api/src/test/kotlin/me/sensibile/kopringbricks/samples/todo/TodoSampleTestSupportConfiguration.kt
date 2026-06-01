package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.testsupport.audit.RecordingAuditEventPublisher
import me.sensibile.kopringbricks.testsupport.eventsourcing.InMemoryEventStore
import me.sensibile.kopringbricks.testsupport.outbox.InMemoryOutboxEventRepository
import me.sensibile.kopringbricks.testsupport.outbox.RecordingOutboxEventPublisher
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TodoSampleTestSupportConfiguration {
    @Bean
    @Primary
    fun recordingAuditEventPublisher(): RecordingAuditEventPublisher = RecordingAuditEventPublisher()

    @Bean
    @Primary
    fun inMemoryEventStore(): InMemoryEventStore = InMemoryEventStore()

    @Bean
    @Primary
    fun inMemoryOutboxEventRepository(): InMemoryOutboxEventRepository = InMemoryOutboxEventRepository()

    @Bean
    @Primary
    fun recordingOutboxEventPublisher(): RecordingOutboxEventPublisher = RecordingOutboxEventPublisher()
}
