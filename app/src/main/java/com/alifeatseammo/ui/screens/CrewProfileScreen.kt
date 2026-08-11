package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Crew
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewProfileScreen(
    crew: Crew?,
    onBackClick: () -> Unit,
    onJoinClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crew Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (crew == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🏴‍☠️ ${crew.name.uppercase()}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text(text = "Level ${crew.level}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = crew.description.ifEmpty { "No description provided." },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Members:", fontWeight = FontWeight.Bold)
                                Text(text = "${crew.members.size} / 20")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Total Bounty:", fontWeight = FontWeight.Bold)
                                Text(text = String.format(Locale.getDefault(), "%,d B", crew.totalBounty), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onJoinClick(crew.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("REQUEST TO JOIN", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "MEMBERS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                    
                    crew.members.forEach { memberId ->
                        val role = crew.roles[memberId] ?: com.alifeatseammo.data.model.CrewRole.Member
                        ListItem(
                            headlineContent = { Text(text = memberId) },
                            supportingContent = { Text(text = role.name) }
                        )
                    }
                }
            }
        }
    }
}
