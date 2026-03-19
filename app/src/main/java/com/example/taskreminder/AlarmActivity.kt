package com.example.taskreminder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskreminder.ui.Coral
import com.example.taskreminder.ui.TaskReminderTheme

class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TASK_TITLE = "TASK_TITLE"
        const val EXTRA_TASK_ID   = "TASK_ID"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Show over lock screen & keep screen on ──────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"

        startAlarm()

        setContent {
            TaskReminderTheme(darkTheme = true) {
                AlarmScreen(
                    taskTitle = taskTitle,
                    onStop    = { stopAlarm() }
                )
            }
        }
    }

    // ── Alarm sound + vibration ───────────────────────────────────────────────

    private fun startAlarm() {
        // Sound and Volume
        val preferences = AlarmPreferences(applicationContext)
        val customUriStr = preferences.alarmUri
        val volumePercent = preferences.alarmVolume

        val alarmUri = if (customUriStr != null) {
            android.net.Uri.parse(customUriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // Convert 0-100 percentage to 0.0-1.0 logarithmic scale
        val volume = (1 - (kotlin.math.ln(101.0 - volumePercent) / kotlin.math.ln(101.0))).coerceIn(0.0, 1.0).toFloat()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setAudioAttributes(attrs)
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }
                setVolume(volume, volume)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibration  (long-short-long pattern, repeat)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 600, 200, 600, 200, 1000, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, /* repeat= */ 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, /* repeat= */ 0)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        finish()
    }

    // ── Hardware key interception ─────────────────────────────────────────────

    /** Volume Up, Volume Down  → stop alarm */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                stopAlarm()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /** Power button → screen turns off → activity loses focus → stop alarm */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            stopAlarm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Alarm Screen UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlarmScreen(taskTitle: String, onStop: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")

    // Outer ring pulse
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "outer_scale"
    )
    // Inner icon pulse
    val innerScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "inner_scale"
    )
    // Icon tint flash red ↔ coral
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(500),
            repeatMode = RepeatMode.Reverse
        ), label = "icon_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A0A2E), Color(0xFF0F0E18)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // ── Pulsing rings + bell icon ──────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                // Outer fading ring
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(outerScale)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.12f))
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.20f))
                )
                // Inner filled circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(innerScale)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = iconAlpha),
                        modifier           = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Label ──────────────────────────────────────────────────────
            Text(
                text       = "⏰  Reminder",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = Coral.copy(alpha = 0.8f),
                letterSpacing = 4.sp
            )

            Text(
                text       = taskTitle,
                fontSize   = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                lineHeight = 36.sp
            )

            // ── Dismiss hint ────────────────────────────────────────────────
            Text(
                text  = "Press volume or power button to stop",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── STOP button ─────────────────────────────────────────────────
            Button(
                onClick = onStop,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Coral,
                    contentColor   = Color.White
                ),
                shape    = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "STOP ALARM",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
