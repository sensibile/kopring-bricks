package me.sensibile.kopringbricks.samples.todo

import com.fasterxml.jackson.databind.ObjectMapper
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditActor
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditEvent
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditEventPublisher
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditTarget
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventAppender
import me.sensibile.kopringbricks.web.concurrency.autoconfigure.IfMatchValidator
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class TodoService(
    private val repository: TodoRepository,
    private val auditEvents: AuditEventPublisher,
    private val outbox: OutboxEventAppender,
    private val ifMatchValidator: IfMatchValidator,
    private val objectMapper: ObjectMapper,
) {
    fun list(): List<Todo> = repository.findAll()

    @Cacheable("todos")
    fun get(id: Long): Todo = repository.findById(id) ?: throw TodoNotFoundException(id)

    @CacheEvict(cacheNames = ["todos"], allEntries = true)
    fun create(request: CreateTodoRequest): Todo {
        val todo = repository.save(request.title)
        publishAuditEvent(todo, TODO_CREATED)
        appendOutboxEvent(todo, TODO_CREATED)

        return todo
    }

    @CacheEvict(cacheNames = ["todos"], key = "#id")
    fun complete(
        id: Long,
        ifMatchHeader: String?,
    ): Todo {
        val completed =
            repository.complete(id) { current ->
                ifMatchValidator.requireMatch(ifMatchHeader, current.version)
            }
        publishAuditEvent(completed, TODO_COMPLETED)
        appendOutboxEvent(completed, TODO_COMPLETED)

        return completed
    }

    private fun publishAuditEvent(
        todo: Todo,
        action: String,
    ) {
        auditEvents.publish(
            AuditEvent(
                actor = SYSTEM_ACTOR,
                action = action,
                target =
                    AuditTarget(
                        type = TODO_TARGET_TYPE,
                        id = todo.id.toString(),
                        name = todo.title,
                    ),
                metadataJson = todoJson(todo),
            ),
        )
    }

    private fun appendOutboxEvent(
        todo: Todo,
        eventType: String,
    ) {
        outbox.append(
            OutboxEvent(
                aggregateType = TODO_TARGET_TYPE,
                aggregateId = todo.id.toString(),
                eventType = eventType,
                payloadJson = todoJson(todo),
            ),
        )
    }

    private fun todoJson(todo: Todo): String = objectMapper.writeValueAsString(todo)

    private companion object {
        private const val TODO_TARGET_TYPE = "todo"
        private const val TODO_CREATED = "todo.created"
        private const val TODO_COMPLETED = "todo.completed"
        private val SYSTEM_ACTOR = AuditActor(type = "system", id = "sample")
    }
}
