package com.example.taskreminder.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedGlassBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")

    // Orb 1: Purple
    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1_x"
    )
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1_y"
    )

    // Orb 2: Cyan
    val offsetX2 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2_x"
    )
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2_y"
    )

    // Orb 3: Pink
    val offsetX3 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb3_x"
    )
    val offsetY3 by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb3_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (GlassTheme.isDark) listOf(
                        Color(0xFF0D0D1A), // Deep purple
                        Color(0xFF0A1628), // Dark blue
                        Color(0xFF0D1F2D)  // Dark teal
                    ) else listOf(
                        Color(0xFFEDE7FF), // Light purple
                        Color(0xFFE1F5FE), // Light blue
                        Color(0xFFF6F4FF)  // Light teal/gray
                    )
                )
            )
    ) {
        // Orb 1: Purple at top-left
        Box(
            modifier = Modifier
                .offset(x = offsetX1.dp, y = offsetY1.dp)
                .size(300.dp)
                .align(Alignment.TopStart)
                .graphicsLayer { alpha = 0.25f }
                .blur(120.dp)
                .background(GlassTheme.accentPurple, shape = RoundedCornerShape(100))
        )

        // Orb 2: Cyan at bottom-right
        Box(
            modifier = Modifier
                .offset(x = offsetX2.dp, y = offsetY2.dp)
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer { alpha = 0.2f }
                .blur(100.dp)
                .background(GlassTheme.accentCyan, shape = RoundedCornerShape(100))
        )

        // Orb 3: Pink at center-right
        Box(
            modifier = Modifier
                .offset(x = offsetX3.dp, y = offsetY3.dp)
                .size(180.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer { alpha = 0.15f }
                .blur(80.dp)
                .background(GlassTheme.accentPink, shape = RoundedCornerShape(100))
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    glassAlpha: Float = 0.12f,
    borderAlpha: Float = 0.25f,
    content: @Composable () -> Unit
) {
    val isDarkTheme = GlassTheme.isDark

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassTheme.glassWhite,
                        GlassTheme.glassWhite.copy(alpha = GlassTheme.glassWhite.alpha / 2f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = GlassTheme.glassBorder,
                shape = RoundedCornerShape(20.dp)
            )
            // Note: Canvas provides inner shadow simulation
    ) {
        // Inner subtle white glow at top-left
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width / 2, size.height / 2)
                ),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
            )
        }
        content()
    }
}
