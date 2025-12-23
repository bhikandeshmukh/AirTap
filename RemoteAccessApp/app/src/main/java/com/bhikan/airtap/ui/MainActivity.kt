package com.bhikan.airtap.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhikan.airtap.data.repository.UserRepository
import com.bhikan.airtap.server.WebServerService
import com.bhikan.airtap.server.auth.AuthManager
import com.bhikan.airtap.ui.navigation.Screen
import com.bhikan.airtap.ui.screens.MainScreen
import com.bhikan.airtap.ui.screens.SettingsScreen
import com.bhikan.airtap.ui.screens.SetupScreen
import com.bhikan.airtap.ui.theme.AirTapTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var userRepository: UserRepository

    private var webServerService: WebServerService? = null
    private var serviceBound = mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebServerService.LocalBinder
            webServerService = binder.getService()
            serviceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webServerService = null
            serviceBound.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AirTapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn by userRepository.isLoggedIn.collectAsState()
                    val currentUser by userRepository.currentUser.collectAsState()

                    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Setup.route

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(Screen.Setup.route) {
                            SetupScreen(
                                onSetupComplete = { email, deviceName ->
                                    val user = userRepository.register(email, deviceName)
                                    authManager.userEmail = user.email
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(Screen.Setup.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Main.route) {
                            MainScreen(
                                serverState = webServerService?.serverState?.collectAsState()?.value
                                    ?: WebServerService.ServerState(),
                                onStartServer = { startServer() },
                                onStopServer = { stopServer() },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                onStartScreenMirror = { startScreenMirror() },
                                userEmail = currentUser?.email
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                currentPort = authManager.serverPort,
                                autoStartEnabled = authManager.autoStartEnabled,
                                userEmail = currentUser?.email ?: "",
                                deviceName = currentUser?.deviceName ?: "",
                                onPortChange = { authManager.serverPort = it },
                                onAutoStartChange = { authManager.autoStartEnabled = it },
                                onDeviceNameChange = { userRepository.updateDeviceName(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, WebServerService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound.value) {
            unbindService(serviceConnection)
            serviceBound.value = false
        }
    }

    private fun startServer() {
        val intent = Intent(this, WebServerService::class.java).apply {
            action = WebServerService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopServer() {
        val intent = Intent(this, WebServerService::class.java).apply {
            action = WebServerService.ACTION_STOP
        }
        startService(intent)
    }

    private fun startScreenMirror() {
        startActivity(Intent(this, ScreenCaptureActivity::class.java))
    }
}
