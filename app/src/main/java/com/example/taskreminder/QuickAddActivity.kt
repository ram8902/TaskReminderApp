package com.example.taskreminder

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.example.taskreminder.ui.TaskReminderTheme
import com.example.taskreminder.ui.TaskViewModel

class QuickAddActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        showSheet()
    }

    // Called when singleTop re-uses existing instance (e.g. tapping widget add again)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Re-show keyboard since the activity is being brought back to front
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    private fun showSheet() {
        setContent {
            TaskReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AddTaskBottomSheet(
                        onDismiss = { finish() },
                        onAdd = { title, startDate, endDate, intervalHours, intervalMinutes ->
                            viewModel.addTask(title, startDate, endDate, intervalHours, intervalMinutes)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
