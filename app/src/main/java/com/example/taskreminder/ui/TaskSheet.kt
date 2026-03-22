package com.example.taskreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens (local to this sheet)
// ─────────────────────────────────────────────────────────────────────────────

private object FlatSheet {
    val surface         = Color(0xFFFAF9F7)
    val surfaceDark     = Color(0xFF1E1E24)
    val border          = Color(0xFFE5E2DC)
    val borderDark      = Color(0xFF2E2E38)
    val textPrimary     = Color(0xFF1A1A1A)
    val textPrimaryDark = Color(0xFFF0EEE8)
    val textSecondary   = Color(0xFF888480)
    val accent          = Color(0xFF3D7A5F)
    val accentDark      = Color(0xFF5BA882)
    val accentSurface   = Color(0xFFEDF4F0)
    val accentSurfDark  = Color(0xFF1A2E25)
    val handle          = Color(0xFFD4D0CA)
    val handleDark      = Color(0xFF3A3A44)
    val fieldBg         = Color(0xFFF2F0EC)
    val fieldBgDark     = Color(0xFF28282F)
    val fieldBorder     = Color(0xFFDAD7D1)
    val fieldBorderDark = Color(0xFF3A3A44)
}

// ─────────────────────────────────────────────────────────────────────────────
// Add / Edit Task Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Unified sheet for both adding and editing tasks.
 * Pass [existingTask] to pre-populate fields for editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBottomSheet(
    existingTask: Task? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, startDate: Long, endDate: Long, intervalHours: Int, intervalMinutes: Int) -> Unit
) {
    val isEditing = existingTask != null
    val isDark    = GlassTheme.isDark

    var title       by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var intervalStr by remember { mutableStateOf(existingTask?.intervalHours?.toString() ?: "0") }
    var minutesStr  by remember { mutableStateOf(existingTask?.intervalMinutes?.toString() ?: "0") }

    val dateStateStart = rememberDatePickerState(
        initialSelectedDateMillis = existingTask?.startDate ?: System.currentTimeMillis()
    )
    val dateStateEnd = rememberDatePickerState(
        initialSelectedDateMillis = existingTask?.endDate ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
    )

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }
    var dateError       by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val startStr   = dateStateStart.selectedDateMillis?.let { dateFormat.format(Date(it)) } ?: "–"
    val endStr     = dateStateEnd.selectedDateMillis?.let   { dateFormat.format(Date(it)) } ?: "–"

    // Derived colors
    val sheetBg     = if (isDark) FlatSheet.surfaceDark  else FlatSheet.surface
    val divider     = if (isDark) FlatSheet.borderDark   else FlatSheet.border
    val labelColor  = FlatSheet.textSecondary
    val titleColor  = if (isDark) FlatSheet.textPrimaryDark else FlatSheet.textPrimary
    val accent      = if (isDark) FlatSheet.accentDark   else FlatSheet.accent
    val accentTint  = if (isDark) FlatSheet.accentSurfDark else FlatSheet.accentSurface
    val fBg         = if (isDark) FlatSheet.fieldBgDark  else FlatSheet.fieldBg
    val fBorder     = if (isDark) FlatSheet.fieldBorderDark else FlatSheet.fieldBorder
    val handleColor = if (isDark) FlatSheet.handleDark   else FlatSheet.handle

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
            // ── Header ────────────────────────────────────────────────────────
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
                    Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (isEditing) "Edit task" else "New task",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = titleColor
                )
            }

            // ── DETAILS section (title + description first) ──────────────────
            Text(
                "DETAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = labelColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                placeholder   = { Text("What do you need to do?", color = labelColor, fontSize = 14.sp) },
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                colors        = outlinedColors(accent, fBorder, fBg, titleColor),
                modifier      = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                placeholder   = { Text("Notes (optional)", color = labelColor, fontSize = 14.sp) },
                maxLines      = 3,
                shape         = RoundedCornerShape(10.dp),
                colors        = outlinedColors(accent, fBorder, fBg, titleColor),
                modifier      = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(divider))
            Spacer(modifier = Modifier.height(16.dp))

            // ── SCHEDULE section ──────────────────────────────────────────────
            Text(
                "SCHEDULE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = labelColor,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlatDateButton(
                    label       = "Start",
                    dateStr     = startStr,
                    accent      = accent,
                    accentTint  = accentTint,
                    bgColor     = fBg,
                    borderColor = if (dateError) Color(0xFFEF4444) else fBorder,
                    textColor   = titleColor,
                    labelColor  = labelColor,
                    onClick     = { showStartPicker = true },
                    modifier    = Modifier.weight(1f)
                )
                FlatDateButton(
                    label       = "End",
                    dateStr     = endStr,
                    accent      = accent,
                    accentTint  = accentTint,
                    bgColor     = fBg,
                    borderColor = if (dateError) Color(0xFFEF4444) else fBorder,
                    textColor   = titleColor,
                    labelColor  = labelColor,
                    onClick     = { showEndPicker = true },
                    modifier    = Modifier.weight(1f)
                )
            }

            if (dateError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "End date must be after start date",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Interval row ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(fBg)
                    .border(1.dp, fBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Remind every", fontSize = 14.sp, color = titleColor, modifier = Modifier.weight(1f))

                OutlinedTextField(
                    value         = intervalStr,
                    onValueChange = { intervalStr = it.filter { c -> c.isDigit() }.take(2) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = outlinedColors(accent, fBorder, sheetBg, titleColor),
                    modifier      = Modifier.width(52.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("hr", fontSize = 14.sp, color = labelColor)
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value         = minutesStr,
                    onValueChange = { minutesStr = it.filter { c -> c.isDigit() }.take(2) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = outlinedColors(accent, fBorder, sheetBg, titleColor),
                    modifier      = Modifier.width(52.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("min", fontSize = 14.sp, color = labelColor)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Save button ──────────────────────────────────────────────────
            val hours   = intervalStr.toIntOrNull() ?: 0
            val minutes = minutesStr.toIntOrNull() ?: 0
            val canSave = title.isNotBlank() && (hours > 0 || minutes > 0)

            Button(
                onClick = {
                    val start = dateStateStart.selectedDateMillis ?: System.currentTimeMillis()
                    val end   = dateStateEnd.selectedDateMillis   ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                    if (end <= start) {
                        dateError = true
                        return@Button
                    }
                    dateError = false
                    onSave(title.trim(), description.trim(), start, end, hours, minutes)
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
                Text(
                    if (isEditing) "Save changes" else "Save task",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared sub-components
// ─────────────────────────────────────────────────────────────────────────────

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
                        tint = accent,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun outlinedColors(accent: Color, border: Color, bg: Color, text: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = accent,
        unfocusedBorderColor    = border,
        focusedContainerColor   = bg,
        unfocusedContainerColor = bg,
        focusedTextColor        = text,
        unfocusedTextColor      = text,
        cursorColor             = accent
    )
