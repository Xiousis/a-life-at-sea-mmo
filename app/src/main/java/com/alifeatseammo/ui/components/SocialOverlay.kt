package com.alifeatseammo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.repository.ChatMessage
import com.alifeatseammo.ui.SocialViewModel
import kotlinx.coroutines.launch

@Composable
fun SocialOverlay(
    viewModel: SocialViewModel,
    showFriendsList: Boolean,
    onDismissFriendsList: () -> Unit
) {
    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    var selectedFriendForPM by remember { mutableStateOf<Character?>(null) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    if (showFriendsList) {
        AlertDialog(
            onDismissRequest = onDismissFriendsList,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Social")
                    IconButton(onClick = { showAddFriendDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxHeight(0.7f)) {
                    if (pendingRequests.isNotEmpty()) {
                        Text("Pending Requests", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        LazyColumn {
                            items(pendingRequests) { sender ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sender.name)
                                    Row {
                                        IconButton(onClick = { viewModel.acceptFriendRequest(sender.id) }) {
                                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.Green)
                                        }
                                        IconButton(onClick = { viewModel.declineFriendRequest(sender.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    Text("Friends", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (friends.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No friends yet.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn {
                            items(friends) { friend ->
                                FriendItem(
                                    friend = friend,
                                    onMessageClick = { selectedFriendForPM = friend },
                                    onRemoveClick = { viewModel.removeFriend(friend.id) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissFriendsList) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddFriendDialog) {
        var targetId by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Add Friend") },
            text = {
                TextField(
                    value = targetId,
                    onValueChange = { targetId = it },
                    label = { Text("Player ID") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addFriend(targetId)
                    showAddFriendDialog = false
                    targetId = ""
                }) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedFriendForPM?.let { friend ->
        PrivateMessageDialog(
            friend = friend,
            viewModel = viewModel,
            onDismiss = { selectedFriendForPM = null }
        )
    }
}

@Composable
fun FriendItem(
    friend: Character,
    onMessageClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(friend.name, fontWeight = FontWeight.Bold)
            Text("Lvl ${friend.level} ${friend.race}", style = MaterialTheme.typography.bodySmall)
        }
        Row {
            IconButton(onClick = onMessageClick) {
                Icon(Icons.Default.Chat, contentDescription = "Message")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove Friend", color = Color.Red) },
                        onClick = {
                            onRemoveClick()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

@Composable
fun PrivateMessageDialog(
    friend: Character,
    viewModel: SocialViewModel,
    onDismiss: () -> Unit
) {
    val messages by viewModel.getPrivateMessages(friend.id).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chat with ${friend.name}", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    reverseLayout = false
                ) {
                    items(messages) { msg ->
                        ChatMessageItem(msg)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message...") }
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendPrivateMessage(friend.id, messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(msg: ChatMessage) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(msg.message, style = MaterialTheme.typography.bodyMedium)
    }
}
