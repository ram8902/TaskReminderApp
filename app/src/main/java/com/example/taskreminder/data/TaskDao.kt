package com.example.taskreminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Query("SELECT * FROM tasks WHERE isActive = 1")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isActive = 1")
    fun getActiveTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    // Pending tasks first, then sorted by urgency (nearest deadline first)
    @Query("SELECT * FROM tasks ORDER BY CASE status WHEN 'PENDING' THEN 0 ELSE 1 END, endDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Delete
    suspend fun deleteTask(task: Task)
}
