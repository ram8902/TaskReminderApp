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
        return TaskListFactory(this.applicationContext)
    }
}

class TaskListFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<Task> = emptyList()
    private val db = AppDatabase.getDatabase(context)

    override fun onCreate() {
    }

    override fun onDataSetChanged() {
        runBlocking {
            val activeTasks = db.taskDao().getActiveTasks().firstOrNull() ?: emptyList()
            tasks = activeTasks.sortedBy { it.startDate }
        }
    }

    override fun onDestroy() {
    }

    override fun getCount(): Int = tasks.size

    private fun formatRelativeTime(timeInMillis: Long): Pair<String, Int> {
        val now = System.currentTimeMillis()
        val diff = timeInMillis - now
        val isOverdue = diff < 0
        val absDiff = Math.abs(diff)

        val dotRes = if (isOverdue) R.drawable.ic_widget_dot_overdue 
                     else if (absDiff < 24 * 60 * 60 * 1000) R.drawable.ic_widget_dot_soon
                     else R.drawable.ic_widget_dot_normal

        val timeString = if (isOverdue) {
            val mins = absDiff / (1000 * 60)
            if (mins < 60) "Overdue by $mins m" else "Overdue by ${mins/60} h"
        } else {
            val hours = absDiff / (1000 * 60 * 60)
            if (hours > 24) {
                "Due in ${hours / 24} days"
            } else if (hours > 0) {
                "Due in $hours hrs"
            } else {
                "Due in ${absDiff / (1000 * 60)} mins"
            }
        }
        return Pair(timeString, dotRes)
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) return RemoteViews(context.packageName, R.layout.widget_task_list_item)

        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_list_item)

        views.setTextViewText(R.id.item_task_name, task.title)
        
        val (timeStr, dotResId) = formatRelativeTime(task.startDate)
        views.setTextViewText(R.id.item_task_time, timeStr)
        views.setImageViewResource(R.id.item_task_dot, dotResId)

        val fillInIntent = Intent().apply {
            putExtra(WidgetActionReceiver.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.item_btn_done, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
