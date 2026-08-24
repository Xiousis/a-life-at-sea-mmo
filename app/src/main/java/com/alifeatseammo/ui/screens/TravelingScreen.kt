package com.alifeatseammo.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.R
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.LocationDef
import com.alifeatseammo.data.model.TravelEvent
import com.alifeatseammo.util.MusicManager
import kotlinx.coroutines.delay

import java.util.Locale

@Composable
fun TravelingScreen(
    character: Character,
    locations: List<LocationDef> = emptyList(),
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

    val shipOffset by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "ShipProgress"
    )

    val destinationLoc = locations.find { it.name == travelState.destination }
    val weather = destinationLoc?.weather ?: "Clear"
    
    val bgColor by animateColorAsState(
        targetValue = when (weather.lowercase()) {
            "stormy", "thunderstorm" -> Color(0xFF2C3E50).copy(alpha = 0.1f)
            "rainy" -> Color(0xFF34495E).copy(alpha = 0.1f)
            "foggy", "misty" -> Color(0xFFBDC3C7).copy(alpha = 0.1f)
            "clear", "sunny" -> Color(0xFFF1C40F).copy(alpha = 0.05f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "WeatherBG"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.BottomStart) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val shipX = maxWidth * shipOffset
                Text(
                    text = "⛵", 
                    fontSize = 60.sp,
                    modifier = Modifier.offset(x = shipX - 30.dp) // Adjust for center of emoji roughly
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Voyaging to ${travelState.destination}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when(weather.lowercase()) {
                    "stormy", "thunderstorm" -> "⛈️"
                    "rainy" -> "🌧️"
                    "foggy", "misty" -> "🌫️"
                    "clear", "sunny" -> "☀️"
                    else -> "🌊"
                },
                fontSize = 24.sp
            )
        }
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

            val allMessages = mutableListOf<TravelEvent>()
            travelState.eventMessage?.let { 
                allMessages.add(TravelEvent(message = it, timestamp = travelState.startTime)) 
            }
            allMessages.addAll(travelState.events)

            // Show events that happened at or before current time
            val visibleEvents = allMessages.filter { it.timestamp <= currentTime }
                .sortedByDescending { it.timestamp }

            visibleEvents.forEach { event ->
                val icon = when {
                    event.message.contains("storm", ignoreCase = true) -> "⛈️"
                    event.message.contains("merchant", ignoreCase = true) || event.message.contains("convoy", ignoreCase = true) -> "🚢"
                    event.message.contains("kraken", ignoreCase = true) || event.message.contains("monster", ignoreCase = true) -> "🐙"
                    event.message.contains("wreck", ignoreCase = true) -> "🏚️"
                    event.message.contains("mist", ignoreCase = true) || event.message.contains("fog", ignoreCase = true) -> "🌫️"
                    else -> "📝"
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = event.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
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
