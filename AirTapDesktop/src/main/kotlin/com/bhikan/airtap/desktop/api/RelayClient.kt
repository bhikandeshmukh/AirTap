package com.bhikan.airtap.desktop.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class DeviceInfo(
    val device_id: String,
    val email: String,
    val device_name: String,
    val local_ip: String? = null,
    val port: Int = 8080,
    val last_seen: Double = 0.0,
    val online: Boolean = false
)

@Serializable
data class DevicesResponse(val devices: List<DeviceInfo>)

@Serializable
data class RelayMessage(
    val from_device: String,
    val action: String,
    val payload: Map<String, String>,
    val timestamp: Double = 0.0
)

@Serializable
data class PollResponse(val messages: List<RelayMessage>)

@Serializable
data class SendMessage(
    val from_device: String,
    val to_device: String,
    val action: String,
    val payload: Map<String, String>
)

class RelayClient(
    private val serverUrl: String  // Your Vercel URL
) {
    private val desktopId = "desktop-${UUID.randomUUID().toString().take(8)}"
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingResponses = mutableMapOf<String, CompletableDeferred<Map<String, String>>>()
    
    /**
     * Get all devices registered with this email
     */
    suspend fun getDevices(email: String): List<DeviceInfo> {
        return try {
            val response: DevicesResponse = client.get("$serverUrl/devices/$email").body()
            response.devices
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Send command to device and wait for response
     */
    suspend fun sendCommand(
        toDevice: String, 
        action: String, 
        payload: Map<String, String> = emptyMap(),
        timeoutMs: Long = 30000
    ): Map<String, String> {
        val deferred = CompletableDeferred<Map<String, String>>()
        val requestId = "${action}_${System.currentTimeMillis()}"
        pendingResponses[requestId] = deferred
        
        try {
            client.post("$serverUrl/send") {
                contentType(ContentType.Application.Json)
                setBody(SendMessage(
                    from_device = desktopId,
                    to_device = toDevice,
                    action = action,
                    payload = payload + ("request_id" to requestId)
                ))
            }
            
            return withTimeout(timeoutMs) { deferred.await() }
        } finally {
            pendingResponses.remove(requestId)
        }
    }
    
    /**
     * Start polling for responses from devices
     */
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val response: PollResponse = client.get("$serverUrl/poll/$desktopId").body()
                    
                    for (msg in response.messages) {
                        // Match response to pending request
                        val requestId = msg.payload["request_id"]
                        if (requestId != null && pendingResponses.containsKey(requestId)) {
                            pendingResponses[requestId]?.complete(msg.payload)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore polling errors
                }
                delay(1000) // Poll every second
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
    }
    
    fun close() {
        stopPolling()
        scope.cancel()
        client.close()
    }
}
