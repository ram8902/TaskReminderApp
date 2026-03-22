package com.example.taskreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_events")
data class TaskEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val taskTitle: String,
    val action: String, // e.g. "COMPLETED", "ALARM_STOPPED", "ALARM_FIRED"
    val timestamp: Long
)
