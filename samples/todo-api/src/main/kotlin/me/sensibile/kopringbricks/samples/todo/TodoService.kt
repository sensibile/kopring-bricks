package me.sensibile.kopringbricks.samples.todo

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class TodoService(
    private val repository: TodoRepository,
) {
    fun list(): List<Todo> = repository.findAll()

    @Cacheable("todos")
    fun get(id: Long): Todo = repository.findById(id) ?: throw TodoNotFoundException(id)

    @CacheEvict(cacheNames = ["todos"], allEntries = true)
    fun create(request: CreateTodoRequest): Todo = repository.save(request.title)

    @CacheEvict(cacheNames = ["todos"], key = "#id")
    fun complete(id: Long): Todo = repository.complete(id)
}
