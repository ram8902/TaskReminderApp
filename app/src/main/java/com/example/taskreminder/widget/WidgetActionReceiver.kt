package com.example.taskreminder.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.taskreminder.R
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.worker.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_TASK_DONE) {
            val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
            if (taskId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null && task.isActive) {
                        db.taskDao().updateTask(task.copy(isActive = false))
                        ReminderManager.cancelTaskReminder(context, taskId)
                        
                        // Notify widgets to update
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        
                        val singleWidgetIds = appWidgetManager.getAppWidgetIds(
                            ComponentName(context, SingleTaskWidgetProvider::class.java)
                        )
                        singleWidgetIds.forEach {
                            SingleTaskWidgetProvider.updateAppWidget(context, appWidgetManager, it)
                        }

                        val listWidgetIds = appWidgetManager.getAppWidgetIds(
                            ComponentName(context, TaskListWidgetProvider::class.java)
                        )
                        appWidgetManager.notifyAppWidgetViewDataChanged(listWidgetIds, R.id.widget_list_view)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_TASK_DONE = "com.example.taskreminder.widget.ACTION_MARK_TASK_DONE"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
    }
}
