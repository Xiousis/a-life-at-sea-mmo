package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.R
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.util.MusicManager
import kotlinx.coroutines.delay

@Composable
fun TravelingScreen(
    character: Character,
    onCompleteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val travelState = character.travelState ?: return
    
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val isFinished = currentTime >= travelState.arrivalTime

    LaunchedEffect(Unit) {
        MusicManager.play(context, R.raw.life_at_sea_menu_sound)
    }

    LaunchedEffect(travelState.arrivalTime) {
        while (true) {
            currentTime = System.currentTimeMillis()
            if (currentTime >= travelState.arrivalTime) {
                // The ViewModel now handles completion authoritatively
                break
            }
            delay(1000)
        }
    }

    val totalDuration = travelState.arrivalTime - travelState.startTime
    val elapsed = currentTime - travelState.startTime
    val progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val remainingSec = ((travelState.arrivalTime - currentTime) / 1000).coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⛵", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Voyaging to ${travelState.destination}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "On your ${character.ship.name}",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = String.format("Arrival in %02d:%02d", remainingSec / 60, remainingSec % 60),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        if (isFinished) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCompleteClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Complete Voyage")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Watch out for monsters and rival pirates...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
