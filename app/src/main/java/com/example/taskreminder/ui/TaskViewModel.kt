package com.example.taskreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.data.Task
import com.example.taskreminder.worker.ReminderManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.example.taskreminder.R
import com.example.taskreminder.widget.SingleTaskWidgetProvider
import com.example.taskreminder.widget.TaskListWidgetProvider

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()

    val activeTasks: StateFlow<List<Task>> = taskDao.getActiveTasks().stateIn(
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

    fun addTask(title: String, startDate: Long, endDate: Long, intervalHours: Int, intervalMinutes: Int = 0) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                startDate = startDate,
                endDate = endDate,
                intervalHours = intervalHours,
                intervalMinutes = intervalMinutes,
                isActive = true
            )
            val id = taskDao.insertTask(task)
            
            // Schedule the reminder using WorkManager
            ReminderManager.scheduleTaskReminder(
                getApplication<Application>().applicationContext,
                id.toInt(),
                intervalHours,
                intervalMinutes
            )
            updateWidgets()
        }
    }

    fun finishTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isActive = false))
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
}
