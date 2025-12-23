package com.bhikan.airtap.server.auth

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val activeSessions = mutableMapOf<String, SessionInfo>()

    companion object {
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_PORT = "server_port"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_DEVICE_ID = "device_id"
        private const val SESSION_TIMEOUT_MS = 30 * 24 * 60 * 60 * 1000L // 30 days
        const val SUPERADMIN_EMAIL = "thebhikandeshmukh@gmail.com"
    }

    data class SessionInfo(
        val token: String,
        val createdAt: Long,
        val lastAccess: Long,
        val ipAddress: String,
        val deviceId: String? = null
    )

    // Email storage for verification
    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    // Device ID for Firestore
    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    // Validate email match OR superadmin
    fun validateEmail(email: String): Boolean {
        val inputEmail = email.lowercase().trim()
        
        // Superadmin can access any device
        if (inputEmail == SUPERADMIN_EMAIL.lowercase()) {
            return true
        }
        
        // Normal user - check if email matches
        val storedEmail = userEmail ?: return false
        return inputEmail == storedEmail.lowercase().trim()
    }
    
    // Check if email is superadmin
    fun isSuperAdmin(email: String): Boolean {
        return email.lowercase().trim() == SUPERADMIN_EMAIL.lowercase()
    }

    fun createSession(ipAddress: String, deviceId: String? = null): String {
        cleanExpiredSessions()
        val token = generateToken()
        val now = System.currentTimeMillis()
        activeSessions[token] = SessionInfo(
            token = token,
            createdAt = now,
            lastAccess = now,
            ipAddress = ipAddress,
            deviceId = deviceId
        )
        return token
    }

    fun validateSession(token: String?): Boolean {
        if (token.isNullOrEmpty()) return false

        val session = activeSessions[token] ?: return false
        val now = System.currentTimeMillis()

        if (now - session.lastAccess > SESSION_TIMEOUT_MS) {
            activeSessions.remove(token)
            return false
        }

        activeSessions[token] = session.copy(lastAccess = now)
        return true
    }

    fun invalidateSession(token: String) {
        activeSessions.remove(token)
    }

    fun invalidateAllSessions() {
        activeSessions.clear()
    }

    var autoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var serverPort: Int
        get() = prefs.getInt(KEY_PORT, 8080)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun cleanExpiredSessions() {
        val now = System.currentTimeMillis()
        activeSessions.entries.removeIf { (_, session) ->
            now - session.lastAccess > SESSION_TIMEOUT_MS
        }
    }
}
