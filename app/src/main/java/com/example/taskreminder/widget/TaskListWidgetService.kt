package com.example.taskreminder.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.taskreminder.R
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.data.Task
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
            android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return TaskListFactory(this.applicationContext, appWidgetId)
    }
}

class TaskListFactory(private val context: Context, private val appWidgetId: Int) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<Task> = emptyList()
    private val db = AppDatabase.getDatabase(context)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        tasks = db.taskDao().getActiveTasksSync().sortedBy { it.startDate }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = tasks.size

    private fun formatRelativeTime(timeInMillis: Long): Pair<String, Int> {
        val now     = System.currentTimeMillis()
        val diff    = timeInMillis - now
        val isOverdue = diff < 0
        val absDiff = Math.abs(diff)

        val dotRes = if (isOverdue) R.drawable.ic_widget_dot_overdue
                     else if (absDiff < 24 * 60 * 60 * 1000) R.drawable.ic_widget_dot_soon
                     else R.drawable.ic_widget_dot_normal

        val timeString = if (isOverdue) {
            val mins = absDiff / (1000 * 60)
            if (mins < 60) "Overdue by $mins m" else "Overdue by ${mins / 60} h"
        } else {
            val hours = absDiff / (1000 * 60 * 60)
            when {
                hours > 24 -> "Due in ${hours / 24} days"
                hours > 0  -> "Due in $hours hrs"
                else       -> "Due in ${absDiff / (1000 * 60)} mins"
            }
        }
        return Pair(timeString, dotRes)
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) return RemoteViews(context.packageName, R.layout.widget_task_list_item)

        val isDark = WidgetPreferences.isDarkMode(context, appWidgetId)
        val textPrimary = if (isDark) 0xFFF0EEE8.toInt() else 0xFF1A1A1A.toInt()

        val task  = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_list_item)

        views.setTextViewText(R.id.item_task_name, task.title)
        views.setTextColor(R.id.item_task_name, textPrimary)

        val (timeStr, dotResId) = formatRelativeTime(task.startDate)
        views.setTextViewText(R.id.item_task_time, timeStr)
        views.setImageViewResource(R.id.item_task_dot, dotResId)

        // Pass both task id AND title so ConfirmTaskActivity can show the name
        val fillInIntent = Intent().apply {
            putExtra(WidgetActionReceiver.EXTRA_TASK_ID,    task.id)
            putExtra(WidgetActionReceiver.EXTRA_TASK_TITLE, task.title)
        }
        views.setOnClickFillInIntent(R.id.item_btn_done, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
