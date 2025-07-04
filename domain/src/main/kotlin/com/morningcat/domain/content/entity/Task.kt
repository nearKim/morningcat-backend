package com.morningcat.domain.content.entity

import java.time.LocalDateTime

data class Task(
    val title: String,
    val description: String?,
    val dueDate: LocalDateTime?,
    val priority: Priority,
    val isCompleted: Boolean = false
) {
    init {
        require(title.isNotBlank()) { "Task title cannot be blank" }
    }

    enum class Priority {
        HIGH,
        MEDIUM,
        LOW
    }
}