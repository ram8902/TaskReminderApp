package com.example.taskreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskEvent(event: TaskEvent)

    @Query("SELECT * FROM task_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TaskEvent>>

    @Query("SELECT * FROM task_events WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getEventsForTask(taskId: Int): Flow<List<TaskEvent>>
    
    // For the 3D graph: grouped by day of week and hour of day (can be processed in memory or using SQLite STRFTIME. 
    // We'll just fetch all events or recent events and bucket them in Kotlin for flexibility)
    @Query("SELECT * FROM task_events ORDER BY timestamp ASC")
    suspend fun getAllEventsSync(): List<TaskEvent>
}
