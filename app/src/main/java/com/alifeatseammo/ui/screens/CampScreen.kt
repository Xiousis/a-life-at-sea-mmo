package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampScreen(
    character: Character,
    onStartRest: () -> Unit,
    onInstantHeal: () -> Unit,
    onBackClick: () -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
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
                    text = String.format("%02d:%02d", (remainingMs / 60000), (remainingMs % 60000) / 1000),
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
                        enabled = character.hp < character.maxHp
                    ) {
                        Text("REST")
                    }
                    
                    OutlinedButton(
                        onClick = onInstantHeal,
                        modifier = Modifier.weight(1f),
                        enabled = character.hp < character.maxHp && character.gold >= 50
                    ) {
                        Text("USE SUPPLIES (50G)")
                    }
                }
            }

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
