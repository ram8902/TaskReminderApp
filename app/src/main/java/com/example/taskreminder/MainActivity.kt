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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskreminder.data.Task
import com.example.taskreminder.data.TaskStatus
import com.example.taskreminder.ui.*
import android.content.Intent
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

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
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
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
                        darkTheme      = darkTheme,
                        onToggleTheme  = { darkTheme = !darkTheme },
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
// Root composable — orchestrates screens and sheet state
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
    var showTaskSheet   by remember { mutableStateOf(false) }
    var taskToEdit      by remember { mutableStateOf<Task?>(null) }
    var selectedTab     by remember { mutableStateOf(0) }
    var selectedFilter  by remember { mutableStateOf(TaskStatus.PENDING.name) }

    val allTasks    by viewModel.allTasks.collectAsState()
    val taskEvents  by viewModel.taskEvents.collectAsState()
    val listState   = rememberLazyListState()

    val displayTasks = allTasks.filter { it.status == selectedFilter }
    val pendingCount = allTasks.count { it.status == TaskStatus.PENDING.name }

    LaunchedEffect(openAddTaskFlag.value) {
        if (openAddTaskFlag.value) {
            showTaskSheet = true
            taskToEdit = null
            openAddTaskFlag.value = false
        }
    }

    val fabExpanded by remember { derivedStateOf { !listState.isScrollInProgress } }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGlassBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title          = when (selectedTab) { 0 -> "My Tasks"; 1 -> "History"; else -> "Stats" },
                    pendingCount   = pendingCount,
                    darkTheme      = darkTheme,
                    onToggleTheme  = onToggleTheme,
                    onOpenSettings = onOpenSettings
                )
            },
            bottomBar = {
                GlassBottomBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    GlassFab(
                        expanded = fabExpanded,
                        onClick  = { taskToEdit = null; showTaskSheet = true }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (selectedTab) {
                    0 -> TaskListScreen(
                        displayTasks    = displayTasks,
                        selectedFilter  = selectedFilter,
                        listState       = listState,
                        onFilterSelected = { selectedFilter = it },
                        onFinish        = viewModel::finishTask,
                        onIncomplete    = viewModel::markIncomplete,
                        onDelete        = viewModel::deleteTask,
                        onEdit          = { task -> taskToEdit = task; showTaskSheet = true }
                    )
                    1 -> HistoryScreen(events = taskEvents)
                    2 -> StatsScreen(tasks = allTasks, isDark = darkTheme)
                }
            }
        }
    }

    if (showTaskSheet) {
        TaskBottomSheet(
            existingTask = taskToEdit,
            onDismiss    = { showTaskSheet = false; taskToEdit = null },
            onSave       = { title, description, start, end, hours, minutes ->
                val editing = taskToEdit
                if (editing != null) {
                    viewModel.editTask(editing, title, description, start, end, hours, minutes)
                } else {
                    viewModel.addTask(title, description, start, end, hours, minutes)
                }
                showTaskSheet = false
                taskToEdit = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task list screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun TaskListScreen(
    displayTasks: List<Task>,
    selectedFilter: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onFilterSelected: (String) -> Unit,
    onFinish: (Task) -> Unit,
    onIncomplete: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterTabs(selectedFilter = selectedFilter, onFilterSelected = onFilterSelected)

        AnimatedContent(
            targetState = displayTasks.isEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "listEmptyTransition",
            modifier = Modifier.weight(1f)
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayTasks, key = { it.id }) { task ->
                        TaskItem(
                            task        = task,
                            onFinish    = { onFinish(task) },
                            onIncomplete = { onIncomplete(task) },
                            onDelete    = { onDelete(task) },
                            onEdit      = { onEdit(task) },
                            modifier    = Modifier.animateItemPlacement(
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassFab(expanded: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.95f,
        label = "fabScale"
    )
    GlassCard(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(56.dp)
            .clickable { onClick() },
        glassAlpha = 0.85f,
        borderAlpha = 0.3f,
        blurRadius  = 30.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(GlassTheme.accentPurple, Color(0xFF3B82F6))
                    ),
                    shape = RoundedCornerShape(20.dp)
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
