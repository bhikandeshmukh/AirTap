package com.bhikan.airtap.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhikan.airtap.desktop.api.AirTapClient
import kotlinx.coroutines.launch

@Composable
fun ControlTab(client: AirTapClient) {
    var controlStatus by remember { mutableStateOf("Checking...") }
    var isEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Phone screen dimensions (default)
    val phoneWidth = 1080f
    val phoneHeight = 1920f

    fun checkStatus() {
        scope.launch {
            try {
                val status = client.getControlStatus()
                controlStatus = status.message
                isEnabled = status.enabled
            } catch (e: Exception) {
                controlStatus = "Error: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) { checkStatus() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Remote Control", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { checkStatus() }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            controlStatus,
            color = if (isEnabled) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Buttons
            Column(
                modifier = Modifier.width(200.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Navigation", fontWeight = FontWeight.Bold)
                
                ControlButton(Icons.Default.ArrowBack, "Back") {
                    scope.launch { client.pressButton("back") }
                }
                ControlButton(Icons.Default.Home, "Home") {
                    scope.launch { client.pressButton("home") }
                }
                ControlButton(Icons.Default.ViewList, "Recents") {
                    scope.launch { client.pressButton("recents") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Quick Actions", fontWeight = FontWeight.Bold)
                
                ControlButton(Icons.Default.Notifications, "Notifications") {
                    scope.launch { client.pressButton("notifications") }
                }
                ControlButton(Icons.Default.Settings, "Quick Settings") {
                    scope.launch { client.pressButton("quicksettings") }
                }
                ControlButton(Icons.Default.Screenshot, "Screenshot") {
                    scope.launch { client.pressButton("screenshot") }
                }
                ControlButton(Icons.Default.Lock, "Lock Screen", isDestructive = true) {
                    scope.launch { client.pressButton("lock") }
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Touch Pad
            Column(modifier = Modifier.weight(1f)) {
                Text("Touch Pad", fontWeight = FontWeight.Bold)
                Text(
                    "Click to tap • Drag to swipe",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                var dragStart by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val x = (offset.x / size.width) * phoneWidth
                                val y = (offset.y / size.height) * phoneHeight
                                scope.launch { client.sendTap(x, y) }
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> dragStart = offset },
                                onDragEnd = {},
                                onDrag = { change, _ ->
                                    val startX = (dragStart.x / size.width) * phoneWidth
                                    val startY = (dragStart.y / size.height) * phoneHeight
                                    val endX = (change.position.x / size.width) * phoneWidth
                                    val endY = (change.position.y / size.height) * phoneHeight
                                    scope.launch { 
                                        client.sendSwipe(startX, startY, endX, endY) 
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TouchApp,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Touch here to control",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Enable Accessibility Service on your phone:\nSettings → Accessibility → AirTap",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDestructive) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            icon,
            null,
            tint = if (isDestructive) 
                MaterialTheme.colorScheme.onError 
            else 
                MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            color = if (isDestructive) 
                MaterialTheme.colorScheme.onError 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}
