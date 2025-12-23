package com.bhikan.airtap.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.bhikan.airtap.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository(
    private val context: Context,
    private val deviceRepository: DeviceRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airtap_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId

    init {
        loadUser()
    }

    private fun loadUser() {
        val email = prefs.getString(KEY_EMAIL, null)
        val deviceName = prefs.getString(KEY_DEVICE_NAME, null)
        val createdAt = prefs.getLong(KEY_CREATED_AT, 0)
        val storedDeviceId = prefs.getString(KEY_DEVICE_ID, null)

        if (email != null) {
            _currentUser.value = User(
                email = email,
                deviceName = deviceName ?: getDefaultDeviceName(),
                createdAt = createdAt
            )
            _isLoggedIn.value = true
            _deviceId.value = storedDeviceId
        }
    }

    fun register(email: String, deviceName: String? = null): User {
        val name = deviceName ?: getDefaultDeviceName()
        val user = User(
            email = email.lowercase().trim(),
            deviceName = name,
            createdAt = System.currentTimeMillis()
        )

        prefs.edit().apply {
            putString(KEY_EMAIL, user.email)
            putString(KEY_DEVICE_NAME, user.deviceName)
            putLong(KEY_CREATED_AT, user.createdAt)
            apply()
        }

        _currentUser.value = user
        _isLoggedIn.value = true

        // Register to Firestore
        scope.launch {
            val result = deviceRepository.registerDevice(user.email, name, 8080)
            result.onSuccess { id ->
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
                _deviceId.value = id
            }
        }

        return user
    }

    fun updateDeviceStatus(isOnline: Boolean, ip: String, port: Int) {
        val id = _deviceId.value ?: return
        scope.launch {
            deviceRepository.updateDeviceStatus(id, isOnline, ip, port)
        }
    }

    fun updateDeviceName(name: String) {
        _currentUser.value?.let { user ->
            val updated = user.copy(deviceName = name)
            prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
            _currentUser.value = updated
        }
    }

    fun logout() {
        // Set offline before logout
        _deviceId.value?.let { id ->
            scope.launch {
                deviceRepository.updateDeviceStatus(id, false, "", 0)
            }
        }
        
        prefs.edit().clear().apply()
        _currentUser.value = null
        _isLoggedIn.value = false
        _deviceId.value = null
    }

    fun getEmail(): String? = _currentUser.value?.email

    fun getDeviceId(): String? = _deviceId.value

    private fun getDefaultDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
