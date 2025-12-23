package com.bhikan.airtap.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhikan.airtap.desktop.api.FirestoreClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeviceListScreen(
    onDeviceSelect: (ip: String, port: Int) -> Unit,
    onBack: () -> Unit
) {
    var devices by remember { mutableStateOf<List<FirestoreClient.DeviceInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                devices = FirestoreClient.getAllDevices()
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "All Registered Devices",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                isLoading = true
                scope.launch {
                    devices = FirestoreClient.getAllDevices()
                    isLoading = false
                }
            }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
            }
        } else if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No devices registered yet")
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onClick = {
                            if (device.isOnline && device.lastIp.isNotEmpty()) {
                                onDeviceSelect("http://${device.lastIp}:${device.serverPort}", device.serverPort)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: FirestoreClient.DeviceInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = device.isOnline) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isOnline) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (device.isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Device icon
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Device info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName.ifEmpty { device.deviceModel },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = device.email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = device.deviceModel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // Connection info
            Column(horizontalAlignment = Alignment.End) {
                if (device.isOnline) {
                    Text(
                        text = device.lastIp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Port: ${device.serverPort}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Offline",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatLastOnline(device.lastOnline),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            if (device.isOnline) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Connect",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatLastOnline(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return "Last: ${sdf.format(Date(timestamp))}"
}
