package com.bhikan.airtap.data.repository

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import com.bhikan.airtap.data.model.SmsConversation
import com.bhikan.airtap.data.model.SmsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository @Inject constructor(
    private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun getConversations(limit: Int = 50): List<SmsConversation> = withContext(Dispatchers.IO) {
        val conversations = mutableMapOf<Long, SmsConversation>()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: continue
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ))

                if (!conversations.containsKey(threadId)) {
                    val contactName = getContactName(address)
                    conversations[threadId] = SmsConversation(
                        threadId = threadId,
                        address = address,
                        contactName = contactName,
                        lastMessage = body,
                        lastTimestamp = date,
                        unreadCount = if (read == 0) 1 else 0,
                        messageCount = 1
                    )
                } else {
                    val existing = conversations[threadId]!!
                    conversations[threadId] = existing.copy(
                        messageCount = existing.messageCount + 1,
                        unreadCount = existing.unreadCount + if (read == 0) 1 else 0
                    )
                }

                if (conversations.size >= limit) break
            }
        }

        conversations.values.sortedByDescending { it.lastTimestamp }
    }

    suspend fun getMessages(threadId: Long, limit: Int = 100): List<SmsItem> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsItem>()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                messages.add(
                    SmsItem(
                        id = id,
                        threadId = threadId,
                        address = address,
                        contactName = getContactName(address),
                        body = body,
                        timestamp = date,
                        type = type,
                        isRead = read == 1
                    )
                )
            }
        }

        messages.reversed() // Return in chronological order
    }

    suspend fun searchMessages(query: String, limit: Int = 50): List<SmsItem> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsItem>()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.BODY} LIKE ?",
            arrayOf("%$query%"),
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                messages.add(
                    SmsItem(
                        id = id,
                        threadId = threadId,
                        address = address,
                        contactName = getContactName(address),
                        body = body,
                        timestamp = date,
                        type = type,
                        isRead = read == 1
                    )
                )
            }
        }

        messages
    }

    @Suppress("DEPRECATION")
    suspend fun sendSms(address: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }

            val sentIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent("SMS_SENT"),
                PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent("SMS_DELIVERED"),
                PendingIntent.FLAG_IMMUTABLE
            )

            // Split message if too long
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(address, null, message, sentIntent, deliveredIntent)
            } else {
                val sentIntents = ArrayList<PendingIntent>().apply {
                    repeat(parts.size) { add(sentIntent) }
                }
                val deliveredIntents = ArrayList<PendingIntent>().apply {
                    repeat(parts.size) { add(deliveredIntent) }
                }
                smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, deliveredIntents)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString())
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
