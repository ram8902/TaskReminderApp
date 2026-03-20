package com.example.taskreminder.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderManager {

    /**
     * Schedules the first exact alarm for a task.
     * AlarmReceiver reschedules the next one after each fire.
     */
    fun scheduleTaskReminder(
        context: Context,
        taskId: Int,
        intervalHours: Int,
        intervalMinutes: Int
    ) {
        val totalMillis = ((intervalHours * 60L) + intervalMinutes) * 60_000L
        val triggerAt = System.currentTimeMillis() + totalMillis
        setExactAlarm(context, taskId, triggerAt, intervalHours, intervalMinutes)
    }

    /**
     * Sets a single exact alarm. Called from scheduleTaskReminder (first fire)
     * and from AlarmReceiver (each subsequent fire).
     */
    fun setExactAlarm(
        context: Context,
        taskId: Int,
        triggerAtMillis: Long,
        intervalHours: Int,
        intervalMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, taskId, intervalHours, intervalMinutes)!!

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    // Fallback if exact alarm permission not granted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val flags = PendingIntent.FLAG_NO_CREATE or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = buildPendingIntent(context, taskId, 0, 0, flags)
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    fun buildPendingIntent(
        context: Context,
        taskId: Int,
        intervalHours: Int,
        intervalMinutes: Int,
        flags: Int = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_INTERVAL_HOURS, intervalHours)
            putExtra(AlarmReceiver.EXTRA_INTERVAL_MINUTES, intervalMinutes)
        }
        return PendingIntent.getBroadcast(context, taskId, intent, flags)
    }
}
