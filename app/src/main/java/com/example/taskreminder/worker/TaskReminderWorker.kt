package com.example.taskreminder.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.taskreminder.AlarmActivity
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

        // Intent that launches AlarmActivity
        val alarmIntent = Intent(appContext, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmActivity.EXTRA_TASK_TITLE, taskTitle)
            putExtra(AlarmActivity.EXTRA_TASK_ID, taskId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        val alarmPendingIntent = PendingIntent.getActivity(
            appContext,
            taskId,          // unique request code per task
            alarmIntent,
            flags
        )

        // Full-screen intent notification — required on Android 10+ for
        // launching an activity from the background.
        val notification = NotificationCompat.Builder(appContext, "ALARM_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰  Task Reminder")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(alarmPendingIntent, /* highPriority= */ true)
            .setContentIntent(alarmPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)   // keeps notification visible until alarm is stopped
            .build()

        NotificationManagerCompat.from(appContext)
            .notify(taskId, notification)
    }
}
