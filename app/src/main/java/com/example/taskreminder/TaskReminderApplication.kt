package com.example.taskreminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

class TaskReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // General reminder channel (kept for compatibility)
            val reminderChannel = NotificationChannel(
                "REMINDER_CHANNEL",
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for task reminder notifications"
            }

            // Alarm channel — full-screen intents require a high-importance channel
            // with alarm audio attributes so the OS treats it as a real alarm.
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val alarmChannel = NotificationChannel(
                "ALARM_CHANNEL",
                "Alarm Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm notifications for task reminders"
                setSound(alarmUri, alarmAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 200, 600, 200, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(alarmChannel)

            // Heads-up channel — used when screen is ON (mimics a toast from above)
            val notifSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notifAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val headsUpChannel = NotificationChannel(
                "HEADS_UP_CHANNEL",
                "Heads-Up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up banner when phone is in use"
                enableVibration(true)
                setSound(notifSoundUri, notifAttributes)
            }
            notificationManager.createNotificationChannel(headsUpChannel)
        }
    }
}

