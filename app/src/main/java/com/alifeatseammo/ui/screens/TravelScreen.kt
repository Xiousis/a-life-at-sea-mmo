package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelScreen(
    character: Character,
    onTravelClick: (String, Long) -> Unit,
    onBackClick: () -> Unit
) {
    val islands = listOf(
        Island("Logue Town", 0),
        Island("Shells Town", 60000), // 1 min
        Island("Orange Town", 120000), // 2 min
        Island("Syrup Village", 300000) // 5 min
    )

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
            
            if (character.travelState != null) {
                TravelTimer(character.travelState.arrivalTime)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(islands.filter { it.name != character.currentLocation }) { island ->
                        OutlinedCard(
                            onClick = { onTravelClick(island.name, System.currentTimeMillis() + island.travelTime) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text(island.name) },
                                supportingContent = { Text("Travel Time: ${island.travelTime / 60000} min") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TravelTimer(arrivalTime: Long) {
    var timeLeft by remember { mutableLongStateOf(arrivalTime - System.currentTimeMillis()) }

    LaunchedEffect(arrivalTime) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft = arrivalTime - System.currentTimeMillis()
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sailing...", style = MaterialTheme.typography.headlineSmall)
            Text("Estimated Arrival: ${timeLeft / 1000}s", style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(
                progress = { 1f - (timeLeft.toFloat() / 60000f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

data class Island(val name: String, val travelTime: Long)
