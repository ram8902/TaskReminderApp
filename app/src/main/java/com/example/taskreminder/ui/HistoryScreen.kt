package com.example.taskreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskreminder.data.TaskEvent
import com.example.taskreminder.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(events: List<TaskEvent>) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No task history yet.", color = GlassTheme.textSecondary)
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { event ->
            HistoryItem(event)
        }
    }
}

@Composable
fun HistoryItem(event: TaskEvent) {
    val (icon, iconTint, label) = when (event.action) {
        TaskStatus.COMPLETED.name  -> Triple(Icons.Default.CheckCircle,  GlassTheme.priorityLow,  "Completed")
        TaskStatus.INCOMPLETE.name -> Triple(Icons.Default.RemoveCircle,  GlassTheme.priorityHigh, "Marked incomplete")
        TaskStatus.MISSED.name     -> Triple(Icons.Default.NotificationsOff, GlassTheme.priorityMed, "Missed")
        else                       -> Triple(Icons.Default.NotificationsOff, GlassTheme.accentPurple, event.action)
    }

    val timeStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        .format(Date(event.timestamp))

    GlassCard(modifier = Modifier.fillMaxWidth(), glassAlpha = 0.08f, borderAlpha = 0.22f, blurRadius = 20.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(event.taskTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GlassTheme.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$label • $timeStr",
                    fontSize = 12.sp,
                    color = GlassTheme.textSecondary
                )
            }
        }
    }
}
