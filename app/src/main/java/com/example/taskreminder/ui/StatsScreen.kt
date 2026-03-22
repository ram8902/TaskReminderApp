package com.example.taskreminder.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskreminder.data.Task

@Composable
fun StatsScreen(tasks: List<Task>, isDark: Boolean) {
    val pendingCount = tasks.count { it.status == "PENDING" }
    val completedCount = tasks.count { it.status == "COMPLETED" }
    val incompleteCount = tasks.count { it.status == "INCOMPLETE" }
    // Build JSON for the 3D pie chart: { pending, completed, missed }
    val dataJsonStr = remember(pendingCount, completedCount, incompleteCount) {
        JSONObject().apply {
            put("pending",   pendingCount)
            put("completed", completedCount)
            put("missed",    incompleteCount)
        }.toString()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // ── Top Stats Row ──────────────────────────────────────────────────
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            glassAlpha = 0.08f,
            borderAlpha = 0.15f,
            blurRadius = 30.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("Pending", pendingCount, Color(0xFFFACC15))
                StatItem("Completed", completedCount, Color(0xFF4ADE80))
                StatItem("Missed", incompleteCount, Color(0xFFF87171))
            }
        }
        
        // ── 3D Graph ───────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        
                        setBackgroundColor(0x00000000)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                view.evaluateJavascript("updateChart('$dataJsonStr', $isDark)", null)
                            }
                        }

                        loadUrl("file:///android_asset/echarts_3d_graph.html")
                    }
                },
                update = { webView ->
                    webView.evaluateJavascript("updateChart('$dataJsonStr', $isDark)", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GlassTheme.textPrimary)
        Text(text = label, fontSize = 12.sp, color = GlassTheme.textSecondary)
    }
}
