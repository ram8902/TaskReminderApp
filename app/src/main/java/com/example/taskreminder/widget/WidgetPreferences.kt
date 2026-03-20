package com.example.taskreminder.widget

import android.content.Context

object WidgetPreferences {

    private const val PREFS_NAME = "widget_prefs"
    private const val KEY_DARK_MODE = "dark_mode_"

    fun isDarkMode(context: Context, widgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE + widgetId, false)
    }

    fun toggleDarkMode(context: Context, widgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getBoolean(KEY_DARK_MODE + widgetId, false)
        val next = !current
        prefs.edit().putBoolean(KEY_DARK_MODE + widgetId, next).apply()
        return next
    }

    fun removeWidget(context: Context, widgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DARK_MODE + widgetId).apply()
    }
}
