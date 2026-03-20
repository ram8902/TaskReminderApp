package com.example.taskreminder.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.taskreminder.R

class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            ACTION_TOGGLE_DARK_MODE -> {
                val widgetId   = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val widgetType = intent.getStringExtra(EXTRA_WIDGET_TYPE) ?: return
                if (widgetId == -1) return

                WidgetPreferences.toggleDarkMode(context, widgetId)

                val mgr = AppWidgetManager.getInstance(context)
                when (widgetType) {
                    TYPE_SINGLE -> SingleTaskWidgetProvider.updateAppWidget(context, mgr, widgetId)
                    TYPE_LIST   -> {
                        TaskListWidgetProvider.updateAppWidget(context, mgr, widgetId)
                        mgr.notifyAppWidgetViewDataChanged(intArrayOf(widgetId), R.id.widget_list_view)
                    }
                }
            }

            ACTION_MARK_TASK_DONE -> {
                // Route to ConfirmTaskActivity instead of directly finishing the task
                val taskId    = intent.getIntExtra(EXTRA_TASK_ID, -1)
                val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: ""
                if (taskId == -1) return

                val confirmIntent = Intent(context, com.example.taskreminder.ConfirmTaskActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(com.example.taskreminder.ConfirmTaskActivity.EXTRA_TASK_ID, taskId)
                    putExtra(com.example.taskreminder.ConfirmTaskActivity.EXTRA_TASK_TITLE, taskTitle)
                }
                context.startActivity(confirmIntent)
            }
        }
    }

    companion object {
        const val ACTION_MARK_TASK_DONE  = "com.example.taskreminder.widget.ACTION_MARK_TASK_DONE"
        const val ACTION_TOGGLE_DARK_MODE = "com.example.taskreminder.widget.ACTION_TOGGLE_DARK_MODE"

        const val EXTRA_TASK_ID    = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_WIDGET_ID  = "EXTRA_WIDGET_ID"
        const val EXTRA_WIDGET_TYPE = "EXTRA_WIDGET_TYPE"

        const val TYPE_SINGLE = "single"
        const val TYPE_LIST   = "list"
    }
}
