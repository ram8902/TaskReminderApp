package com.example.taskreminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.taskreminder.MainActivity
import com.example.taskreminder.QuickAddActivity
import com.example.taskreminder.R

class TaskListWidgetProvider : AppWidgetProvider() {

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
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val isDark = WidgetPreferences.isDarkMode(context, appWidgetId)
            val views  = RemoteViews(context.packageName, R.layout.widget_task_list)

            // ── Theme colors ──────────────────────────────────────────────
            val bgRes        = if (isDark) R.drawable.widget_bg_dark    else R.drawable.widget_bg_light
            val textPrimary  = if (isDark) 0xFFF0EEE8.toInt()           else 0xFF1A1A1A.toInt()
            val dividerColor = if (isDark) 0xFF2E2E38.toInt()           else 0xFFE5E2DC.toInt()
            val iconBtnBg    = if (isDark) R.drawable.widget_icon_btn_bg_dark else R.drawable.widget_icon_btn_bg_light
            val toggleIcon   = if (isDark) R.drawable.ic_widget_sun     else R.drawable.ic_widget_moon

            views.setInt(R.id.widget_container_list, "setBackgroundResource", bgRes)
            views.setTextColor(R.id.widget_title, textPrimary)
            views.setInt(R.id.widget_divider,         "setBackgroundColor",   dividerColor)
            views.setInt(R.id.widget_btn_dark_toggle, "setBackgroundResource", iconBtnBg)
            views.setInt(R.id.widget_btn_add,         "setBackgroundResource", iconBtnBg)
            views.setImageViewResource(R.id.widget_btn_dark_toggle, toggleIcon)

            // ── Dark mode toggle ──────────────────────────────────────────
            val toggleIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_TOGGLE_DARK_MODE
                putExtra(WidgetActionReceiver.EXTRA_WIDGET_ID,   appWidgetId)
                putExtra(WidgetActionReceiver.EXTRA_WIDGET_TYPE, WidgetActionReceiver.TYPE_LIST)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_dark_toggle, togglePendingIntent)

            // ── Remote adapter for list ───────────────────────────────────
            val serviceIntent = Intent(context, TaskListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            // ── Click template for list items → ConfirmTaskActivity ───────
            val clickTemplate = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_MARK_TASK_DONE
            }
            val clickPendingTemplate = PendingIntent.getBroadcast(
                context, 0, clickTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingTemplate)

            // ── Title → MainActivity ──────────────────────────────────────
            val mainPendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)

            // ── Add button → QuickAddActivity ─────────────────────────────
            val addIntent = Intent(context, QuickAddActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, appWidgetId * 10 + 1, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
