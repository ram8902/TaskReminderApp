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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

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
                    onAdd     = { title, startDate, endDate, intervalHours, intervalMinutes ->
                        viewModel.addTask(title, startDate, endDate, intervalHours, intervalMinutes)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassTheme.priorityLow
                    )
                ) {
                    Text("Mark complete", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        isChecked = false
                    }
                ) {
                    Text("Cancel", color = GlassTheme.textSecondary)
                }
            }
        )
    }

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
                        val hrText = if (task.intervalHours > 0) "${task.intervalHours} hr${if (task.intervalHours != 1) "s" else ""}" else ""
                        val minText = if (task.intervalMinutes > 0) "${task.intervalMinutes} min" else ""
                        val intervalText = listOf(hrText, minText).filter { it.isNotEmpty() }.joinToString(" ")
                        Text(
                            text = "Every $intervalText",
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
                            if (it) {
                                showConfirmDialog = true
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
// Add Task Dialog  —  minimal / flat redesign
// ─────────────────────────────────────────────────────────────────────────────

// Design tokens for the flat sheet — independent of GlassTheme
private object FlatSheet {
    val surface      = Color(0xFFFAF9F7)       // warm off-white
    val surfaceDark  = Color(0xFF1E1E24)       // near-black for dark mode
    val border       = Color(0xFFE5E2DC)       // warm gray divider
    val borderDark   = Color(0xFF2E2E38)
    val textPrimary  = Color(0xFF1A1A1A)
    val textPrimaryDark = Color(0xFFF0EEE8)
    val textSecondary  = Color(0xFF888480)
    val textSecondaryDark = Color(0xFF888480)
    val accent       = Color(0xFF3D7A5F)       // muted sage green
    val accentDark   = Color(0xFF5BA882)       // lighter sage for dark bg
    val accentSurface = Color(0xFFEDF4F0)      // accent tint fill
    val accentSurfaceDark = Color(0xFF1A2E25)
    val handle       = Color(0xFFD4D0CA)
    val handleDark   = Color(0xFF3A3A44)
    val fieldBg      = Color(0xFFF2F0EC)
    val fieldBgDark  = Color(0xFF28282F)
    val fieldBorder  = Color(0xFFDAD7D1)
    val fieldBorderDark = Color(0xFF3A3A44)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(onDismiss: () -> Unit, onAdd: (String, Long, Long, Int, Int) -> Unit) {
    var title       by remember { mutableStateOf("") }
    var intervalStr by remember { mutableStateOf("0") }
    var minutesStr  by remember { mutableStateOf("0") }
    val isDark      = GlassTheme.isDark

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

    val sheetBg      = if (isDark) FlatSheet.surfaceDark      else FlatSheet.surface
    val divider      = if (isDark) FlatSheet.borderDark       else FlatSheet.border
    val labelColor   = if (isDark) FlatSheet.textSecondaryDark else FlatSheet.textSecondary
    val titleColor   = if (isDark) FlatSheet.textPrimaryDark  else FlatSheet.textPrimary
    val accent       = if (isDark) FlatSheet.accentDark       else FlatSheet.accent
    val accentTint   = if (isDark) FlatSheet.accentSurfaceDark else FlatSheet.accentSurface
    val fBg          = if (isDark) FlatSheet.fieldBgDark      else FlatSheet.fieldBg
    val fBorder      = if (isDark) FlatSheet.fieldBorderDark  else FlatSheet.fieldBorder
    val handleColor  = if (isDark) FlatSheet.handleDark       else FlatSheet.handle

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton    = { TextButton(onClick = { showStartPicker = false }) { Text("OK") } }
        ) { DatePicker(state = dateStateStart) }
    }
    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton    = { TextButton(onClick = { showEndPicker = false }) { Text("OK") } }
        ) { DatePicker(state = dateStateEnd) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "New task",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = titleColor
                )
            }

            // ── Divider + section label ──────────────────────────────────────
            Text(
                "SCHEDULE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = labelColor,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // ── Date row ────────────────────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlatDateButton(
                    label    = "Start",
                    dateStr  = startStr,
                    accent   = accent,
                    accentTint = accentTint,
                    bgColor  = fBg,
                    borderColor = fBorder,
                    textColor = titleColor,
                    labelColor = labelColor,
                    onClick  = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                )
                FlatDateButton(
                    label    = "End",
                    dateStr  = endStr,
                    accent   = accent,
                    accentTint = accentTint,
                    bgColor  = fBg,
                    borderColor = fBorder,
                    textColor = titleColor,
                    labelColor = labelColor,
                    onClick  = { showEndPicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Interval field ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(fBg)
                    .border(1.dp, fBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint   = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Remind every", fontSize = 14.sp, color = titleColor, modifier = Modifier.weight(1f))
                
                OutlinedTextField(
                    value         = intervalStr,
                    onValueChange = { intervalStr = it.filter { c -> c.isDigit() }.take(2) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = accent,
                        unfocusedBorderColor    = fBorder,
                        focusedContainerColor   = sheetBg,
                        unfocusedContainerColor = sheetBg,
                        focusedTextColor        = titleColor,
                        unfocusedTextColor      = titleColor,
                        cursorColor             = accent
                    ),
                    modifier = Modifier.width(52.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("hr", fontSize = 14.sp, color = labelColor)
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value         = minutesStr,
                    onValueChange = { minutesStr = it.filter { c -> c.isDigit() }.take(2) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = accent,
                        unfocusedBorderColor    = fBorder,
                        focusedContainerColor   = sheetBg,
                        unfocusedContainerColor = sheetBg,
                        focusedTextColor        = titleColor,
                        unfocusedTextColor      = titleColor,
                        cursorColor             = accent
                    ),
                    modifier = Modifier.width(52.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("min", fontSize = 14.sp, color = labelColor)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(divider))
            Spacer(modifier = Modifier.height(16.dp))

            // ── Section label ────────────────────────────────────────────────
            Text(
                "DETAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = labelColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ── Task title field ─────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                placeholder   = { Text("What do you need to do?", color = labelColor, fontSize = 14.sp) },
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accent,
                    unfocusedBorderColor = fBorder,
                    focusedContainerColor   = fBg,
                    unfocusedContainerColor = fBg,
                    focusedTextColor    = titleColor,
                    unfocusedTextColor  = titleColor,
                    cursorColor         = accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Save button ──────────────────────────────────────────────────
            val canSave = title.isNotBlank() && ((intervalStr.toIntOrNull() ?: 0) > 0 || (minutesStr.toIntOrNull() ?: 0) > 0)
            Button(
                onClick = {
                    val intervalHours   = intervalStr.toIntOrNull() ?: 0
                    val intervalMinutes = minutesStr.toIntOrNull() ?: 0
                    val start    = dateStateStart.selectedDateMillis ?: System.currentTimeMillis()
                    val end      = dateStateEnd.selectedDateMillis   ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                    if (canSave) onAdd(title, start, end, intervalHours, intervalMinutes)
                },
                enabled  = canSave,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = accent,
                    contentColor           = Color.White,
                    disabledContainerColor = fBg,
                    disabledContentColor   = labelColor
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save task", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun FlatDateButton(
    label: String,
    dateStr: String,
    accent: Color,
    accentTint: Color,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick      = onClick,
        shape        = RoundedCornerShape(10.dp),
        color        = bgColor,
        border       = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier     = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint     = accent,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 11.sp, color = labelColor, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(dateStr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        }
    }
}

// Keep the old DatePickerButton name as a shim so nothing else breaks
@Composable
fun DatePickerButton(
    label: String,
    dateStr: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val isDark = GlassTheme.isDark
    FlatDateButton(
        label       = label,
        dateStr     = dateStr,
        accent      = if (isDark) FlatSheet.accentDark else FlatSheet.accent,
        accentTint  = if (isDark) FlatSheet.accentSurfaceDark else FlatSheet.accentSurface,
        bgColor     = if (isDark) FlatSheet.fieldBgDark else FlatSheet.fieldBg,
        borderColor = if (isDark) FlatSheet.fieldBorderDark else FlatSheet.fieldBorder,
        textColor   = if (isDark) FlatSheet.textPrimaryDark else FlatSheet.textPrimary,
        labelColor  = FlatSheet.textSecondary,
        onClick     = onClick
    )
}
