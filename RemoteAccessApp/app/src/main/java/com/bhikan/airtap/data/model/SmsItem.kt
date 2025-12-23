package com.bhikan.airtap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SmsItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val timestamp: Long,
    val type: Int, // 1 = inbox, 2 = sent
    val isRead: Boolean
)

@Serializable
data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val messageCount: Int
)

@Serializable
data class SmsListResponse(
    val conversations: List<SmsConversation>,
    val count: Int
)

@Serializable
data class SmsThreadResponse(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val messages: List<SmsItem>
)

@Serializable
data class SendSmsRequest(
    val address: String,
    val message: String
)

@Serializable
data class SendSmsResponse(
    val success: Boolean,
    val message: String
)
