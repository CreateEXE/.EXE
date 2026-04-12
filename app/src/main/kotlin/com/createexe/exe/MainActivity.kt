package com.createexe.exe

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.createexe.exe.service.OverlayService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var overlayRunning by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasOverlayPermission = Settings.canDrawOverlays(context)
        }
    }

    val filePickerLauncher: ActivityResultLauncher<Array<String>> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val path = uri.lastPathSegment ?: ""
            if (!path.endsWith(".vrm", ignoreCase = true) &&
                !path.endsWith(".glb", ignoreCase = true)) {
                Toast.makeText(context, "Select a .vrm or .glb file.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w("MainActivity", "URI persist failed: ${e.message}")
            }
            val intent = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START
                putExtra(OverlayService.EXTRA_VRM_URI, uri.toString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            overlayRunning = true
        }

    val settingsLauncher: ActivityResultLauncher<Intent> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { hasOverlayPermission = Settings.canDrawOverlays(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Project.EXE",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF88),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Spatial Agent System",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))

            val statusText = when {
                overlayRunning       -> "● Agent Active"
                hasOverlayPermission -> "● Permission Granted"
                else                 -> "○ Permission Required"
            }
            val statusColor = when {
                overlayRunning       -> Color(0xFF00FF88)
                hasOverlayPermission -> Color(0xFFFFAA00)
                else                 -> Color(0xFFFF4444)
            }
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = statusColor,
                modifier = Modifier
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(32.dp))

            if (!hasOverlayPermission) {
                Text(
                    text = "Allow Project.EXE to display over other apps.",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        settingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(4.dp))
                ) {
                    Text("Grant Overlay Permission", color = Color(0xFF00FF88))
                }
            } else {
                if (overlayRunning) {
                    Button(
                        onClick = {
                            context.startService(
                                Intent(context, OverlayService::class.java).apply {
                                    action = OverlayService.ACTION_STOP
                                }
                            )
                            overlayRunning = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A0A0A)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, Color(0xFFFF4444), RoundedCornerShape(4.dp))
                    ) {
                        Text("Stop Agent", color = Color(0xFFFF4444))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1A0A)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        if (overlayRunning) "Change Avatar (.vrm / .glb)"
                        else "Select Avatar & Launch Agent",
                        color = Color(0xFF00FF88)
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
            Text("v0.1.0 — Edge AI Core", fontSize = 11.sp, color = Color(0xFF333333))
        }
    }
}
