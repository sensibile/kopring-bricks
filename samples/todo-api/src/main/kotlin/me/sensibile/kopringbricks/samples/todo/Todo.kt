package me.sensibile.kopringbricks.samples.todo

data class Todo(
    val id: Long,
    val title: String,
    val completed: Boolean,
    val version: Long,
)
