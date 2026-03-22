package com.example.taskreminder.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskreminder.data.Task
import com.example.taskreminder.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// Task Card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onFinish: () -> Unit,
    onIncomplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val startStr   = dateFormat.format(Date(task.startDate))
    val endStr     = dateFormat.format(Date(task.endDate))

    val daysLeft = TimeUnit.MILLISECONDS.toDays(task.endDate - System.currentTimeMillis()).coerceAtLeast(0)
    val accentColor = when {
        daysLeft <= 1  -> GlassTheme.priorityHigh
        daysLeft <= 3  -> GlassTheme.priorityMed
        else           -> GlassTheme.priorityLow
    }

    var isChecked by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                isChecked = false
            },
            containerColor = if (GlassTheme.isDark) Color(0xFF1C1B2E) else Color(0xFFFAF9F7),
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.priorityLow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = GlassTheme.priorityLow,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    "Complete task?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = GlassTheme.textPrimary
                )
            },
            text = {
                Text(
                    "\"${task.title}\" will be marked as done and reminders will stop.",
                    fontSize = 14.sp,
                    color = GlassTheme.textSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onFinish()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTheme.priorityLow)
                ) {
                    Text("Mark complete", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false; isChecked = false }) {
                    Text("Cancel", color = GlassTheme.textSecondary)
                }
            }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                GlassCard(modifier = Modifier.fillMaxSize(), glassAlpha = 0.2f, borderAlpha = 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GlassTheme.priorityHigh.copy(alpha = 0.4f))
                            .padding(end = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        val animatedAlpha by animateFloatAsState(
            targetValue = if (isChecked) 0.4f else 1f,
            label = "alpha"
        )

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = animatedAlpha },
            glassAlpha = 0.08f,
            borderAlpha = 0.22f,
            blurRadius = 20.dp
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Colored urgency bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.4f))),
                            RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, top = 14.dp, end = 8.dp, bottom = 14.dp)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = GlassTheme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = GlassTheme.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$startStr → $endStr",
                            fontSize = 12.sp,
                            color = GlassTheme.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = GlassTheme.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val hrText  = if (task.intervalHours > 0) "${task.intervalHours} hr${if (task.intervalHours != 1) "s" else ""}" else ""
                        val minText = if (task.intervalMinutes > 0) "${task.intervalMinutes} min" else ""
                        val intervalText = listOf(hrText, minText).filter { it.isNotEmpty() }.joinToString(" ")
                        Text(text = "Every $intervalText", fontSize = 12.sp, color = GlassTheme.textSecondary)
                    }
                }

                // Action buttons / status badge
                Box(
                    modifier = Modifier.fillMaxHeight().padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.status == TaskStatus.PENDING.name) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Edit button
                            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Edit task",
                                    tint = GlassTheme.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Mark incomplete
                            IconButton(onClick = onIncomplete, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Mark Incomplete",
                                    tint = GlassTheme.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Animated complete checkbox
                            AnimatedCheckbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    isChecked = checked
                                    if (checked) showConfirmDialog = true
                                }
                            )
                        }
                    } else {
                        val statusColor = when (task.status) {
                            TaskStatus.COMPLETED.name -> Color(0xFF10B981)
                            TaskStatus.MISSED.name    -> Color(0xFFF59E0B)
                            else                      -> Color(0xFFEF4444)
                        }
                        Text(
                            text = TaskStatus.entries.find { it.name == task.status }?.displayName() ?: task.status,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                if (checked) Brush.linearGradient(listOf(GlassTheme.accentPurple, GlassTheme.accentCyan))
                else SolidColor(Color.Transparent)
            )
            .border(
                width = if (checked) 0.dp else 2.dp,
                color = if (checked) Color.Transparent else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter Tabs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FilterTabs(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(TaskStatus.PENDING, TaskStatus.COMPLETED, TaskStatus.INCOMPLETE).forEach { status ->
            val isSelected = selectedFilter == status.name
            Surface(
                onClick = { onFilterSelected(status.name) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) GlassTheme.accentPurple.copy(alpha = 0.2f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) GlassTheme.accentPurple else GlassTheme.textSecondary.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = status.displayName(),
                    color = if (isSelected) GlassTheme.accentPurple else GlassTheme.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(modifier = Modifier.padding(32.dp), glassAlpha = 0.08f, borderAlpha = 0.2f) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f, targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "scale"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.accentPurple.copy(alpha = 0.2f))
                        .border(1.dp, GlassTheme.accentPurple.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = GlassTheme.accentCyan,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("No tasks yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GlassTheme.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap + to add your first task",
                    fontSize = 14.sp,
                    color = GlassTheme.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
