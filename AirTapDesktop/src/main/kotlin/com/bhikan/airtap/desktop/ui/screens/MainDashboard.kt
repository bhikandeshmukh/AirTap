package com.bhikan.airtap.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bhikan.airtap.desktop.api.AirTapClient

enum class Tab(val title: String, val icon: ImageVector) {
    FILES("Files", Icons.Default.Folder),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    SMS("SMS", Icons.Default.Sms),
    SCREEN("Screen", Icons.Default.ScreenShare),
    CONTROL("Control", Icons.Default.TouchApp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    client: AirTapClient,
    onDisconnect: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(Tab.FILES) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Tab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                    label = { Text(tab.title) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = onDisconnect) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Disconnect",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Main Content
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                Tab.FILES -> FilesTab(client)
                Tab.NOTIFICATIONS -> NotificationsTab(client)
                Tab.SMS -> SmsTab(client)
                Tab.SCREEN -> ScreenTab(client)
                Tab.CONTROL -> ControlTab(client)
            }
        }
    }
}
