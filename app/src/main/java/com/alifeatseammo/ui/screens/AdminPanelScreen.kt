package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.ui.GameViewModel
import com.alifeatseammo.ui.UIActionState
import com.alifeatseammo.ui.components.ActionOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: GameViewModel,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchPlayers(searchQuery).collectAsState(initial = emptyList())
    val worldVersion by viewModel.worldVersion.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var showTeleportDialog by remember { mutableStateOf<Character?>(null) }
    var showGoldDialog by remember { mutableStateOf<Character?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Players") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAnnouncementDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Send Global Announcement")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Current World Version: ${worldVersion ?: "0 (Not Initialized)"}", style = MaterialTheme.typography.bodyLarge)
            var nextVersionText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = nextVersionText,
                onValueChange = { nextVersionText = it },
                label = { Text("Next World Version") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { 
                    nextVersionText.toIntOrNull()?.let { 
                        viewModel.seedWorld(it) 
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = nextVersionText.toIntOrNull() != null
            ) {
                Text("RE-SEED WORLD DATA")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Search Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn {
                items(searchResults) { player ->
                    PlayerAdminCard(
                        player = player,
                        onTeleport = { showTeleportDialog = player },
                        onAdjustGold = { showGoldDialog = player },
                        onBan = { viewModel.banPlayer(player.id, "Banned by Admin") },
                        onMute = { viewModel.mutePlayer(player.id, "Muted by Admin", 24) }
                    )
                }
            }
        }

        if (actionState is UIActionState.Loading) {
            ActionOverlay(actionState)
        }
    }

    if (showTeleportDialog != null) {
        var location by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTeleportDialog = null },
            title = { Text("Teleport ${showTeleportDialog?.name}") },
            text = {
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Destination") })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.teleportPlayer(showTeleportDialog!!.id, location)
                    showTeleportDialog = null
                }) { Text("Teleport") }
            }
        )
    }

    if (showGoldDialog != null) {
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGoldDialog = null },
            title = { Text("Adjust Gold for ${showGoldDialog?.name}") },
            text = {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (can be negative)") })
            },
            confirmButton = {
                TextButton(onClick = {
                    amount.toIntOrNull()?.let {
                        viewModel.adjustGold(showGoldDialog!!.id, it, "Admin Adjustment")
                    }
                    showGoldDialog = null
                }) { Text("Adjust") }
            }
        )
    }

    if (showAnnouncementDialog) {
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAnnouncementDialog = false },
            title = { Text("Global Announcement") },
            text = {
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.sendGlobalAnnouncement(message)
                    showAnnouncementDialog = false
                }) { Text("Send") }
            }
        )
    }
}

@Composable
fun PlayerAdminCard(
    player: Character,
    onTeleport: () -> Unit,
    onAdjustGold: () -> Unit,
    onBan: () -> Unit,
    onMute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("${player.name} (Lvl ${player.level})", fontWeight = FontWeight.Bold)
            Text("Loc: ${player.currentLocation} | Gold: ${player.gold}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onTeleport) { Text("Tele") }
                TextButton(onClick = onAdjustGold) { Text("Gold") }
                TextButton(onClick = onMute) { Text("Mute") }
                TextButton(onClick = onBan, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Ban") }
            }
        }
    }
}
