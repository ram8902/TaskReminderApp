package com.example.taskreminder.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassTopBar(
    title: String,
    pendingCount: Int,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        glassAlpha = 0.08f,
        borderAlpha = 0.15f,
        blurRadius = 30.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val gradientBrush = Brush.horizontalGradient(
                    colors = listOf(GlassTheme.accentPurple, GlassTheme.accentCyan)
                )
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(brush = gradientBrush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (title == "My Tasks") {
                    Text(
                        text = "$pendingCount pending task${if (pendingCount != 1) "s" else ""}",
                        fontSize = 13.sp,
                        color = GlassTheme.textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassCard(modifier = Modifier.size(42.dp), glassAlpha = 0.15f, borderAlpha = 0.3f) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = GlassTheme.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                GlassCard(modifier = Modifier.size(42.dp), glassAlpha = 0.15f, borderAlpha = 0.3f) {
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

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Navigation Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        glassAlpha = 0.85f,
        borderAlpha = 0.3f,
        blurRadius = 30.dp
    ) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            listOf(
                Triple(0, Icons.Default.List, "Tasks"),
                Triple(1, Icons.Default.History, "History"),
                Triple(2, Icons.Default.BarChart, "Stats")
            ).forEach { (index, icon, label) ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlassTheme.accentPurple,
                        unselectedIconColor = GlassTheme.textSecondary,
                        selectedTextColor = GlassTheme.accentPurple,
                        unselectedTextColor = GlassTheme.textSecondary,
                        indicatorColor = GlassTheme.accentPurple.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}
