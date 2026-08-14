package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampScreen(
    character: Character,
    actionState: com.alifeatseammo.ui.UIActionState,
    playersAtLocation: List<Character>,
    onStartRest: () -> Unit,
    onInstantHeal: () -> Unit,
    onHealPlayer: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isActionLoading = actionState is com.alifeatseammo.ui.UIActionState.Loading
    
    val healingEndTime = character.healingState?.endTime ?: 0
    val remainingMs = (healingEndTime - currentTime).coerceAtLeast(0)
    val isHealing = character.healingState != null

    LaunchedEffect(character.healingState) {
        if (isHealing) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wilderness Camp") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "⛺", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (character.hp <= 0) "You are exhausted and injured..." else "A safe place to rest.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (character.hp <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Current HP: ${character.hp} / ${character.maxHp}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isHealing) {
                Text(text = "Sleeping by the fire...", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = String.format(java.util.Locale.US, "%02d:%02d", (remainingMs / 60000), (remainingMs % 60000) / 1000),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black
                )
                LinearProgressIndicator(
                    progress = { 1f - (remainingMs.toFloat() / 120000f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(
                    text = "You can rest here to slowly recover your health, or use some supplies to heal instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartRest,
                        modifier = Modifier.weight(1f),
                        enabled = !isActionLoading && character.hp < character.maxHp
                    ) {
                        Text("REST")
                    }
                    
                    OutlinedButton(
                        onClick = onInstantHeal,
                        modifier = Modifier.weight(1f),
                        enabled = !isActionLoading && character.hp < character.maxHp && character.gold >= 50
                    ) {
                        Text("USE SUPPLIES (50G)")
                    }
                }
            }

            if (character.hasMedicalLicense) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                Text(
                    text = "Medical Profession",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Medical Skill: Level ${character.professionStats.medical}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Injured Travelers at Camp:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                val injuredPlayers = playersAtLocation.filter { it.id != character.id && it.healingState != null }
                
                if (injuredPlayers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No one currently needs help.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(injuredPlayers.size) { index ->
                            val p = injuredPlayers[index]
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = p.name, fontWeight = FontWeight.Bold)
                                        Text(text = "HP: ${p.hp}/${p.maxHp}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(
                                        onClick = { onHealPlayer(p.id) },
                                        enabled = !isActionLoading
                                    ) {
                                        Text("HEAL")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ℹ️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Resting at a camp is essential in dangerous islands where no hospitals are available.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
