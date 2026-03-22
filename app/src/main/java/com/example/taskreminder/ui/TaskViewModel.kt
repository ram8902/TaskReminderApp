package com.example.taskreminder.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskreminder.R
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.data.Task
import com.example.taskreminder.data.TaskEvent
import com.example.taskreminder.data.TaskStatus
import com.example.taskreminder.widget.SingleTaskWidgetProvider
import com.example.taskreminder.widget.TaskListWidgetProvider
import com.example.taskreminder.worker.ReminderManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val taskEvents: StateFlow<List<TaskEvent>> = db.taskEventDao().getAllEvents().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(getApplication())

        val singleWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(getApplication(), SingleTaskWidgetProvider::class.java)
        )
        singleWidgetIds.forEach {
            SingleTaskWidgetProvider.updateAppWidget(getApplication(), appWidgetManager, it)
        }

        val listWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(getApplication(), TaskListWidgetProvider::class.java)
        )
        appWidgetManager.notifyAppWidgetViewDataChanged(listWidgetIds, R.id.widget_list_view)
    }

    fun addTask(
        title: String,
        description: String = "",
        startDate: Long,
        endDate: Long,
        intervalHours: Int,
        intervalMinutes: Int = 0
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                startDate = startDate,
                endDate = endDate,
                intervalHours = intervalHours,
                intervalMinutes = intervalMinutes,
                isActive = true,
                status = TaskStatus.PENDING.name
            )
            val id = taskDao.insertTask(task)
            ReminderManager.scheduleTaskReminder(
                getApplication<Application>().applicationContext,
                id.toInt(),
                intervalHours,
                intervalMinutes
            )
            updateWidgets()
        }
    }

    fun editTask(
        task: Task,
        title: String,
        description: String,
        startDate: Long,
        endDate: Long,
        intervalHours: Int,
        intervalMinutes: Int
    ) {
        viewModelScope.launch {
            // Cancel existing reminder before rescheduling
            ReminderManager.cancelTaskReminder(
                getApplication<Application>().applicationContext,
                task.id
            )
            val updated = task.copy(
                title = title,
                description = description,
                startDate = startDate,
                endDate = endDate,
                intervalHours = intervalHours,
                intervalMinutes = intervalMinutes
            )
            taskDao.updateTask(updated)
            if (updated.isActive) {
                ReminderManager.scheduleTaskReminder(
                    getApplication<Application>().applicationContext,
                    updated.id,
                    intervalHours,
                    intervalMinutes
                )
            }
            updateWidgets()
        }
    }

    fun finishTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isActive = false, status = TaskStatus.COMPLETED.name))
            db.taskEventDao().insertTaskEvent(
                TaskEvent(
                    taskId = task.id,
                    taskTitle = task.title,
                    action = TaskStatus.COMPLETED.name,
                    timestamp = System.currentTimeMillis()
                )
            )
            ReminderManager.cancelTaskReminder(
                getApplication<Application>().applicationContext,
                task.id
            )
            updateWidgets()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            ReminderManager.cancelTaskReminder(
                getApplication<Application>().applicationContext,
                task.id
            )
            updateWidgets()
        }
    }

    fun markIncomplete(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isActive = false, status = TaskStatus.INCOMPLETE.name))
            db.taskEventDao().insertTaskEvent(
                TaskEvent(
                    taskId = task.id,
                    taskTitle = task.title,
                    action = TaskStatus.INCOMPLETE.name,
                    timestamp = System.currentTimeMillis()
                )
            )
            ReminderManager.cancelTaskReminder(
                getApplication<Application>().applicationContext,
                task.id
            )
            updateWidgets()
        }
    }
}
