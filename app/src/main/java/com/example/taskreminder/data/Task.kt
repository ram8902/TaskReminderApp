package com.example.taskreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val startDate: Long,
    val endDate: Long,
    val intervalHours: Int,
    val intervalMinutes: Int = 0,
    val isActive: Boolean = true,
    val status: String = TaskStatus.PENDING.name
)
