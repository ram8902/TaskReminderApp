package com.example.taskreminder.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.taskreminder.AlarmActivity
import com.example.taskreminder.MainActivity
import com.example.taskreminder.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("TASK_ID", -1)
        if (taskId == -1) return Result.failure()

        return withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(appContext)
            val task = database.taskDao().getTaskById(taskId)

            if (task == null || !task.isActive) {
                WorkManager.getInstance(appContext).cancelAllWorkByTag("task_$taskId")
                return@withContext Result.success()
            }

            val currentTime = System.currentTimeMillis()

            if (currentTime > task.endDate) {
                WorkManager.getInstance(appContext).cancelAllWorkByTag("task_$taskId")
                database.taskDao().updateTask(task.copy(isActive = false))
                return@withContext Result.success()
            }

            // Still within date range — fire the alarm
            fireAlarm(taskId, task.title)

            Result.success()
        }
    }

    private fun fireAlarm(taskId: Int, taskTitle: String) {
        // Check POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isInteractive = powerManager.isInteractive

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        if (isInteractive) {
            // Screen is ON — show heads-up notification banner
            val mainIntent = Intent(appContext, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(appContext, taskId, mainIntent, flags)

            val notification = NotificationCompat.Builder(appContext, "HEADS_UP_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰  Task Reminder")
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(appContext).notify(taskId, notification)
        } else {
            // Screen is OFF — directly launch AlarmActivity.
            // WorkManager workers don't have bg-start exemption, so we also send
            // a full-screen notification as a lock-screen visual fallback.
            val alarmIntent = Intent(appContext, AlarmActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmActivity.EXTRA_TASK_TITLE, taskTitle)
                putExtra(AlarmActivity.EXTRA_TASK_ID, taskId)
            }

            // Attempt direct launch
            try { appContext.startActivity(alarmIntent) } catch (_: Exception) {}

            val alarmPendingIntent = PendingIntent.getActivity(
                appContext, taskId, alarmIntent, flags
            )

            val notification = NotificationCompat.Builder(appContext, "ALARM_CHANNEL")
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

            NotificationManagerCompat.from(appContext).notify(taskId, notification)
        }
    }
}
