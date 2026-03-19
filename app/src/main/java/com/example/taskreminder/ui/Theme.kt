package com.example.taskreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────

// Shared accent colours (used in both themes)
val Coral   = Color(0xFFFF6B6B)
val Amber   = Color(0xFFFFBE0B)
val Mint    = Color(0xFF06D6A0)

// Dark-mode palette  (deep space purple-indigo)
private val DarkPrimary          = Color(0xFFB69DF8)   // light lavender
private val DarkOnPrimary        = Color(0xFF2D1A6B)
private val DarkPrimaryContainer = Color(0xFF4527A0)
private val DarkOnPrimaryContainer = Color(0xFFEDE7FF)
private val DarkSecondary        = Color(0xFF81D4FA)
private val DarkOnSecondary      = Color(0xFF003A52)
private val DarkSecondaryContainer = Color(0xFF004D6E)
private val DarkOnSecondaryContainer = Color(0xFFB3E5FC)
private val DarkBackground       = Color(0xFF0F0E18)
private val DarkSurface          = Color(0xFF1C1B2E)
private val DarkSurfaceVariant   = Color(0xFF2A2840)
private val DarkOnBackground     = Color(0xFFE8E6F0)
private val DarkOnSurface        = Color(0xFFE8E6F0)
private val DarkOnSurfaceVariant = Color(0xFFABA8C3)
private val DarkError            = Color(0xFFFF6B6B)
private val DarkOnError          = Color(0xFF5C0000)
private val DarkOutline          = Color(0xFF4A4870)

// Light-mode palette  (soft white + indigo)
private val LightPrimary          = Color(0xFF5C35D9)
private val LightOnPrimary        = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFEDE7FF)
private val LightOnPrimaryContainer = Color(0xFF2D1A6B)
private val LightSecondary        = Color(0xFF0277BD)
private val LightOnSecondary      = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFE1F5FE)
private val LightOnSecondaryContainer = Color(0xFF003A52)
private val LightBackground       = Color(0xFFF6F4FF)
private val LightSurface          = Color(0xFFFFFFFF)
private val LightSurfaceVariant   = Color(0xFFEDE9FA)
private val LightOnBackground     = Color(0xFF1A1730)
private val LightOnSurface        = Color(0xFF1A1730)
private val LightOnSurfaceVariant = Color(0xFF5A5470)
private val LightError            = Color(0xFFD32F2F)
private val LightOnError          = Color(0xFFFFFFFF)
private val LightOutline          = Color(0xFFB0ABC8)

// ── Color Schemes ─────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryContainer,
    onPrimaryContainer   = DarkOnPrimaryContainer,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background           = DarkBackground,
    surface              = DarkSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onBackground         = DarkOnBackground,
    onSurface            = DarkOnSurface,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    error                = DarkError,
    onError              = DarkOnError,
    outline              = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightPrimaryContainer,
    onPrimaryContainer   = LightOnPrimaryContainer,
    secondary            = LightSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background           = LightBackground,
    surface              = LightSurface,
    surfaceVariant       = LightSurfaceVariant,
    onBackground         = LightOnBackground,
    onSurface            = LightOnSurface,
    onSurfaceVariant     = LightOnSurfaceVariant,
    error                = LightError,
    onError              = LightOnError,
    outline              = LightOutline
)

// ── Theme Composable ──────────────────────────────────────────────────────────

@Composable
fun TaskReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
