package com.alifeatseammo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.MailMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailScreen(
    messages: List<MailMessage>,
    onClaimRewards: (String) -> Unit,
    onDeleteMail: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val locale = Locale.US
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mailbox") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No messages", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(messages) { message ->
                    MailItem(message, onClaimRewards, onDeleteMail, onMarkAsRead, locale)
                }
            }
        }
    }
}

@Composable
fun MailItem(
    message: MailMessage,
    onClaimRewards: (String) -> Unit,
    onDeleteMail: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    locale: Locale
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { if (!message.isRead) onMarkAsRead(message.id) },
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", locale).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message.subject, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message.body, style = MaterialTheme.typography.bodyMedium)
            
            message.rewards?.let { rewards ->
                if (rewards.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onClaimRewards(message.id) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Claim Rewards")
                    }
                }
            }
            
            IconButton(
                onClick = { onDeleteMail(message.id) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
