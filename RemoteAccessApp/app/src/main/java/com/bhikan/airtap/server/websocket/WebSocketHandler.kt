package com.bhikan.airtap.server.websocket

import com.bhikan.airtap.data.model.NotificationItem
import com.bhikan.airtap.data.model.SmsItem
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class WebSocketMessage(
    val type: String,
    val data: String
)

object WebSocketHandler {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<WebSocketMessage>(replay = 0)
    val events: SharedFlow<WebSocketMessage> = _events

    fun addSession(sessionId: String, session: WebSocketSession) {
        sessions[sessionId] = session
    }

    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun getSessionCount(): Int = sessions.size

    suspend fun broadcastNotification(notification: NotificationItem) {
        val message = WebSocketMessage(
            type = "notification",
            data = json.encodeToString(notification)
        )
        broadcast(message)
    }

    suspend fun broadcastSms(sms: SmsItem) {
        val message = WebSocketMessage(
            type = "sms",
            data = json.encodeToString(sms)
        )
        broadcast(message)
    }

    suspend fun broadcastServerStatus(isRunning: Boolean) {
        val message = WebSocketMessage(
            type = "server_status",
            data = if (isRunning) "running" else "stopped"
        )
        broadcast(message)
    }

    private suspend fun broadcast(message: WebSocketMessage) {
        val messageJson = json.encodeToString(message)
        val deadSessions = mutableListOf<String>()

        sessions.forEach { (sessionId, session) ->
            try {
                session.send(Frame.Text(messageJson))
            } catch (e: Exception) {
                deadSessions.add(sessionId)
            }
        }

        // Clean up dead sessions
        deadSessions.forEach { sessions.remove(it) }
    }

    suspend fun sendToSession(sessionId: String, message: WebSocketMessage) {
        val session = sessions[sessionId] ?: return
        try {
            val messageJson = json.encodeToString(message)
            session.send(Frame.Text(messageJson))
        } catch (e: Exception) {
            sessions.remove(sessionId)
        }
    }
}
