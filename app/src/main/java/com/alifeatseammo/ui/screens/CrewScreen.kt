package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Crew

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewScreen(
    character: Character,
    crew: Crew?,
    onCreateCrew: (String, String) -> Unit,
    onJoinCrew: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crew") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (character.crewId == null) {
                // No Crew - Show Create/Join
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "You are not in a crew.", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    var crewName by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = crewName,
                        onValueChange = { crewName = it },
                        label = { Text("Crew Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onCreateCrew(crewName, "") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = crewName.length >= 3
                    ) {
                        Text("Create Crew (10,000 Gold)")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "OR", style = MaterialTheme.typography.labelLarge)
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
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "MEMBERS: ${crew?.members?.size ?: 0} / 20")
                            Text(text = "TOTAL BOUNTY: ${crew?.totalBounty ?: 0} B")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Members list coming soon...")
                }
            }
        }
    }
}
