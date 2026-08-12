package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import com.alifeatseammo.R
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.util.MusicManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfirmaryScreen(
    character: Character,
    onStartRest: () -> Unit,
    onInstantHeal: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        MusicManager.play(context, R.raw.life_at_sea_menu_sound)
    }

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(character.healingState) {
        while (character.healingState != null) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val healingEndTime = character.healingState?.endTime ?: 0
    val remainingMs = (healingEndTime - currentTime).coerceAtLeast(0)
    val isHealing = character.healingState != null

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🏥", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
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

            Spacer(modifier = Modifier.height(32.dp))

            if (isHealing) {
                Text(text = "Resting...", style = MaterialTheme.typography.titleMedium)
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
                Text(
                    text = "Please wait while the doctor treats your wounds.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Button(
                    onClick = onStartRest,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = character.hp < character.maxHp
                ) {
                    Text("FREE REST (2 MINS)")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onInstantHeal,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = character.hp < character.maxHp && character.gold >= 50
                ) {
                    Text("QUICK TREATMENT (50 GOLD)")
                }
                
                if (character.gold < 50 && character.hp < character.maxHp) {
                    Text(
                        text = "Not enough gold for quick treatment.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
