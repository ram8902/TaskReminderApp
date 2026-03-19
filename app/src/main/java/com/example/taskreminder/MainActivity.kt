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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.taskreminder.ui.GlassTheme
import com.example.taskreminder.ui.GlassCard
import com.example.taskreminder.ui.AnimatedGlassBackground

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    private val openAddTaskState = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        if (intent.getBooleanExtra("EXTRA_OPEN_ADD_TASK", false)) {
            openAddTaskState.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openAddTaskState.value = intent.getBooleanExtra("EXTRA_OPEN_ADD_TASK", false)

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
                        },
                        openAddTaskFlag = openAddTaskState
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
    onOpenSettings: () -> Unit,
    openAddTaskFlag: MutableState<Boolean>
) {
    var showAddTaskSheet by remember { mutableStateOf(false) }
    val tasks by viewModel.activeTasks.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(openAddTaskFlag.value) {
        if (openAddTaskFlag.value) {
            showAddTaskSheet = true
            openAddTaskFlag.value = false
        }
    }

    // Collapse FAB text when scrolling
    val expanded by remember { derivedStateOf { !listState.isScrollInProgress } }

    Box(modifier = Modifier.fillMaxSize()) {
        // Behind everything: Animated background
        AnimatedGlassBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    taskCount   = tasks.size,
                    darkTheme   = darkTheme,
                    onToggleTheme = onToggleTheme,
                    onOpenSettings = onOpenSettings
                )
            },
            floatingActionButton = {
                // Pill-shaped glass FAB
                val scale by animateFloatAsState(
                    targetValue = if (expanded) 1f else 0.95f,
                    label = "fabScale"
                )
                GlassCard(
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .height(56.dp)
                        .clickable { showAddTaskSheet = true },
                    glassAlpha = 0.85f,
                    borderAlpha = 0.3f,
                    blurRadius = 30.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GlassTheme.accentPurple, Color(0xFF3B82F6))
                                )
                            )
                            .padding(horizontal = if (expanded) 24.dp else 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            AnimatedVisibility(visible = expanded) {
                                Row {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("New Task", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskItem(
                                task     = task,
                                onFinish = { viewModel.finishTask(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                modifier = Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness    = Spring.StiffnessMediumLow
                                    )
                                )
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            if (showAddTaskSheet) {
                AddTaskBottomSheet(
                    onDismiss = { showAddTaskSheet = false },
                    onAdd     = { title, startDate, endDate, interval ->
                        viewModel.addTask(title, startDate, endDate, interval)
                        showAddTaskSheet = false
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradient Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassTopBar(
    taskCount: Int,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Frosted glass top bar
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        glassAlpha = 0.08f,
        borderAlpha = 0.15f,
        blurRadius = 30.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val gradientBrush = Brush.horizontalGradient(
                    colors = listOf(GlassTheme.accentPurple, GlassTheme.accentCyan)
                )
                Text(
                    text = "My Tasks",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(brush = gradientBrush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$taskCount tasks due today",
                    fontSize = 13.sp,
                    color = GlassTheme.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassCard(
                    modifier = Modifier.size(42.dp),
                    glassAlpha = 0.15f,
                    borderAlpha = 0.3f
                ) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = GlassTheme.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                GlassCard(
                    modifier = Modifier.size(42.dp),
                    glassAlpha = 0.15f,
                    borderAlpha = 0.3f
                ) {
                    IconButton(onClick = onToggleTheme, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = GlassTheme.textPrimary
                        )
                    }
                }
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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.padding(32.dp),
            glassAlpha = 0.08f,
            borderAlpha = 0.2f
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Checkmark FastOutSlowIn
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

                Text(
                    text = "No tasks yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to add your first task",
                    fontSize = 14.sp,
                    color = GlassTheme.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task Card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
        daysLeft <= 1  -> GlassTheme.priorityHigh
        daysLeft <= 3  -> GlassTheme.priorityMed
        else           -> GlassTheme.priorityLow
    }
    
    var isChecked by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                GlassCard(
                    modifier = Modifier.fillMaxSize(),
                    glassAlpha = 0.2f,
                    borderAlpha = 0f
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GlassTheme.priorityHigh.copy(alpha = 0.4f))
                            .padding(end = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        val targetAlpha = if (isChecked) 0.4f else 1f
        val animatedAlpha by animateFloatAsState(targetValue = targetAlpha, label = "alpha")

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = animatedAlpha },
            glassAlpha = 0.08f,
            borderAlpha = 0.22f,
            blurRadius = 20.dp
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Colored left accent bar
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
                    // Title
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Date range row
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

                    // Interval row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = GlassTheme.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Every ${task.intervalHours} hr${if (task.intervalHours != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = GlassTheme.textSecondary
                        )
                    }
                }

                // Checkbox
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedCheckbox(
                        checked = isChecked,
                        onCheckedChange = { 
                            isChecked = it
                            if(it) {
                                // optional small delay before deleting/finishing
                                onFinish() 
                            }
                        }
                    )
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
        androidx.compose.animation.AnimatedVisibility(
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
// Add Task Dialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(onDismiss: () -> Unit, onAdd: (String, Long, Long, Int) -> Unit) {
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            glassAlpha = 0.15f,
            borderAlpha = 0.3f,
            blurRadius = 40.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "New Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = GlassTheme.textPrimary
                )

                // Task title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title", color = GlassTheme.textSecondary) },
                    leadingIcon = { Icon(Icons.Outlined.Notes, contentDescription = null, tint = GlassTheme.textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassTheme.accentCyan,
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedContainerColor = Color.White.copy(0.07f),
                        unfocusedContainerColor = Color.White.copy(0.07f),
                        focusedTextColor = GlassTheme.textPrimary,
                        unfocusedTextColor = GlassTheme.textPrimary,
                        cursorColor = GlassTheme.accentCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Interval field
                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { intervalStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Reminder interval (hours)", color = GlassTheme.textSecondary) },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null, tint = GlassTheme.textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassTheme.accentCyan,
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedContainerColor = Color.White.copy(0.07f),
                        unfocusedContainerColor = Color.White.copy(0.07f),
                        focusedTextColor = GlassTheme.textPrimary,
                        unfocusedTextColor = GlassTheme.textPrimary,
                        cursorColor = GlassTheme.accentCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "SCHEDULE",
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.White.copy(0.4f),
                    fontWeight = FontWeight.SemiBold
                )

                // Start date
                DatePickerButton(
                    label = "Start date",
                    dateStr = startStr,
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { showStartPicker = true }
                )

                // End date
                DatePickerButton(
                    label = "End date",
                    dateStr = endStr,
                    icon = Icons.Outlined.EventBusy,
                    onClick = { showEndPicker = true }
                )

                Button(
                    onClick = {
                        val interval = intervalStr.toIntOrNull() ?: 2
                        val start    = dateStateStart.selectedDateMillis ?: System.currentTimeMillis()
                        val end      = dateStateEnd.selectedDateMillis   ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                        if (title.isNotBlank() && interval > 0) {
                            onAdd(title, start, end, interval)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(GlassTheme.accentPurple, GlassTheme.accentCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Task", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DatePickerButton(
    label: String,
    dateStr: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = GlassTheme.accentCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = GlassTheme.textSecondary
                )
                Text(
                    dateStr,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTheme.textPrimary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GlassTheme.textSecondary
            )
        }
    }
}
