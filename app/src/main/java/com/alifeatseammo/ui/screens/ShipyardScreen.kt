package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Ship

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipyardScreen(
    character: Character,
    availableShips: List<Ship>,
    onBuyShip: (Ship) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Island Shipyard") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Your Fleet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Current Ship: ${character.ship.name}", fontWeight = FontWeight.Bold)
                        Text("Speed Multiplier: x${character.ship.speedMultiplier}")
                        Text("Current Gold: ${character.gold}", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            item {
                Text("Available Ships", style = MaterialTheme.typography.titleLarge)
            }
            
            items(availableShips) { ship ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(ship.name) },
                        supportingContent = { 
                            Text("Speed: x${ship.speedMultiplier} | Price: ${ship.price} Gold") 
                        },
                        trailingContent = {
                            Button(
                                onClick = { onBuyShip(ship) },
                                enabled = character.gold >= ship.price && character.ship.id != ship.id
                            ) {
                                Text(if (character.ship.id == ship.id) "Owned" else "Buy")
                            }
                        }
                    )
                }
            }
        }
    }
}
