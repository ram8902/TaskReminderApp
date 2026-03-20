package com.example.taskreminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.taskreminder.MainActivity
import com.example.taskreminder.QuickAddActivity
import com.example.taskreminder.R
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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPreferences.removeWidget(context, id)
    }

    companion object {

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

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val isDark = WidgetPreferences.isDarkMode(context, appWidgetId)
            val views  = RemoteViews(context.packageName, R.layout.widget_single_task)

            // ── Theme colors ──────────────────────────────────────────────
            val bgRes        = if (isDark) R.drawable.widget_bg_dark    else R.drawable.widget_bg_light
            val textPrimary  = if (isDark) 0xFFF0EEE8.toInt()           else 0xFF1A1A1A.toInt()
            val textSecondary= if (isDark) 0xFF888480.toInt()           else 0xFF888480.toInt()
            val iconBtnBg    = if (isDark) R.drawable.widget_icon_btn_bg_dark else R.drawable.widget_icon_btn_bg_light
            val doneBtnBg    = if (isDark) R.drawable.widget_icon_btn_bg_dark else R.drawable.widget_icon_btn_bg_light
            val toggleIcon   = if (isDark) R.drawable.ic_widget_sun     else R.drawable.ic_widget_moon

            views.setInt(R.id.widget_container,      "setBackgroundResource", bgRes)
            views.setTextColor(R.id.widget_title,     textSecondary)
            views.setTextColor(R.id.widget_task_name, textPrimary)
            views.setTextColor(R.id.widget_task_time, textSecondary)
            views.setInt(R.id.widget_btn_dark_toggle, "setBackgroundResource", iconBtnBg)
            views.setInt(R.id.widget_btn_add,         "setBackgroundResource", iconBtnBg)
            views.setInt(R.id.widget_btn_done,        "setBackgroundResource", doneBtnBg)
            views.setImageViewResource(R.id.widget_btn_dark_toggle, toggleIcon)

            // ── Dark mode toggle ──────────────────────────────────────────
            val toggleIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_TOGGLE_DARK_MODE
                putExtra(WidgetActionReceiver.EXTRA_WIDGET_ID,   appWidgetId)
                putExtra(WidgetActionReceiver.EXTRA_WIDGET_TYPE, WidgetActionReceiver.TYPE_SINGLE)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_dark_toggle, togglePendingIntent)

            // ── Title click → MainActivity ────────────────────────────────
            val mainIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title,     mainIntent)
            views.setOnClickPendingIntent(R.id.widget_task_name, mainIntent)

            // ── Add button → QuickAddActivity ─────────────────────────────
            val addIntent = Intent(context, QuickAddActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, appWidgetId * 10 + 1, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

            // ── Load task data ─────────────────────────────────────────────
            CoroutineScope(Dispatchers.IO).launch {
                val db       = AppDatabase.getDatabase(context)
                val nextTask = db.taskDao().getActiveTasks().firstOrNull()?.minByOrNull { it.startDate }

                withContext(Dispatchers.Main) {
                    if (nextTask != null) {
                        views.setTextViewText(R.id.widget_task_name, nextTask.title)

                        val (timeStr, dotResId) = formatRelativeTime(nextTask.startDate)
                        views.setTextViewText(R.id.widget_task_time, timeStr)
                        views.setImageViewResource(R.id.widget_task_dot, dotResId)
                        views.setViewVisibility(R.id.widget_task_dot,  View.VISIBLE)
                        views.setViewVisibility(R.id.widget_btn_done,  View.VISIBLE)

                        // Done button → ConfirmTaskActivity via receiver
                        val doneIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                            action = WidgetActionReceiver.ACTION_MARK_TASK_DONE
                            putExtra(WidgetActionReceiver.EXTRA_TASK_ID,    nextTask.id)
                            putExtra(WidgetActionReceiver.EXTRA_TASK_TITLE, nextTask.title)
                        }
                        val donePendingIntent = PendingIntent.getBroadcast(
                            context, nextTask.id,
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
