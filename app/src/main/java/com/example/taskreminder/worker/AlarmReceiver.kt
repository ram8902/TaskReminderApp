package com.example.taskreminder.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.taskreminder.AlarmActivity
import com.example.taskreminder.MainActivity
import com.example.taskreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_INTERVAL_HOURS = "INTERVAL_HOURS"
        const val EXTRA_INTERVAL_MINUTES = "INTERVAL_MINUTES"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val intervalHours = intent.getIntExtra(EXTRA_INTERVAL_HOURS, 0)
        val intervalMinutes = intent.getIntExtra(EXTRA_INTERVAL_MINUTES, 0)

        if (taskId == -1) return

        // Use goAsync so we can do DB work without blocking the main thread
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)

                if (task == null || !task.isActive) {
                    pendingResult.finish()
                    return@launch
                }

                val now = System.currentTimeMillis()
                if (now > task.endDate) {
                    db.taskDao().updateTask(task.copy(isActive = false, status = com.example.taskreminder.data.TaskStatus.MISSED.name))
                    pendingResult.finish()
                    return@launch
                }

                // Fire the notification
                fireAlarm(context, taskId, task.title)

                // Schedule the NEXT alarm exactly N minutes from now
                val totalMillis = ((intervalHours * 60L) + intervalMinutes) * 60_000L
                val nextTrigger = now + totalMillis
                ReminderManager.setExactAlarm(
                    context, taskId, nextTrigger, intervalHours, intervalMinutes
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun fireAlarm(context: Context, taskId: Int, taskTitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isInteractive = powerManager.isInteractive

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT

        if (isInteractive) {
            // Screen ON — heads-up banner, auto-dismisses after 4 seconds
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(context, taskId, mainIntent, flags)

            val notification = NotificationCompat.Builder(context, "HEADS_UP_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰  Task Reminder")
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setTimeoutAfter(4000)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(taskId, notification)
        } else {
            // Screen is OFF — directly start AlarmActivity (exact-alarm receivers
            // have a special exemption to start activities in the background).
            val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmActivity.EXTRA_TASK_TITLE, taskTitle)
                putExtra(AlarmActivity.EXTRA_TASK_ID, taskId)
            }
            // Direct launch — works because this receiver is triggered by an exact alarm
            context.startActivity(alarmIntent)

            val alarmPendingIntent = PendingIntent.getActivity(context, taskId, alarmIntent, flags)

            // Full-screen notification as a visible lock-screen backstop
            val notification = NotificationCompat.Builder(context, "ALARM_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰  Task Reminder")
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(alarmPendingIntent, true)
                .setContentIntent(alarmPendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .build()

            NotificationManagerCompat.from(context).notify(taskId, notification)
        }
    }
}
