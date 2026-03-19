package com.example.taskreminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.taskreminder.MainActivity
import com.example.taskreminder.R
import com.example.taskreminder.SettingsActivity
import com.example.taskreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SingleTaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
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

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_single_task)
            
            // Pending intent for standard root click -> Main
            val pendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // Settings button -> SettingsActivity
            val settingsIntent = PendingIntent.getActivity(
                context, 0, Intent(context, SettingsActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_settings, settingsIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val activeTasks = db.taskDao().getActiveTasks().firstOrNull()
                val nextTask = activeTasks?.minByOrNull { it.startDate }

                withContext(Dispatchers.Main) {
                    if (nextTask != null) {
                        views.setTextViewText(R.id.widget_task_name, nextTask.title)
                        
                        val (timeStr, dotResId) = formatRelativeTime(nextTask.startDate)
                        views.setTextViewText(R.id.widget_task_time, timeStr)
                        views.setImageViewResource(R.id.widget_task_dot, dotResId)
                        views.setViewVisibility(R.id.widget_task_dot, View.VISIBLE)
                        
                        views.setViewVisibility(R.id.widget_btn_done, View.VISIBLE)

                        val doneIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                            action = WidgetActionReceiver.ACTION_MARK_TASK_DONE
                            putExtra(WidgetActionReceiver.EXTRA_TASK_ID, nextTask.id)
                        }
                        val donePendingIntent = PendingIntent.getBroadcast(
                            context,
                            nextTask.id,
                            doneIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_btn_done, donePendingIntent)

                    } else {
                        views.setTextViewText(R.id.widget_task_name, "No active tasks")
                        views.setTextViewText(R.id.widget_task_time, "All caught up!")
                        views.setViewVisibility(R.id.widget_task_dot, View.GONE)
                        views.setViewVisibility(R.id.widget_btn_done, View.GONE)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
