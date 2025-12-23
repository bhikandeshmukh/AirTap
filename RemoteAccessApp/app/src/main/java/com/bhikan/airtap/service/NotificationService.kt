package com.bhikan.airtap.service

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import com.bhikan.airtap.data.model.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class NotificationService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        val notifications: StateFlow<List<NotificationItem>> = _notifications

        private val _newNotification = MutableStateFlow<NotificationItem?>(null)
        val newNotification: StateFlow<NotificationItem?> = _newNotification

        private var instance: NotificationService? = null

        fun getInstance(): NotificationService? = instance

        fun dismissNotification(key: String) {
            instance?.cancelNotification(key)
        }

        fun dismissAllNotifications() {
            instance?.cancelAllNotifications()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val item = convertToNotificationItem(sbn)
        if (item != null) {
            _newNotification.value = item
            refreshNotifications()
            // Broadcast to WebSocket clients
            CoroutineScope(Dispatchers.IO).launch {
                com.bhikan.airtap.server.websocket.WebSocketHandler.broadcastNotification(item)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refreshNotifications()
    }

    fun refreshNotifications() {
        try {
            val notifications = activeNotifications
            val activeList = notifications
                ?.mapNotNull { sbn -> convertToNotificationItem(sbn) }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()

            _notifications.value = activeList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNotificationsList(): List<NotificationItem> {
        return _notifications.value
    }

    private fun convertToNotificationItem(sbn: StatusBarNotification): NotificationItem? {
        return try {
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() 
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() 
                ?: ""

            // Skip empty notifications
            if (title.isEmpty() && text.isEmpty()) return null

            // Skip system notifications from certain packages
            val skipPackages = listOf(
                "com.android.systemui",
                "com.bhikan.airtap" // Skip our own notifications
            )
            if (sbn.packageName in skipPackages) return null

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                sbn.packageName
            }

            val iconBase64 = try {
                getAppIcon(sbn.packageName)?.let { encodeIconToBase64(it) }
            } catch (e: Exception) {
                null
            }

            NotificationItem(
                id = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                timestamp = sbn.postTime,
                isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
                isClearable = sbn.isClearable,
                iconBase64 = iconBase64
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun encodeIconToBase64(drawable: Drawable): String? {
        return try {
            val bitmap = drawableToBitmap(drawable, 48)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
