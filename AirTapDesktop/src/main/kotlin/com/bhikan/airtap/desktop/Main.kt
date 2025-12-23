package com.bhikan.airtap.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bhikan.airtap.desktop.api.AirTapClient
import com.bhikan.airtap.desktop.api.FirestoreClient
import com.bhikan.airtap.desktop.ui.screens.*
import com.bhikan.airtap.desktop.ui.theme.AirTapDesktopTheme

enum class Screen {
    CONNECT,
    DEVICE_LIST,
    DASHBOARD
}

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "AirTap Desktop",
        state = windowState
    ) {
        AirTapDesktopTheme {
            App()
        }
    }
}

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.CONNECT) }
    var serverUrl by remember { mutableStateOf("") }
    var currentEmail by remember { mutableStateOf("") }
    var token by remember { mutableStateOf<String?>(null) }
    val client = remember { AirTapClient() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            Screen.CONNECT -> {
                ConnectScreen(
                    onConnect = { url, email ->
                        serverUrl = url
                        currentEmail = email
                        client.connect(url, email) { result ->
                            result.onSuccess { 
                                token = it
                                currentScreen = Screen.DASHBOARD
                            }
                        }
                    },
                    onShowDeviceList = {
                        currentScreen = Screen.DEVICE_LIST
                    }
                )
            }
            
            Screen.DEVICE_LIST -> {
                DeviceListScreen(
                    onDeviceSelect = { ip, port ->
                        serverUrl = ip
                        // Auto connect with superadmin email
                        client.connect(ip, FirestoreClient.SUPERADMIN_EMAIL) { result ->
                            result.onSuccess {
                                token = it
                                currentScreen = Screen.DASHBOARD
                            }
                        }
                    },
                    onBack = {
                        currentScreen = Screen.CONNECT
                    }
                )
            }
            
            Screen.DASHBOARD -> {
                MainDashboard(
                    client = client,
                    onDisconnect = {
                        client.disconnect()
                        token = null
                        currentScreen = Screen.CONNECT
                    }
                )
            }
        }
    }
}
