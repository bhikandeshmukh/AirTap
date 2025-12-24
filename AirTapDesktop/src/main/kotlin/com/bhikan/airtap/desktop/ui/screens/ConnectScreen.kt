package com.bhikan.airtap.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhikan.airtap.desktop.Config
import com.bhikan.airtap.desktop.api.DeviceInfo
import com.bhikan.airtap.desktop.api.FirestoreClient
import com.bhikan.airtap.desktop.api.RelayClient
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(
    onConnect: (url: String, email: String, onResult: (Result<Unit>) -> Unit) -> Unit,
    onConnectViaRelay: (deviceId: String, email: String, onResult: (Result<Unit>) -> Unit) -> Unit = { _, _, _ -> },
    onShowDeviceList: () -> Unit = {}
) {
    var serverUrl by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var useRelay by remember { mutableStateOf(false) }
    var relayDevices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var isLoadingDevices by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<DeviceInfo?>(null) }
    
    val scope = rememberCoroutineScope()
    val relayClient = remember { RelayClient(Config.RELAY_SERVER_URL) }
    
    // Check if superadmin
    val isSuperAdmin = remember(email) { 
        FirestoreClient.isSuperAdmin(email) 
    }
    
    // Load devices when relay mode is enabled and email is entered
    LaunchedEffect(useRelay, email) {
        if (useRelay && email.isNotBlank() && email.contains("@")) {
            isLoadingDevices = true
            relayDevices = relayClient.getDevices(email.trim().lowercase())
            isLoadingDevices = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(450.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "AirTap Desktop",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Connect to your Android phone",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Connection Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !useRelay,
                        onClick = { useRelay = false; selectedDevice = null },
                        label = { Text("Local Network") },
                        leadingIcon = if (!useRelay) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = useRelay,
                        onClick = { useRelay = true },
                        label = { Text("Remote (Relay)") },
                        leadingIcon = if (useRelay) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (same as phone)") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSuperAdmin) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Superadmin",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!useRelay) {
                    // Local Network Mode - Manual IP entry
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Phone IP Address") },
                        placeholder = { Text("http://192.168.1.100:8080") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    // Relay Mode - Show discovered devices
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Your Devices", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                if (isLoadingDevices) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                } else {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                isLoadingDevices = true
                                                relayDevices = relayClient.getDevices(email.trim().lowercase())
                                                isLoadingDevices = false
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (relayDevices.isEmpty() && !isLoadingDevices) {
                                Text(
                                    text = if (email.isBlank()) "Enter your email to find devices" 
                                           else "No devices found. Make sure AirTap is running on your phone.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                relayDevices.forEach { device ->
                                    Card(
                                        onClick = { selectedDevice = device },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedDevice == device) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (device.online) Icons.Default.PhoneAndroid else Icons.Default.PhonelinkOff,
                                                null,
                                                tint = if (device.online) MaterialTheme.colorScheme.primary 
                                                       else MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(device.device_name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                                Text(
                                                    if (device.online) "Online" else "Offline",
                                                    fontSize = 11.sp,
                                                    color = if (device.online) MaterialTheme.colorScheme.primary 
                                                            else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            if (selectedDevice == device) {
                                                Icon(Icons.Default.CheckCircle, null, 
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Superadmin badge
                if (isSuperAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Superadmin - Access all devices",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        errorMessage = null
                        
                        if (email.isBlank()) {
                            errorMessage = "Please enter your email"
                            return@Button
                        }
                        
                        if (useRelay) {
                            // Relay mode - connect via relay proxy
                            if (selectedDevice == null) {
                                errorMessage = "Please select a device"
                                return@Button
                            }
                            if (!selectedDevice!!.online) {
                                errorMessage = "Selected device is offline"
                                return@Button
                            }
                            
                            isConnecting = true
                            val device = selectedDevice!!
                            
                            onConnectViaRelay(device.device_id, email.trim().lowercase()) { result ->
                                isConnecting = false
                                result.onFailure { error ->
                                    errorMessage = error.message ?: "Connection failed"
                                }
                            }
                        } else {
                            // Local network mode
                            if (serverUrl.isBlank()) {
                                errorMessage = "Please enter IP address"
                                return@Button
                            }
                            
                            isConnecting = true
                            val finalUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                                "http://$serverUrl"
                            } else serverUrl

                            onConnect(finalUrl, email.trim().lowercase()) { result ->
                                isConnecting = false
                                result.onFailure { error ->
                                    errorMessage = error.message ?: "Connection failed"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isConnecting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(if (useRelay) Icons.Default.Cloud else Icons.Default.Wifi, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (useRelay) "Connect via Relay" else "Connect", fontSize = 16.sp)
                    }
                }
                
                // Superadmin - View all devices button
                if (isSuperAdmin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onShowDeviceList,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Devices, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View All Devices", fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // How to connect
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (useRelay) "Remote Access:" else "Local Network:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (useRelay) 
                                "1. Open AirTap on your phone\n2. Start the server\n3. Your device will appear above\n4. Select and connect!"
                            else 
                                "1. Open AirTap on your phone\n2. Start the server\n3. Enter the IP shown on phone\n4. Use the same email you registered",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        relayClient.startPolling()
        onDispose { relayClient.stopPolling() }
    }
}
