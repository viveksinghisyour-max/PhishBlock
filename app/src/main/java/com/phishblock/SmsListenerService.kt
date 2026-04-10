package com.phishblock

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.phishblock.models.Message

class SmsListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "SmsListenerService"
        private val _newMessage = MutableLiveData<Message?>()
        val newMessage: LiveData<Message?> = _newMessage
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Ignore notifications from our own app to avoid loops
        if (sbn.packageName == packageName) return

        val notification = sbn.notification
        val extras = notification.extras
        
        // Extract basic info
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        // Log for debugging (useful to see which apps are being triggered)
        Log.d(TAG, "Notification received from: ${sbn.packageName}")

        // Improved filtering logic
        val isMessageCategory = notification.category == Notification.CATEGORY_MESSAGE
        val isMessagingApp = sbn.packageName.contains("mms", ignoreCase = true) || 
                             sbn.packageName.contains("sms", ignoreCase = true) ||
                             sbn.packageName.contains("messaging", ignoreCase = true) ||
                             sbn.packageName.contains("whatsapp", ignoreCase = true) ||
                             sbn.packageName.contains("telegram", ignoreCase = true)

        val likelySms = title?.contains("SMS", ignoreCase = true) == true || 
                        subText?.contains("SMS", ignoreCase = true) == true

        if (isMessageCategory || isMessagingApp || likelySms) {
            val messageContent = text ?: return
            
            // Avoid duplicate processing if the same notification is updated frequently
            // (Optional: implement a hashing mechanism if needed)
            
            Log.i(TAG, "Processing message: $messageContent")
            _newMessage.postValue(Message(
                text = messageContent, 
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Handle if needed
    }
}