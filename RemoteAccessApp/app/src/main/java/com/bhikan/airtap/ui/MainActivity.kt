package com.bhikan.airtap.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkManageStoragePermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            AirTapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val bound by serviceBound
                    val isLoggedIn by userRepository.isLoggedIn.collectAsState()
                    val currentUser by userRepository.currentUser.collectAsState()

                    // Determine start destination based on login state
                    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Setup.route

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(Screen.Setup.route) {
                            SetupScreen(
                                onSetupComplete = { email, deviceName ->
                                    val user = userRepository.register(email, deviceName)
                                    // Store email in AuthManager for verification
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

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // SMS permissions
        permissions.add(Manifest.permission.READ_SMS)
        permissions.add(Manifest.permission.SEND_SMS)
        permissions.add(Manifest.permission.RECEIVE_SMS)
        permissions.add(Manifest.permission.READ_CONTACTS)

        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun checkManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
        // Prompt for notification access
        checkNotificationAccess()
    }

    private fun checkNotificationAccess() {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        if (enabledListeners == null || !enabledListeners.contains(packageName)) {
            // Show dialog to enable notification access
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
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
