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
import com.bhikan.airtap.desktop.api.FirestoreClient

@Composable
fun ConnectScreen(
    onConnect: (url: String, email: String, onResult: (Result<Unit>) -> Unit) -> Unit,
    onShowDeviceList: () -> Unit = {}
) {
    var serverUrl by remember { mutableStateOf("http://192.168.1.") }
    var email by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // ... (rest of code)

    // Inside Button onClick:
                    onClick = {
                        if (serverUrl.isNotBlank() && email.isNotBlank()) {
                            isConnecting = true
                            errorMessage = null
                            
                            // Ensure URL has scheme
                            val finalUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                                "http://$serverUrl"
                            } else serverUrl

                            onConnect(finalUrl, email.trim().lowercase()) { result ->
                                isConnecting = false
                                result.onFailure { error ->
                                    errorMessage = error.message ?: "Connection failed"
                                }
                            }
                        } else {
                            errorMessage = "Please enter IP address and email"
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
                        Icon(Icons.Default.Wifi, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect", fontSize = 16.sp)
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
                            text = "How to connect:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "1. Open AirTap on your phone\n2. Start the server\n3. Enter the IP shown on phone\n4. Use the same email you registered",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
