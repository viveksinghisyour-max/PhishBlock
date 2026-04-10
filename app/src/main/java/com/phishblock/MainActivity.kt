package com.phishblock

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phishblock.models.Message
import com.phishblock.ui.theme.PhishBlockTheme
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            PhishBlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecurityApp()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(application))
    val messages by viewModel.messages.observeAsState(emptyList())
    val news by viewModel.news.observeAsState(emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PhishBlock") },
                actions = {
                    IconButton(onClick = { viewModel.addDemoMessages() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Demo")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) {
                    Text("Enable")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Messages", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Awareness", modifier = Modifier.padding(16.dp))
                }
            }
            when (selectedTab) {
                0 -> MessageList(messages)
                1 -> NewsList(news)
            }
        }
    }
}

@Composable
fun MessageList(messages: List<Message>) {
    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No messages yet.\nTap the refresh icon for demo messages.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn {
            items(messages) { msg ->
                MessageCard(msg)
            }
        }
    }
}

@Composable
fun MessageCard(msg: Message) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (msg.isMalicious) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = msg.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            if (msg.isMalicious) {
                Text(
                    text = "⚠️ Risk: ${msg.riskScore}% - ${msg.reason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "✅ Safe (risk ${msg.riskScore}%)",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(msg.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun NewsList(news: List<NewsItem>) {
    if (news.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading news...")
        }
    } else {
        LazyColumn {
            items(news) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleSmall)
                        Text(text = item.pubDate, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}