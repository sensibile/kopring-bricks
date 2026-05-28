package me.sensibile.kopringbricks.samples.todo

import jakarta.validation.Valid
import me.sensibile.kopringbricks.web.concurrency.autoconfigure.ETagGenerator
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/todos")
class TodoController(
    private val service: TodoService,
    private val etags: ETagGenerator,
) {
    @GetMapping
    fun list(): List<Todo> = service.list()

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): ResponseEntity<Todo> {
        val todo = service.get(id)

        return ResponseEntity
            .ok()
            .eTag(etags.generate(todo.version))
            .body(todo)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateTodoRequest,
    ): Todo = service.create(request)

    @PatchMapping("/{id}/complete")
    fun complete(
        @PathVariable id: Long,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatchHeader: String?,
    ): ResponseEntity<Todo> {
        val todo = service.complete(id, ifMatchHeader)

        return ResponseEntity
            .ok()
            .eTag(etags.generate(todo.version))
            .body(todo)
    }
}
