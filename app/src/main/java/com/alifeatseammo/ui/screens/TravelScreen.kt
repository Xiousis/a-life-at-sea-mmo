package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.LocationDef
import com.alifeatseammo.data.model.TravelState
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelScreen(
    character: Character,
    locations: List<LocationDef>,
    onTravelClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Sail") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Current Location: ${character.currentLocation}", style = MaterialTheme.typography.titleMedium)
            Text("Islands Found: ${locations.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            
            if (character.travelState != null) {
                TravelTimer(character.travelState)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(locations.filter { it.name != character.currentLocation }) { location ->
                        OutlinedCard(
                            onClick = { onTravelClick(location.name) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text(location.name) },
                                supportingContent = { Text("Region: ${location.region}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TravelTimer(travel: TravelState) {
    var timeLeft by remember { mutableLongStateOf(travel.arrivalTime - System.currentTimeMillis()) }
    val duration = travel.arrivalTime - travel.startTime

    LaunchedEffect(travel.arrivalTime) {
        while (timeLeft > 0) {
            delay(500)
            timeLeft = travel.arrivalTime - System.currentTimeMillis()
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sailing to ${travel.destination}...", style = MaterialTheme.typography.headlineSmall)
            Text("Estimated Arrival: ${Math.max(0, timeLeft / 1000)}s", style = MaterialTheme.typography.bodyLarge)
            
            val progress = if (duration > 0) {
                1f - (timeLeft.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 1f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
