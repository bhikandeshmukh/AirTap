package com.bhikan.airtap.server

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bhikan.airtap.R
import com.bhikan.airtap.AirTapApp
import com.bhikan.airtap.data.repository.UserRepository
import com.bhikan.airtap.server.auth.AuthManager
import com.bhikan.airtap.server.relay.RelayClient
import com.bhikan.airtap.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebServerService : Service() {

    @Inject
    lateinit var webServer: WebServer

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var userRepository: UserRepository

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var relayClient: RelayClient? = null

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState

    data class ServerState(
        val isRunning: Boolean = false,
        val port: Int = 8080,
        val localIp: String = "",
        val relayConnected: Boolean = false
    )

    inner class LocalBinder : Binder() {
        fun getService(): WebServerService = this@WebServerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    private fun startServer() {
        if (webServer.isRunning) return

        val port = authManager.serverPort
        val localIp = getLocalIpAddress()

        webServer.start(port)

        _serverState.value = ServerState(
            isRunning = true,
            port = port,
            localIp = localIp
        )

        // Update Firestore - device is online
        userRepository.updateDeviceStatus(true, localIp, port)

        // Start relay client for remote access
        startRelayClient(localIp, port)

        startForeground(NOTIFICATION_ID, createNotification(localIp, port))
    }

    private fun startRelayClient(localIp: String, port: Int) {
        val email = authManager.userEmail ?: return
        val relayUrl = RELAY_SERVER_URL

        relayClient = RelayClient(
            context = this,
            serverUrl = relayUrl,
            email = email,
            localServerPort = port
        )

        serviceScope.launch {
            val registered = relayClient?.register(localIp, port) ?: false
            if (registered) {
                relayClient?.startPolling()
                _serverState.value = _serverState.value.copy(relayConnected = true)
            }
        }
    }

    private fun stopServer() {
        webServer.stop()
        relayClient?.stopPolling()
        relayClient = null
        _serverState.value = _serverState.value.copy(isRunning = false, relayConnected = false)
        
        // Update Firestore - device is offline
        userRepository.updateDeviceStatus(false, "", 0)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(ip: String, port: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AirTapApp.CHANNEL_ID)
            .setContentTitle("AirTap Server")
            .setContentText("Running at http://$ip:$port")
            .setSmallIcon(R.drawable.ic_server)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    override fun onDestroy() {
        webServer.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.bhikan.airtap.START_SERVER"
        const val ACTION_STOP = "com.bhikan.airtap.STOP_SERVER"
        const val NOTIFICATION_ID = 1001
        
        // TODO: Update this after deploying to Vercel
        const val RELAY_SERVER_URL = com.bhikan.airtap.Config.RELAY_SERVER_URL
    }
}
