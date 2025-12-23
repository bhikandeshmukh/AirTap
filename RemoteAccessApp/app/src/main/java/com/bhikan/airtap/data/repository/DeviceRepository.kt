package com.bhikan.airtap.data.repository

import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val devicesCollection = firestore.collection("devices")
    
    companion object {
        const val SUPERADMIN_EMAIL = "thebhikandeshmukh@gmail.com"
    }
    
    data class DeviceInfo(
        val email: String = "",
        val deviceName: String = "",
        val deviceModel: String = "",
        val lastIp: String = "",
        val lastOnline: Long = 0,
        val serverPort: Int = 8080,
        val isOnline: Boolean = false
    )
    
    // Register device to Firestore
    suspend fun registerDevice(email: String, deviceName: String, port: Int): Result<String> {
        return try {
            val deviceId = generateDeviceId()
            val deviceInfo = hashMapOf(
                "email" to email.lowercase().trim(),
                "deviceName" to deviceName,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "lastIp" to getLocalIpAddress(),
                "lastOnline" to System.currentTimeMillis(),
                "serverPort" to port,
                "isOnline" to true,
                "registeredAt" to System.currentTimeMillis()
            )
            
            devicesCollection.document(deviceId).set(deviceInfo).await()
            Result.success(deviceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update device status (online/offline, IP)
    suspend fun updateDeviceStatus(deviceId: String, isOnline: Boolean, ip: String, port: Int) {
        try {
            val updates = hashMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastIp" to ip,
                "serverPort" to port,
                "lastOnline" to System.currentTimeMillis()
            )
            devicesCollection.document(deviceId).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Get all devices for superadmin
    suspend fun getAllDevices(): List<Pair<String, DeviceInfo>> {
        return try {
            val snapshot = devicesCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val info = doc.toObject(DeviceInfo::class.java)
                if (info != null) doc.id to info else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Get devices by email
    suspend fun getDevicesByEmail(email: String): List<Pair<String, DeviceInfo>> {
        return try {
            val snapshot = devicesCollection
                .whereEqualTo("email", email.lowercase().trim())
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val info = doc.toObject(DeviceInfo::class.java)
                if (info != null) doc.id to info else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Check if email is superadmin
    fun isSuperAdmin(email: String): Boolean {
        return email.lowercase().trim() == SUPERADMIN_EMAIL.lowercase()
    }
    
    // Get device by ID
    suspend fun getDevice(deviceId: String): DeviceInfo? {
        return try {
            val doc = devicesCollection.document(deviceId).get().await()
            doc.toObject(DeviceInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateDeviceId(): String {
        val androidId = Build.FINGERPRINT.hashCode().toString(16)
        val time = System.currentTimeMillis().toString(36)
        return "device_${androidId}_$time"
    }
    
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
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
}
