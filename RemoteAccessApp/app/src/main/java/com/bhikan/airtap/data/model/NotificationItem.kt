package com.bhikan.airtap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isOngoing: Boolean = false,
    val isClearable: Boolean = true,
    val iconBase64: String? = null
)

@Serializable
data class NotificationListResponse(
    val notifications: List<NotificationItem>,
    val count: Int
)

@Serializable
data class NotificationAction(
    val action: String, // "dismiss", "open"
    val notificationId: String
)
