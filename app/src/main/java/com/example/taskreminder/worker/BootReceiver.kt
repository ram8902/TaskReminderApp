package com.example.taskreminder.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taskreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all active task alarms after a device reboot.
 * AlarmManager alarms are wiped on reboot — this receiver restores them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val tasks = db.taskDao().getActiveTasksSync()
                val now = System.currentTimeMillis()

                tasks.forEach { task ->
                    if (now <= task.endDate) {
                        ReminderManager.scheduleTaskReminder(
                            context,
                            task.id,
                            task.intervalHours,
                            task.intervalMinutes
                        )
                    } else {
                        db.taskDao().updateTask(task.copy(isActive = false))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
