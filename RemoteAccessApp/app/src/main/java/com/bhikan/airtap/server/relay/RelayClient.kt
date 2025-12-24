package com.bhikan.airtap.server.relay

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class RelayMessage(
    val from_device: String,
    val action: String,
    val payload: Map<String, String>,
    val timestamp: Double = 0.0
)

@Serializable
data class ProxyRequest(
    val request_id: String,
    val method: String,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
    val body: String? = null,
    val filename: String? = null,
    val timestamp: Double = 0.0
)

@Serializable
data class ProxyPollResponse(val requests: List<ProxyRequest>)

@Serializable
data class ProxyResponseData(
    val request_id: String,
    val status_code: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String,
    val content_type: String = "application/json"
)

class RelayClient(
    private val context: Context,
    private val serverUrl: String,
    private val email: String,
    private val localServerPort: Int = 8080
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val deviceId: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )
    private val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun register(localIp: String?, port: Int = 8080): Boolean {
        return try {
            val body = """
                {
                    "device_id": "$deviceId",
                    "email": "$email",
                    "device_name": "$deviceName",
                    "local_ip": ${localIp?.let { "\"$it\"" } ?: "null"},
                    "port": $port
                }
            """.trimIndent()
            post("$serverUrl/register", body)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    // Send heartbeat
                    post("$serverUrl/heartbeat/$deviceId", "")

                    // Poll for proxy requests
                    val response = get("$serverUrl/proxy/poll/$deviceId")
                    val pollResponse = json.decodeFromString<ProxyPollResponse>(response)

                    for (req in pollResponse.requests) {
                        launch {
                            handleProxyRequest(req)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(500) // Poll every 500ms for faster response
            }
        }
    }

    private suspend fun handleProxyRequest(req: ProxyRequest) {
        try {
            // Forward request to local Ktor server
            val localUrl = buildString {
                append("http://127.0.0.1:$localServerPort")
                append(req.path)
                if (req.params.isNotEmpty()) {
                    append("?")
                    append(req.params.entries.joinToString("&") { "${it.key}=${it.value}" })
                }
            }

            val conn = URL(localUrl).openConnection() as HttpURLConnection
            conn.requestMethod = req.method
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            // Copy headers (skip host)
            req.headers.forEach { (key, value) ->
                if (!key.equals("host", ignoreCase = true) &&
                    !key.equals("content-length", ignoreCase = true)) {
                    conn.setRequestProperty(key, value)
                }
            }

            // Send body if present
            if (req.body != null && req.method in listOf("POST", "PUT")) {
                conn.doOutput = true
                val bodyBytes = Base64.decode(req.body, Base64.DEFAULT)
                conn.outputStream.write(bodyBytes)
            }

            // Read response
            val statusCode = conn.responseCode
            val contentType = conn.contentType ?: "application/octet-stream"

            val responseBody = try {
                val inputStream = if (statusCode >= 400) conn.errorStream else conn.inputStream
                val buffer = ByteArrayOutputStream()
                inputStream?.copyTo(buffer)
                buffer.toByteArray()
            } catch (e: Exception) {
                ByteArray(0)
            }

            // Get response headers
            val responseHeaders = mutableMapOf<String, String>()
            conn.headerFields.forEach { (key, values) ->
                if (key != null && values.isNotEmpty()) {
                    responseHeaders[key] = values.first()
                }
            }

            // Send response back to relay
            val proxyResponse = ProxyResponseData(
                request_id = req.request_id,
                status_code = statusCode,
                headers = responseHeaders,
                body = Base64.encodeToString(responseBody, Base64.NO_WRAP),
                content_type = contentType
            )

            post("$serverUrl/proxy/respond", json.encodeToString(proxyResponse))

        } catch (e: Exception) {
            e.printStackTrace()
            // Send error response
            val errorResponse = ProxyResponseData(
                request_id = req.request_id,
                status_code = 500,
                body = Base64.encodeToString(
                    """{"error": "${e.message}"}""".toByteArray(),
                    Base64.NO_WRAP
                ),
                content_type = "application/json"
            )
            try {
                post("$serverUrl/proxy/respond", json.encodeToString(errorResponse))
            } catch (_: Exception) {}
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        scope.launch {
            try {
                delete("$serverUrl/unregister/$deviceId")
            } catch (_: Exception) {}
        }
    }

    private fun post(url: String, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.doOutput = true
        if (body.isNotEmpty()) {
            conn.outputStream.write(body.toByteArray())
        }
        return conn.inputStream.bufferedReader().readText()
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        return conn.inputStream.bufferedReader().readText()
    }

    private fun delete(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        return conn.inputStream.bufferedReader().readText()
    }
}
