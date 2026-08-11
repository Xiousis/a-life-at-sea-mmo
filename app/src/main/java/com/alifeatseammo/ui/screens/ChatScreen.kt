package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.repository.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    globalMessages: List<ChatMessage>,
    crewMessages: List<ChatMessage>,
    crewId: String?,
    onSendMessage: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Chat") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Text("Back")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Global", modifier = Modifier.padding(16.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, enabled = crewId != null) {
                        Text("Crew", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        },
        bottomBar = {
            Row(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            val channel = if (selectedTab == 0) "global" else crewId ?: "global"
                            onSendMessage(text, channel)
                            text = ""
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Send")
                }
            }
        }
    ) { padding ->
        val messages = if (selectedTab == 0) globalMessages else crewMessages
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            reverseLayout = true
        ) {
            items(messages) { msg ->
                Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = msg.senderName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text = msg.message)
                    }
                }
            }
        }
    }
}
