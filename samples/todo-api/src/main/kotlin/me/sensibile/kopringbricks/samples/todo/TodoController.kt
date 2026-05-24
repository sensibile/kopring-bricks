package me.sensibile.kopringbricks.samples.todo

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/todos")
class TodoController(
    private val service: TodoService,
) {
    @GetMapping
    fun list(): List<Todo> = service.list()

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): Todo = service.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateTodoRequest,
    ): Todo = service.create(request)

    @PatchMapping("/{id}/complete")
    fun complete(
        @PathVariable id: Long,
    ): Todo = service.complete(id)
}
