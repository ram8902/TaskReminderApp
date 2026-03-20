package com.example.taskreminder.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ReminderManager {

    fun scheduleTaskReminder(context: Context, taskId: Int, intervalHours: Int, intervalMinutes: Int) {
        val totalMinutes = (intervalHours * 60L) + intervalMinutes
        // WorkManager enforces a minimum repetition interval of 15 minutes.
        val safeMinutes = maxOf(15L, totalMinutes)

        val workRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            safeMinutes, TimeUnit.MINUTES
        )
            .setInitialDelay(safeMinutes, TimeUnit.MINUTES) // first ring after N minutes
            .setInputData(workDataOf("TASK_ID" to taskId))
            .addTag("task_$taskId")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "task_$taskId",
            ExistingPeriodicWorkPolicy.UPDATE, // Replace existing worker with same ID
            workRequest
        )
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag("task_$taskId")
    }
}
