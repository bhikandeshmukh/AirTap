package com.bhikan.airtap.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhikan.airtap.desktop.api.AirTapClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Composable
fun ScreenTab(client: AirTapClient) {
    var isStreaming by remember { mutableStateOf(false) }
    var screenStatus by remember { mutableStateOf("Checking...") }
    var frameBytes by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()

    fun checkStatus() {
        scope.launch {
            try {
                val status = client.getScreenStatus()
                screenStatus = status.message
                isStreaming = status.streaming
            } catch (e: Exception) {
                screenStatus = "Error: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) { checkStatus() }

    // Auto-refresh frames when streaming
    LaunchedEffect(isStreaming) {
        while (isStreaming) {
            try {
                frameBytes = client.getScreenFrame()
            } catch (_: Exception) {}
            delay(100) // ~10 FPS
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Screen Mirror", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { checkStatus() }) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
                
                Button(
                    onClick = { isStreaming = !isStreaming },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStreaming) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                        null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isStreaming) "Stop" else "Start")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            screenStatus,
            color = if (isStreaming) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Screen Display
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val imageBitmap = remember(frameBytes) {
                frameBytes?.let { bytes ->
                    runCatching {
                        Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    }.getOrNull()
                }
            }
            
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Phone Screen",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (frameBytes != null) {
                Icon(
                    Icons.Default.BrokenImage,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ScreenShare,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Start screen mirroring from phone",
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Start screen mirroring from the AirTap app on your phone first",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
