package com.phishblock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.phishblock.models.Message
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _news = MutableLiveData<List<NewsItem>>(emptyList())
    val news: LiveData<List<NewsItem>> = _news

    // Your API key
    private val apiKey = "AlzaSyDJ2dXkAgowXg1aV-PT4bpjemAoXOLHiXw"
    private val safeBrowsingClient = SafeBrowsingClient(apiKey)
    private val scamDetector = ScamDetector()
    private val analyzer = MessageAnalyzer(safeBrowsingClient, scamDetector)

    private val messageObserver = Observer<Message?> { msg ->
        msg?.let {
            viewModelScope.launch {
                val analyzed = analyzer.analyze(it)
                val current = _messages.value?.toMutableList() ?: mutableListOf()
                current.add(0, analyzed)
                _messages.postValue(current)

                if (analyzed.isMalicious) {
                    showNotification(analyzed)
                }
            }
        }
    }

    init {
        // Observe incoming SMS from the service
        SmsListenerService.newMessage.observeForever(messageObserver)
        loadNews()
    }

    fun addDemoMessages() {
        viewModelScope.launch {
            val demos = listOf(
                Message("URGENT: Your bank account is suspended. Verify now: http://fake-bank.com", System.currentTimeMillis()),
                Message("Congratulations! You won $1000. Click here: http://scam-link.xyz", System.currentTimeMillis()),
                Message("Hi, this is a normal message from a friend", System.currentTimeMillis())
            )
            val current = _messages.value?.toMutableList() ?: mutableListOf()
            for (msg in demos) {
                val analyzed = analyzer.analyze(msg)
                current.add(0, analyzed)
            }
            _messages.postValue(current)
        }
    }

    private fun showNotification(msg: Message) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "phish_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "PhishBlock Alerts", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ PhishBlock Alert")
            .setContentText("Suspicious message detected: ${msg.text.take(50)}...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(msg.timestamp.hashCode(), notification)
    }

    private fun loadNews() {
        viewModelScope.launch {
            _news.postValue(NewsRepository.fetchNews())
        }
    }

    override fun onCleared() {
        super.onCleared()
        SmsListenerService.newMessage.removeObserver(messageObserver)
    }
}