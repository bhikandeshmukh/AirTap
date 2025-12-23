package com.bhikan.airtap.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.bhikan.airtap.data.model.SmsItem
import com.bhikan.airtap.server.websocket.WebSocketHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            messages?.forEach { smsMessage ->
                val smsItem = SmsItem(
                    id = System.currentTimeMillis(),
                    threadId = 0,
                    address = smsMessage.displayOriginatingAddress ?: "",
                    contactName = null,
                    body = smsMessage.displayMessageBody ?: "",
                    timestamp = smsMessage.timestampMillis,
                    type = 1, // Inbox
                    isRead = false
                )

                // Broadcast to WebSocket clients
                CoroutineScope(Dispatchers.IO).launch {
                    WebSocketHandler.broadcastSms(smsItem)
                }
            }
        }
    }
}
