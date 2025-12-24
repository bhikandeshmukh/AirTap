package com.bhikan.airtap.desktop.api

import com.bhikan.airtap.desktop.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class AirTapClient {
    private var deviceId: String = ""
    private var useRelay: Boolean = false
    private var directUrl: String = ""
    private var token: String? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(WebSockets)
        engine {
            requestTimeout = 60000
        }
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

    // Get the base URL - either relay proxy or direct
    private fun getBaseUrl(): String {
        return if (useRelay) {
            "${Config.RELAY_SERVER_URL}/proxy/$deviceId"
        } else {
            directUrl
        }
    }

    fun connectViaRelay(deviceId: String, email: String, callback: (Result<String>) -> Unit) {
        this.deviceId = deviceId
        this.useRelay = true
        _connectionState.value = ConnectionState.CONNECTING

        scope.launch {
            try {
                // Login through relay
                val response: LoginResponse = client.post("${getBaseUrl()}/api/login") {
                    setBody(FormDataContent(Parameters.build {
                        append("email", email.lowercase().trim())
                        append("deviceId", java.util.UUID.randomUUID().toString())
                        append("deviceName", "AirTap Desktop")
                    }))
                }.body()

                if (response.success && response.token != null) {
                    token = response.token
                    _connectionState.value = ConnectionState.CONNECTED
                    withContext(Dispatchers.Main) {
                        callback(Result.success(response.token))
                    }
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    withContext(Dispatchers.Main) {
                        callback(Result.failure(Exception("Email not registered on this phone")))
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.DISCONNECTED
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }

    fun connect(url: String, email: String, callback: (Result<String>) -> Unit) {
        val cleanUrl = url.trimEnd('/')
        directUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            "http://$cleanUrl"
        } else {
            cleanUrl
        }
        useRelay = false

        _connectionState.value = ConnectionState.CONNECTING

        scope.launch {
            try {
                val response: LoginResponse = client.post("${getBaseUrl()}/api/login") {
                    setBody(FormDataContent(Parameters.build {
                        append("email", email.lowercase().trim())
                        append("deviceId", java.util.UUID.randomUUID().toString())
                        append("deviceName", "AirTap Desktop")
                    }))
                }.body()

                if (response.success && response.token != null) {
                    token = response.token
                    _connectionState.value = ConnectionState.CONNECTED
                    withContext(Dispatchers.Main) {
                        callback(Result.success(response.token))
                    }
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    withContext(Dispatchers.Main) {
                        callback(Result.failure(Exception("Email not registered on this phone")))
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.DISCONNECTED
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                client.post("${getBaseUrl()}/api/logout") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            } catch (_: Exception) {}
        }
        token = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // Files API
    suspend fun getFiles(path: String = ""): FileListResponse {
        return client.get("${getBaseUrl()}/api/files") {
            parameter("path", path)
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun downloadFile(path: String, destination: File) {
        val response: HttpResponse = client.get("${getBaseUrl()}/api/files/download") {
            parameter("path", path)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        destination.writeBytes(response.readBytes())
    }

    suspend fun uploadFile(path: String, file: File): Boolean {
        return try {
            if (useRelay) {
                // Upload through relay
                client.post("${Config.RELAY_SERVER_URL}/proxy/$deviceId/upload") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(MultiPartFormDataContent(formData {
                        append("path", path)
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }))
                }
            } else {
                client.post("${getBaseUrl()}/api/files/upload") {
                    parameter("path", path)
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(MultiPartFormDataContent(formData {
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }))
                }
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun deleteFile(path: String): Boolean {
        return try {
            client.delete("${getBaseUrl()}/api/files") {
                parameter("path", path)
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            true
        } catch (_: Exception) { false }
    }

    // Notifications API
    suspend fun getNotifications(): NotificationListResponse {
        return client.get("${getBaseUrl()}/api/notifications") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun dismissNotification(id: String) {
        client.post("${getBaseUrl()}/api/notifications/dismiss") {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(FormDataContent(Parameters.build { append("id", id) }))
        }
    }

    // SMS API
    suspend fun getConversations(): SmsListResponse {
        return client.get("${getBaseUrl()}/api/sms/conversations") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun getThread(threadId: Long): SmsThreadResponse {
        return client.get("${getBaseUrl()}/api/sms/thread/$threadId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun sendSms(address: String, message: String): Boolean {
        return try {
            client.post("${getBaseUrl()}/api/sms/send") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(FormDataContent(Parameters.build {
                    append("address", address)
                    append("message", message)
                }))
            }
            true
        } catch (_: Exception) { false }
    }

    // Screen API
    suspend fun getScreenStatus(): ScreenStatus {
        return client.get("${getBaseUrl()}/api/screen/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun getScreenFrame(): ByteArray? {
        return try {
            client.get("${getBaseUrl()}/api/screen/frame") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.readBytes()
        } catch (_: Exception) { null }
    }

    // Control API
    suspend fun getControlStatus(): ControlStatus {
        return client.get("${getBaseUrl()}/api/control/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun sendTap(x: Float, y: Float): Boolean {
        return try {
            client.post("${getBaseUrl()}/api/control/tap") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(FormDataContent(Parameters.build {
                    append("x", x.toString())
                    append("y", y.toString())
                }))
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun sendSwipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        return try {
            client.post("${getBaseUrl()}/api/control/swipe") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(FormDataContent(Parameters.build {
                    append("startX", startX.toString())
                    append("startY", startY.toString())
                    append("endX", endX.toString())
                    append("endY", endY.toString())
                }))
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun pressButton(button: String): Boolean {
        return try {
            client.post("${getBaseUrl()}/api/control/$button") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            true
        } catch (_: Exception) { false }
    }

    fun close() {
        scope.cancel()
        client.close()
    }
}

// Data classes
@Serializable
data class LoginResponse(val token: String? = null, val success: Boolean = false)

@Serializable
data class FileListResponse(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList()
)

@Serializable
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val extension: String = ""
)

@Serializable
data class NotificationListResponse(
    val notifications: List<NotificationItem> = emptyList(),
    val count: Int = 0
)

@Serializable
data class NotificationItem(
    val id: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isClearable: Boolean = true
)

@Serializable
data class SmsListResponse(
    val conversations: List<SmsConversation> = emptyList(),
    val count: Int = 0
)

@Serializable
data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String? = null,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0
)

@Serializable
data class SmsThreadResponse(
    val threadId: Long,
    val address: String,
    val contactName: String? = null,
    val messages: List<SmsMessage> = emptyList()
)

@Serializable
data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val type: Int,
    val isRead: Boolean = true
)

@Serializable
data class ScreenStatus(val streaming: Boolean = false, val message: String = "")

@Serializable
data class ControlStatus(val enabled: Boolean = false, val message: String = "")
