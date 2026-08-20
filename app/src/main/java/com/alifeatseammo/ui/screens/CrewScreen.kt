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
import com.alifeatseammo.ui.UIActionState
import com.alifeatseammo.ui.components.ActionOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewScreen(
    character: Character,
    crew: Crew?,
    members: List<Character>,
    invites: List<CrewInvite>,
    actionState: UIActionState,
    onCreateCrew: (String, String) -> Unit,
    onJoinCrew: (String) -> Unit,
    onLeaveCrew: () -> Unit,
    onInviteToCrew: (String) -> Unit,
    onRespondToInvite: (String, Boolean) -> Unit,
    onPromoteMember: (String, String) -> Unit,
    onKickMember: (String) -> Unit,
    onDonateGold: (Int) -> Unit,
    onUpdateSettings: (String, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

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
                        val myRole = crew?.roles?.get(character.id) ?: CrewRole.Member
                        if (myRole == CrewRole.Captain) {
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Text("⚙️")
                            }
                        }
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
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "MEMBERS: ${crew?.members?.size ?: 0} / 20")
                                Text(text = "TOTAL BOUNTY: ${crew?.totalBounty ?: 0} B")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "TREASURY: ${crew?.gold ?: 0} Gold")
                                Button(onClick = { showDonateDialog = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                                    Text("Donate", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    
                    if (crew?.unlockedPerks?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "CREW PERKS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            crew.unlockedPerks.forEach { perk ->
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text(perk.label) },
                                    leadingIcon = { Text("✨") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "MEMBERS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                    
                    members.sortedByDescending { it.id == crew?.captainId }.forEach { member ->
                        val role = crew?.roles?.get(member.id) ?: CrewRole.Member
                        val roleDisplay = if (role == CrewRole.Captain && crew?.faction == com.alifeatseammo.data.model.Faction.Pirate) {
                            "Pirate Captain"
                        } else if (role == CrewRole.CoCaptain) {
                            "Co-Captain"
                        } else {
                            role.name
                        }

                        val onlineStatus = if (member.isOnline) "Online" else "Offline"
                        val statusColor = if (member.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

                        var showRoleMenu by remember { mutableStateOf(false) }

                        ListItem(
                            headlineContent = { 
                                Text(
                                    text = if (member.id == character.id) "${member.name} (You)" else member.name,
                                    fontWeight = if (member.id == character.id) androidx.compose.ui.text.font.FontWeight.Bold else null
                                ) 
                            },
                            supportingContent = { 
                                Column {
                                    Text(text = roleDisplay)
                                    Text(text = onlineStatus, color = statusColor, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            trailingContent = {
                                val myRole = crew?.roles?.get(character.id) ?: CrewRole.Member
                                val canManage = myRole == CrewRole.Captain || myRole == CrewRole.CoCaptain
                                
                                if (canManage && member.id != character.id) {
                                    Box {
                                        IconButton(onClick = { showRoleMenu = true }) {
                                            Text("⚙️")
                                        }
                                        DropdownMenu(
                                            expanded = showRoleMenu,
                                            onDismissRequest = { showRoleMenu = false }
                                        ) {
                                            CrewRole.entries.filter { it != CrewRole.Captain }.forEach { r ->
                                                DropdownMenuItem(
                                                    text = { Text("Set as ${r.name}") },
                                                    onClick = {
                                                        onPromoteMember(member.id, r.name)
                                                        showRoleMenu = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Kick Member", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    onKickMember(member.id)
                                                    showRoleMenu = false
                                                }
                                            )
                                        }
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

    if (showDonateDialog) {
        var amountText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            title = { Text("Donate Gold to Crew") },
            text = {
                Column {
                    Text("Your Gold: ${character.gold}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (amount > 0) {
                        onDonateGold(amount)
                        showDonateDialog = false
                    }
                }) {
                    Text("Donate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSettingsDialog && crew != null) {
        var description by remember { mutableStateOf(crew.description) }
        var isPublic by remember { mutableStateOf(crew.isPublic) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Crew Settings") },
            text = {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                        Text("Public Crew")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateSettings(description, isPublic)
                    showSettingsDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    ActionOverlay(actionState)
}
