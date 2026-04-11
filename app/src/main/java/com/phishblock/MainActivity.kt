package com.phishblock

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
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

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning to the app
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
    
    val isNotificationListenerEnabled = remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    // Update state when window gets focus (e.g. returning from settings)
    DisposableEffect(Unit) {
        onDispose {}
    }

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
            if (selectedTab == 0 && !isNotificationListenerEnabled.value) {
                ExtendedFloatingActionButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("Enable Protection")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (!isNotificationListenerEnabled.value && selectedTab == 0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Protection is disabled. Please enable Notification Access to scan messages.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(onClick = {
                            isNotificationListenerEnabled.value = isNotificationServiceEnabled(context)
                        }) {
                            Text("Check")
                        }
                    }
                }
            }

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

fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!TextUtils.isEmpty(flat)) {
        val names = flat.split(":").toTypedArray()
        for (i in names.indices) {
            val cn = ComponentName.unflattenFromString(names[i])
            if (cn != null) {
                if (TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
    }
    return false
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
    val risk = msg.riskScore
    val (statusLabel, statusColor, containerColor) = when {
        risk >= 50 -> Triple(
            "🚨 Phishing",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer
        )
        risk >= 20 -> Triple(
            "⚠️ Suspicious",
            Color(0xFFF57C00), // Orange
            Color(0xFFFFF3E0)  // Light Orange
        )
        else -> Triple(
            "✅ Safe",
            Color(0xFF2E7D32), // Green
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = msg.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$statusLabel (risk $risk%)",
                color = statusColor,
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(msg.timestamp),
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