package com.bhikan.airtap.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhikan.airtap.desktop.api.AirTapClient
import com.bhikan.airtap.desktop.api.NotificationItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsTab(client: AirTapClient) {
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadNotifications() {
        scope.launch {
            isLoading = true
            try {
                notifications = client.getNotifications().notifications
            } catch (e: Exception) {
                notifications = emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadNotifications() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { loadNotifications() }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notifications",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onDismiss = {
                            scope.launch {
                                client.dismissNotification(notification.id)
                                loadNotifications()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Notifications,
                null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        notification.appName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        formatTime(notification.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    notification.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    notification.text,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (notification.isClearable) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}
