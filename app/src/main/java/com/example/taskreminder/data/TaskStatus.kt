package com.example.taskreminder.data

enum class TaskStatus {
    PENDING,
    COMPLETED,
    INCOMPLETE,
    MISSED;

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}
