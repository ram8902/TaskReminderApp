package com.example.taskreminder

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskreminder.ui.Coral
import com.example.taskreminder.ui.Mint
import com.example.taskreminder.ui.TaskReminderTheme
import kotlin.math.ln

class SettingsActivity : ComponentActivity() {

    private lateinit var preferences: AlarmPreferences
    private var mediaPlayer: MediaPlayer? = null

    private val ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            preferences.alarmUri = uri?.toString()
            // Reset media player if it was playing to preview the new sound
            stopPreview()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AlarmPreferences(this)

        setContent {
            TaskReminderTheme(darkTheme = true) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            )
                        )
                    }
                ) { padding ->
                    SettingsScreen(
                        modifier = Modifier.padding(padding),
                        preferences = preferences,
                        onPickRingtone = { pickRingtone() },
                        onPreviewToggle = { isPlaying, volume ->
                            if (isPlaying) stopPreview() else startPreview(volume)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPreview()
    }

    private fun pickRingtone() {
        val currentUriStr = preferences.alarmUri
        val currentUri = if (currentUriStr != null) Uri.parse(currentUriStr) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun startPreview(volumePercent: Int) {
        stopPreview()

        val uriStr = preferences.alarmUri
        val uri = if (uriStr != null) Uri.parse(uriStr) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Convert percentage (0-100) to logarithmic volume (0.0 to 1.0) for MediaPlayer
        val volume = (1 - (ln(101.0 - volumePercent) / ln(101.0))).coerceIn(0.0, 1.0).toFloat()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
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
    }

    private fun stopPreview() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: AlarmPreferences,
    onPickRingtone: () -> Unit,
    onPreviewToggle: (Boolean, Int) -> Unit
) {
    var volume by remember { mutableStateOf(preferences.alarmVolume.toFloat()) }
    var isPlaying by remember { mutableStateOf(false) }

    // Derive ringtone name for display, roughly.
    val ringtoneName = remember(preferences.alarmUri) {
        if (preferences.alarmUri == null) "Default System Alarm"
        else "Custom Sound Selected"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Sound Picker Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Coral)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Alarm Sound",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ringtoneName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPickRingtone,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Sound")
                }
            }
        }

        // Volume Slider Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Mint)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Alarm Volume",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${volume.toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = Mint
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = volume,
                    onValueChange = { 
                        volume = it
                        preferences.alarmVolume = it.toInt()
                    },
                    onValueChangeFinished = {
                        // Optionally auto-preview or update playing track volume
                        if (isPlaying) {
                            onPreviewToggle(true, volume.toInt()) // Stop it
                            onPreviewToggle(false, volume.toInt()) // Restart it with new volume
                        }
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Mint,
                        activeTrackColor = Mint,
                        inactiveTrackColor = Mint.copy(alpha = 0.24f)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Preview Button
                OutlinedButton(
                    onClick = {
                        onPreviewToggle(isPlaying, volume.toInt())
                        isPlaying = !isPlaying
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isPlaying) Coral else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Stop Preview" else "Preview Alarm")
                }
            }
        }
    }
}
