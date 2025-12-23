package com.bhikan.airtap.desktop.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

object FirestoreClient {
    
    private const val PROJECT_ID = "bhikan-airtap"
    private const val API_KEY = "AIzaSyBHSmiHba9RqnbSFQo0GLp_kGSyoy40Q0o"
    private const val FIRESTORE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"
    
    const val SUPERADMIN_EMAIL = "thebhikandeshmukh@gmail.com"
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
    
    data class DeviceInfo(
        val deviceId: String = "",
        val email: String = "",
        val deviceName: String = "",
        val deviceModel: String = "",
        val lastIp: String = "",
        val lastOnline: Long = 0,
        val serverPort: Int = 8080,
        val isOnline: Boolean = false
    )
    
    suspend fun getAllDevices(): List<DeviceInfo> = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.get("$FIRESTORE_URL/devices?key=$API_KEY").body()
            val documents = response["documents"]?.jsonArray ?: return@withContext emptyList()
            documents.mapNotNull { doc -> parseDeviceDocument(doc.jsonObject) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun parseDeviceDocument(doc: JsonObject): DeviceInfo? {
        return try {
            val name = doc["name"]?.jsonPrimitive?.content ?: return null
            val deviceId = name.substringAfterLast("/")
            val fields = doc["fields"]?.jsonObject ?: return null
            
            DeviceInfo(
                deviceId = deviceId,
                email = fields["email"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "",
                deviceName = fields["deviceName"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "",
                deviceModel = fields["deviceModel"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "",
                lastIp = fields["lastIp"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "",
                lastOnline = fields["lastOnline"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.longOrNull ?: 0,
                serverPort = fields["serverPort"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.intOrNull ?: 8080,
                isOnline = fields["isOnline"]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun isSuperAdmin(email: String): Boolean {
        return email.lowercase().trim() == SUPERADMIN_EMAIL.lowercase()
    }
}
