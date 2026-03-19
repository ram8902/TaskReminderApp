package com.example.taskreminder

import android.content.Context
import android.content.SharedPreferences

class AlarmPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALARM_URI = "alarm_uri"
        private const val KEY_ALARM_VOLUME = "alarm_volume"
    }

    var alarmUri: String?
        get() = prefs.getString(KEY_ALARM_URI, null) // null means use default ringtone
        set(value) {
            prefs.edit().putString(KEY_ALARM_URI, value).apply()
        }

    var alarmVolume: Int
        get() = prefs.getInt(KEY_ALARM_VOLUME, 100) // Default to max volume (0 to 100)
        set(value) {
            prefs.edit().putInt(KEY_ALARM_VOLUME, value.coerceIn(0, 100)).apply()
        }
}
