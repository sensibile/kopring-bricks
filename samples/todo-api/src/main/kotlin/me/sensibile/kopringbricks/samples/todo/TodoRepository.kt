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
            )
        todos[todo.id] = todo
        return todo
    }

    fun complete(id: Long): Todo {
        val existing = todos[id] ?: throw TodoNotFoundException(id)
        val completed = existing.copy(completed = true)
        todos[id] = completed
        return completed
    }
}
