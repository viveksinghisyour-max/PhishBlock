package com.phishblock

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.phishblock.models.Message
import com.phishblock.models.ModelHelper
import kotlinx.coroutines.*
import java.util.regex.Pattern

class SmsListenerService : NotificationListenerService() {

    private lateinit var modelHelper: ModelHelper
    private lateinit var safeBrowsingHelper: SafeBrowsingHelper
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val TAG = "SmsListenerService"
        private val _newMessage = MutableLiveData<Message?>()
        val newMessage: LiveData<Message?> = _newMessage

        // URL Regex pattern
        private val URL_PATTERN = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                    + "(([\\w\\-]+\\.){1,}\\w+)"
                    + "(:\\d+)?(\\/[\\w\\-./?%&=]*)?",
            Pattern.CASE_INSENSITIVE
        )
    }

    override fun onCreate() {
        super.onCreate()
        modelHelper = ModelHelper(this)
        // Initialize with your actual API key
        safeBrowsingHelper = SafeBrowsingHelper("YOUR_SAFE_BROWSING_API_KEY")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Ignore notifications from our own app
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val fullContent = "$title $text"
        val urls = extractUrls(fullContent)

        if (urls.isNotEmpty()) {
            Log.d(TAG, "Extracted URLs: $urls")
            
            serviceScope.launch {
                for (url in urls) {
                    // 1. Get ML prediction
                    val mlResult = modelHelper.predict(url)
                    
                    // 2. Call SafeBrowsingHelper
                    val isUnsafeApi = safeBrowsingHelper.isUrlUnsafe(url)
                    
                    Log.d(TAG, "URL: $url | ML: ${mlResult.classification} | SafeBrowsing: $isUnsafeApi")

                    if (mlResult.classification == "phishing" || isUnsafeApi) {
                        val reason = if (isUnsafeApi) "Safe Browsing Flag" else "ML Detection"
                        Log.w(TAG, "⚠️ PHISHING DETECTED ($reason): $url")
                        
                        // Trigger Alert
                        Toast.makeText(
                            applicationContext,
                            "⚠️ Phishing Link Detected!\n$url",
                            Toast.LENGTH_LONG
                        ).show()

                        // Post to LiveData for UI updates
                        _newMessage.postValue(Message(
                            text = "⚠️ Phishing detected in notification from ${sbn.packageName}: $url ($reason)",
                            timestamp = System.currentTimeMillis()
                        ))
                        break
                    }
                }
            }
        }
    }

    private fun extractUrls(text: String): Set<String> {
        val urls = mutableSetOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            val url = matcher.group().trim().trim('(', ')', '.', ',', ';')
            if (url.isNotEmpty()) {
                urls.add(url)
            }
        }
        return urls
    }

    override fun onDestroy() {
        modelHelper.close()
        serviceScope.cancel()
        super.onDestroy()
    }
}
