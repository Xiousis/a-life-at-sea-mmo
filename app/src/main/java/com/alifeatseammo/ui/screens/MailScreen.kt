package com.alifeatseammo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.MailMessage
import com.alifeatseammo.ui.UIActionState
import com.alifeatseammo.ui.components.ActionOverlay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailScreen(
    messages: List<MailMessage>,
    actionState: UIActionState,
    onClaimRewards: (String) -> Unit,
    onDeleteMail: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onSendMail: (String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    val locale = Locale.US
    var showComposeDialog by remember { mutableStateOf<MailMessage?>(null) } // Non-null if replying
    var showNewMailDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mailbox") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewMailDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Compose")
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No messages", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(messages) { message ->
                    MailItem(message, onClaimRewards, onDeleteMail, onMarkAsRead, onReply = { showComposeDialog = it }, locale)
                }
            }
        }
    }

    if (showNewMailDialog) {
        ComposeMailDialog(
            onDismiss = { showNewMailDialog = false },
            onSend = { recipient, subject, body ->
                onSendMail(recipient, subject, body)
                showNewMailDialog = false
            }
        )
    }

    showComposeDialog?.let { replyTo ->
        ComposeMailDialog(
            recipientId = replyTo.senderId,
            initialSubject = "Re: ${replyTo.subject}",
            onDismiss = { showComposeDialog = null },
            onSend = { recipient, subject, body ->
                onSendMail(recipient, subject, body)
                showComposeDialog = null
            }
        )
    }
    ActionOverlay(actionState)
}

@Composable
fun ComposeMailDialog(
    recipientId: String = "",
    initialSubject: String = "",
    onDismiss: () -> Unit,
    onSend: (String, String, String) -> Unit
) {
    var recipient by remember { mutableStateOf(recipientId) }
    var subject by remember { mutableStateOf(initialSubject) }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recipientId.isEmpty()) "New Message" else "Reply") },
        text = {
            Column {
                if (recipientId.isEmpty()) {
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("Recipient ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(recipient, subject, body) },
                enabled = recipient.isNotBlank() && subject.isNotBlank() && body.isNotBlank()
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MailItem(
    message: MailMessage,
    onClaimRewards: (String) -> Unit,
    onDeleteMail: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onReply: (MailMessage) -> Unit,
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
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onReply(message) }) {
                    Text("Reply", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { onDeleteMail(message.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
            
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
        }
    }
}
