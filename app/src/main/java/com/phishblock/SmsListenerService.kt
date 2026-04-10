package com.phishblock

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.phishblock.models.Message

class SmsListenerService : NotificationListenerService() {

    companion object {
        private val _newMessage = MutableLiveData<Message?>()
        val newMessage: LiveData<Message?> = _newMessage
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val extras = it.notification.extras
            val title = extras.getString(android.app.Notification.EXTRA_TITLE)
            val text = extras.getString(android.app.Notification.EXTRA_TEXT)

            // Check if the notification is from an SMS/messaging app
            val packageName = it.packageName
            if (packageName.contains("mms") || packageName.contains("sms") ||
                title?.contains("SMS", ignoreCase = true) == true ||
                title?.contains("Message", ignoreCase = true) == true) {
                val messageText = text ?: return
                _newMessage.postValue(Message(messageText, System.currentTimeMillis()))
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}