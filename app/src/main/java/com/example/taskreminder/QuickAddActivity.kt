package com.example.taskreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.taskreminder.ui.TaskReminderTheme
import com.example.taskreminder.ui.TaskViewModel

class QuickAddActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Force dark theme for the sleek glassmorphism bottom sheet aesthetics, 
            // or adapt to system natively. We leave darkTheme default.
            TaskReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AddTaskBottomSheet(
                        onDismiss = { finish() },
                        onAdd = { title, startDate, endDate, interval ->
                            viewModel.addTask(title, startDate, endDate, interval)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
