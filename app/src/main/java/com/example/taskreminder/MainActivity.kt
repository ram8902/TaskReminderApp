package com.example.taskreminder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskreminder.data.Task
import com.example.taskreminder.ui.Amber
import com.example.taskreminder.ui.Coral
import com.example.taskreminder.ui.Mint
import com.example.taskreminder.ui.TaskReminderTheme
import com.example.taskreminder.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            TaskReminderTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskApp(
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme },
                        onOpenSettings = {
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TaskApp
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun TaskApp(
    viewModel: TaskViewModel = viewModel(),
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    val tasks by viewModel.activeTasks.collectAsState()
    val listState = rememberLazyListState()

    // Collapse FAB text when scrolling
    val expanded by remember { derivedStateOf { !listState.isScrollInProgress } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GradientHeader(
                taskCount   = tasks.size,
                darkTheme   = darkTheme,
                onToggleTheme = onToggleTheme,
                onOpenSettings = onOpenSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick   = { showAddTaskDialog = true },
                expanded  = expanded,
                icon      = { Icon(Icons.Default.Add, contentDescription = null) },
                text      = { Text("New Task", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.shadow(12.dp, RoundedCornerShape(50))
            )
        }
    ) { padding ->
        // Smooth crossfade between empty state and the list
        AnimatedContent(
            targetState = tasks.isEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(200))
            },
            label = "listEmptyTransition"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    state           = listState,
                    modifier        = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task     = task,
                            onFinish = { viewModel.finishTask(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            // animateItemPlacement works with Compose 1.6.x (BOM 2024.02)
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness    = Spring.StiffnessMediumLow
                                )
                            )
                        )
                    }
                    // bottom spacer so last card clears the FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onAdd     = { title, startDate, endDate, interval ->
                    viewModel.addTask(title, startDate, endDate, interval)
                    showAddTaskDialog = false
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradient Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GradientHeader(
    taskCount: Int,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val gradientColors = if (darkTheme)
        listOf(Color(0xFF2D1A6B), Color(0xFF4527A0), Color(0xFF1C1B2E))
    else
        listOf(Color(0xFF5C35D9), Color(0xFF7C4DFF), Color(0xFFEDE7FF))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start  = Offset(0f, 0f),
                    end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text       = "Task Reminder",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(
                            text     = "Stay on track ✨",
                            fontSize = 13.sp,
                            color    = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick  = onOpenSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint               = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick  = onToggleTheme,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector        = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme",
                            tint               = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    label = "$taskCount Active",
                    icon  = Icons.Filled.CheckCircle,
                    tint  = Mint,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Reminders On",
                    icon  = Icons.Filled.Alarm,
                    tint  = Amber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier.fillMaxSize(),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        // Pulsing ring animation
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "scale"
        )

        Box(
            modifier         = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.AssignmentLate,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text       = "No tasks yet!",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text  = "Tap the button below to add\nyour first reminder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TaskItem(
    task: Task,
    onFinish: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val startStr   = dateFormat.format(Date(task.startDate))
    val endStr     = dateFormat.format(Date(task.endDate))

    val daysLeft = TimeUnit.MILLISECONDS.toDays(task.endDate - System.currentTimeMillis()).coerceAtLeast(0)
    val accentColor = when {
        daysLeft <= 1  -> Coral
        daysLeft <= 3  -> Amber
        else           -> Mint
    }
    val chipLabel = when {
        daysLeft == 0L -> "Due today"
        daysLeft == 1L -> "1 day left"
        else           -> "$daysLeft days left"
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Colored left accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.4f))),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 14.dp, end = 8.dp, bottom = 14.dp)
            ) {
                // Title + chip row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text       = task.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Days-remaining chip
                    Surface(
                        color  = accentColor.copy(alpha = 0.15f),
                        shape  = RoundedCornerShape(50),
                    ) {
                        Text(
                            text     = chipLabel,
                            fontSize = 11.sp,
                            color    = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date range row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = "$startStr → $endStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interval row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = "Every ${task.intervalHours} hr${if (task.intervalHours != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            Column(
                modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick  = onFinish,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Mint.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Finish",
                        tint     = Mint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint     = Coral,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Task Dialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, Long, Long, Int) -> Unit) {
    var title       by remember { mutableStateOf("") }
    var intervalStr by remember { mutableStateOf("2") }

    val dateStateStart = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val dateStateEnd = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
    )

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val startStr   = dateStateStart.selectedDateMillis?.let { dateFormat.format(Date(it)) } ?: "–"
    val endStr     = dateStateEnd.selectedDateMillis?.let   { dateFormat.format(Date(it)) } ?: "–"

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton    = {
                TextButton(onClick = { showStartPicker = false }) { Text("OK") }
            }
        ) { DatePicker(state = dateStateStart) }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton    = {
                TextButton(onClick = { showEndPicker = false }) { Text("OK") }
            }
        ) { DatePicker(state = dateStateEnd) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape     = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title     = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "New Task",
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Task title field
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Task title") },
                    leadingIcon   = { Icon(Icons.Outlined.Notes, contentDescription = null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    modifier      = Modifier.fillMaxWidth()
                )

                // Interval field
                OutlinedTextField(
                    value         = intervalStr,
                    onValueChange = { intervalStr = it.filter { c -> c.isDigit() } },
                    label         = { Text("Reminder interval (hours)") },
                    leadingIcon   = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    modifier      = Modifier.fillMaxWidth()
                )

                // Date section label
                Text(
                    "Schedule",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Start date chip
                DatePickerButton(
                    label    = "Start date",
                    dateStr  = startStr,
                    icon     = Icons.Outlined.CalendarMonth,
                    onClick  = { showStartPicker = true }
                )

                // End date chip
                DatePickerButton(
                    label    = "End date",
                    dateStr  = endStr,
                    icon     = Icons.Outlined.EventBusy,
                    onClick  = { showEndPicker = true }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val interval = intervalStr.toIntOrNull() ?: 2
                    val start    = dateStateStart.selectedDateMillis ?: System.currentTimeMillis()
                    val end      = dateStateEnd.selectedDateMillis   ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                    if (title.isNotBlank() && interval > 0) {
                        onAdd(title, start, end, interval)
                    }
                },
                shape    = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Task", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DatePickerButton(
    label: String,
    dateStr: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick      = onClick,
        shape        = RoundedCornerShape(14.dp),
        color        = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier     = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style  = MaterialTheme.typography.labelSmall,
                    color  = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateStr,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
