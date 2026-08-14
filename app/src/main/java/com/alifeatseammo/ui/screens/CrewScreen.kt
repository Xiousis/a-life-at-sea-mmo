package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Crew
import com.alifeatseammo.data.model.CrewInvite
import com.alifeatseammo.data.model.CrewRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewScreen(
    character: Character,
    crew: Crew?,
    invites: List<CrewInvite>,
    onCreateCrew: (String, String) -> Unit,
    onJoinCrew: (String) -> Unit,
    onLeaveCrew: () -> Unit,
    onInviteToCrew: (String) -> Unit,
    onRespondToInvite: (String, Boolean) -> Unit,
    onPromoteMember: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crew") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                },
                actions = {
                    if (character.crewId != null) {
                        IconButton(onClick = { showInviteDialog = true }) {
                            Text("Invite")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (character.crewId == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    // Invites Section
                    if (invites.isNotEmpty()) {
                        Text(text = "PENDING INVITES", style = MaterialTheme.typography.titleSmall)
                        invites.forEach { invite ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = invite.crewName, modifier = Modifier.weight(1f))
                                    Button(onClick = { onRespondToInvite(invite.crewId, true) }) { Text("Accept") }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(onClick = { onRespondToInvite(invite.crewId, false) }) { Text("Decline") }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Create/Join Section
                    Text(text = "You are not in a crew.", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (character.faction == com.alifeatseammo.data.model.Faction.Neutral) {
                        Text(
                            text = "You must join the Navy or become a Pirate to create a crew.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (character.faction == com.alifeatseammo.data.model.Faction.Navy && character.infamy > 80) {
                        Text(
                            text = "WARNING: High Infamy (${character.infamy}). At 100 you will be kicked from the Navy!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    var crewName by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = crewName,
                        onValueChange = { crewName = it },
                        label = { Text("Crew Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = character.faction != com.alifeatseammo.data.model.Faction.Neutral
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onCreateCrew(crewName, "") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = crewName.length >= 3 && character.faction != com.alifeatseammo.data.model.Faction.Neutral
                    ) {
                        Text("Create Crew (10,000 Gold)")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "OR", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var crewId by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = crewId,
                        onValueChange = { crewId = it },
                        label = { Text("Crew ID to Join") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onJoinCrew(crewId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = crewId.isNotEmpty()
                    ) {
                        Text("Join Crew")
                    }
                }
            } else {
                // In Crew - Show Crew Info
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = crew?.name ?: "Loading...", style = MaterialTheme.typography.headlineLarge)
                    Text(text = "Level ${crew?.level ?: 1}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "MEMBERS: ${crew?.members?.size ?: 0} / 20")
                            Text(text = "TOTAL BOUNTY: ${crew?.totalBounty ?: 0} B")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "MEMBERS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                    
                    crew?.members?.forEach { memberId ->
                        val role = crew.roles[memberId] ?: com.alifeatseammo.data.model.CrewRole.Member
                        val roleDisplay = if (role == com.alifeatseammo.data.model.CrewRole.Captain && crew.faction == com.alifeatseammo.data.model.Faction.Pirate) {
                            "Pirate Captain"
                        } else {
                            role.name
                        }

                        ListItem(
                            headlineContent = { Text(text = memberId) }, // In real app, fetch name
                            supportingContent = { Text(text = roleDisplay) },
                            trailingContent = {
                                if (crew.captainId == character.id && memberId != character.id) {
                                    IconButton(onClick = { onPromoteMember(memberId, "Officer") }) {
                                        Text("↑")
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    if (crew?.captainId != character.id) {
                        Button(
                            onClick = onLeaveCrew,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Leave Crew")
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        var targetId by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Player") },
            text = {
                OutlinedTextField(
                    value = targetId,
                    onValueChange = { targetId = it },
                    label = { Text("Player ID") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onInviteToCrew(targetId)
                    showInviteDialog = false
                }) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
