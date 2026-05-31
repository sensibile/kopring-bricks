package me.sensibile.kopringbricks.samples.todo

import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class TodoRepository {
    private val idSequence = AtomicLong()
    private val todos = ConcurrentHashMap<Long, Todo>()

    fun findAll(): List<Todo> = todos.values.sortedBy { it.id }

    fun findById(id: Long): Todo? = todos[id]

    fun save(title: String): Todo {
        val todo =
            Todo(
                id = idSequence.incrementAndGet(),
                title = title,
                completed = false,
                version = INITIAL_VERSION,
            )
        todos[todo.id] = todo
        return todo
    }

    fun complete(
        id: Long,
        validate: (Todo) -> Unit = {},
    ): Todo {
        var completed: Todo? = null

        todos.compute(id) { _, existing ->
            val current = existing ?: throw TodoNotFoundException(id)

            validate(current)

            current
                .copy(
                    completed = true,
                    version = current.version + VERSION_INCREMENT,
                ).also { completed = it }
        }

        return requireNotNull(completed)
    }

    fun clear() {
        todos.clear()
        idSequence.set(0)
    }

    private companion object {
        private const val INITIAL_VERSION = 1L
        private const val VERSION_INCREMENT = 1L
    }
}
