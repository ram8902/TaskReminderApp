package com.example.taskreminder

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taskreminder.data.AppDatabase
import com.example.taskreminder.ui.TaskReminderTheme
import com.example.taskreminder.widget.SingleTaskWidgetProvider
import com.example.taskreminder.widget.TaskListWidgetProvider
import com.example.taskreminder.worker.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfirmTaskActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TASK_ID    = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskId    = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "this task"

        if (taskId == -1) { finish(); return }

        setContent {
            TaskReminderTheme {
                ConfirmDialog(
                    taskTitle = taskTitle,
                    onConfirm = {
                        markDone(taskId)
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun markDone(taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db   = AppDatabase.getDatabase(applicationContext)
            val task = db.taskDao().getTaskById(taskId) ?: return@launch
            if (!task.isActive) return@launch

            db.taskDao().updateTask(task.copy(isActive = false))
            ReminderManager.cancelTaskReminder(applicationContext, taskId)

            val mgr = AppWidgetManager.getInstance(applicationContext)

            val singleIds = mgr.getAppWidgetIds(
                ComponentName(applicationContext, SingleTaskWidgetProvider::class.java)
            )
            singleIds.forEach {
                SingleTaskWidgetProvider.updateAppWidget(applicationContext, mgr, it)
            }

            val listIds = mgr.getAppWidgetIds(
                ComponentName(applicationContext, TaskListWidgetProvider::class.java)
            )
            mgr.notifyAppWidgetViewDataChanged(listIds, R.id.widget_list_view)
        }
    }
}

@Composable
private fun ConfirmDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onCancel:  () -> Unit
) {
    // Dim background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape         = RoundedCornerShape(20.dp),
            color         = Color(0xFF1C1B2E),
            tonalElevation = 8.dp,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Column(
                modifier              = Modifier.padding(24.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint     = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    "Complete task?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 17.sp,
                    color      = Color(0xFFE8E6F0)
                )

                Text(
                    "\"$taskTitle\" will be marked as done and reminders will stop.",
                    fontSize   = 13.sp,
                    color      = Color(0xFFABA8C3),
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel
                    TextButton(
                        onClick  = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFFABA8C3), fontWeight = FontWeight.Medium)
                    }

                    // Confirm
                    Button(
                        onClick  = onConfirm,
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(2f)
                    ) {
                        Text("Mark complete", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
