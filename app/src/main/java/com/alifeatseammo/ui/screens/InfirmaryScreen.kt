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
fun InfirmaryScreen(
    character: Character,
    playersAtLocation: List<Character>,
    onStartRest: () -> Unit,
    onInstantHeal: () -> Unit,
    onPurchaseLicense: () -> Unit,
    onHealPlayer: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
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
                title = { Text("Island Infirmary") },
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
            Text(text = "🏥", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (character.hp <= 0) "You are critically injured!" else "Welcome to the infirmary.",
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
                Text(text = "Resting...", style = MaterialTheme.typography.titleMedium)
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartRest,
                        modifier = Modifier.weight(1f),
                        enabled = character.hp < character.maxHp
                    ) {
                        Text("REST")
                    }
                    
                    OutlinedButton(
                        onClick = onInstantHeal,
                        modifier = Modifier.weight(1f),
                        enabled = character.hp < character.maxHp && character.gold >= 50
                    ) {
                        Text("HEAL (50G)")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text(
                text = "Medical Profession",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            val isZTier = character.mythicArt?.tier == "Z"

            if (isZTier) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Your Mythic Art forbids practicing medicine. You are far beyond such mundane acts.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (!character.hasMedicalLicense) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Become a Doctor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Obtain a medical license to heal other players and earn experience.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onPurchaseLicense,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = character.gold >= 15000
                        ) {
                            Text("BUY LICENSE (15,000 GOLD)")
                        }
                    }
                }
            } else {
                Text(
                    text = "Medical Skill: Level ${character.professionStats.medical}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Patients in Hospital:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                val injuredPlayers = playersAtLocation.filter { it.id != character.id && it.healingState != null }
                
                if (injuredPlayers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No patients currently waiting.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                    Button(onClick = { onHealPlayer(p.id) }) {
                                        Text("HEAL")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
