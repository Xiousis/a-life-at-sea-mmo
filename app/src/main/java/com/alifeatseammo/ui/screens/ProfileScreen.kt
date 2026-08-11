package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    character: Character,
    isOwnProfile: Boolean = false,
    onBackClick: () -> Unit,
    onAttackClick: () -> Unit = {},
    onMessageClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "☠ ${character.name.uppercase()}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(text = "Level ${character.level} ${character.race}", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileStatRow("Bounty", "${character.bounty} B", color = MaterialTheme.colorScheme.error)
                    ProfileStatRow("Crew", "Black Tide") // TODO: Fetch crew name
                    ProfileStatRow("Location", character.currentLocation)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "Combat Stats", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val stats = character.stats
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            StatText("STR: ${stats.strength}")
                            StatText("END: ${stats.endurance}")
                            StatText("AGI: ${stats.agility}")
                        }
                        Column {
                            StatText("PER: ${stats.perception}")
                            StatText("WIL: ${stats.willpower}")
                            StatText("LUK: ${stats.luck}")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (!isOwnProfile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onAttackClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("ATTACK")
                    }
                    Button(
                        onClick = onMessageClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("MESSAGE")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
fun StatText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
}
