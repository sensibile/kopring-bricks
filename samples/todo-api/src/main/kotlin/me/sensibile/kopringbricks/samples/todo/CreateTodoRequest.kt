package me.sensibile.kopringbricks.samples.todo

import jakarta.validation.constraints.NotBlank

data class CreateTodoRequest(
    @field:NotBlank
    val title: String,
)
