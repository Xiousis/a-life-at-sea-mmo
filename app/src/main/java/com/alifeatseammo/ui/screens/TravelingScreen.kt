package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

import java.util.Locale

@Composable
fun TravelingScreen(
    character: Character,
    onCompleteClick: () -> Unit = {}
) {
    val locale = Locale.US
    val travelState = character.travelState ?: return
    
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isFinished = currentTime >= travelState.arrivalTime

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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
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
            text = String.format(locale, "Arrival in %02d:%02d", remainingSec / 60, remainingSec % 60),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        if (travelState.events.isNotEmpty() || travelState.eventMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Voyage Log",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val allMessages = mutableListOf<String>()
            travelState.eventMessage?.let { allMessages.add(it) }
            allMessages.addAll(travelState.events.map { it.message })

            allMessages.reversed().forEach { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

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
