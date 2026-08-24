package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Ship

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipyardScreen(
    character: Character,
    availableShips: List<Ship>,
    onBuyShip: (Ship) -> Unit,
    onUpgradeShip: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Buy Ships", "Upgrades")

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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> BuyShipsContent(character, availableShips, onBuyShip)
                1 -> UpgradesContent(character, onUpgradeShip)
            }
        }
    }
}

@Composable
fun BuyShipsContent(
    character: Character,
    availableShips: List<Ship>,
    onBuyShip: (Ship) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    Text("Hull: Lv.${character.ship.upgrades.hullLevel} | Sails: Lv.${character.ship.upgrades.sailLevel} | Cannons: Lv.${character.ship.upgrades.cannonLevel}")
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

@Composable
fun UpgradesContent(
    character: Character,
    onUpgradeShip: (String) -> Unit
) {
    val ship = character.ship
    val upgrades = listOf(
        Triple("hull", "Reinforce Hull", "Increases Ship HP and Defense"),
        Triple("sail", "Improve Sails", "Increases Sailing Speed"),
        Triple("cannon", "Sharpen Cannons", "Increases Ship Attack Power"),
        Triple("rudder", "Refine Rudder", "Improves Maneuverability and Evasion"),
        Triple("storage", "Expand Storage", "Adds +5 Inventory Slots per level"),
        Triple("cabin", "Luxurious Cabin", "Increases HP and Energy Regen while sailing"),
        Triple("figurehead", "Ornate Figurehead", "Increases Luck and Rare Encounter chance")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Ship Upgrades", style = MaterialTheme.typography.titleLarge)
        }

        items(upgrades) { (type, label, desc) ->
            val level = when(type) {
                "hull" -> ship.upgrades.hullLevel
                "sail" -> ship.upgrades.sailLevel
                "cannon" -> ship.upgrades.cannonLevel
                "rudder" -> ship.upgrades.rudderLevel
                "storage" -> ship.upgrades.storageLevel
                "cabin" -> ship.upgrades.cabinLevel
                "figurehead" -> ship.upgrades.figureheadLevel
                else -> 0
            }
            val cost = (level + 1) * 2000 // Sample cost formula
            
            val statDelta = when(type) {
                "hull" -> "HP: ${ship.maxHp + (level * 20)} -> ${ship.maxHp + ((level + 1) * 20)}"
                "sail" -> "Speed: x${String.format(Locale.US, "%.2f", ship.speedMultiplier + (level * 0.05f))} -> x${String.format(Locale.US, "%.2f", ship.speedMultiplier + ((level + 1) * 0.05f))}"
                "cannon" -> "Attack: ${ship.attack + (level * 5)} -> ${ship.attack + ((level + 1) * 5)}"
                "storage" -> "Slots: +${level * 5} -> +${(level + 1) * 5}"
                else -> null
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$label (Lv. $level)")
                            if (statDelta != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statDelta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    supportingContent = { Text(desc) },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${cost}G", style = MaterialTheme.typography.labelSmall)
                            Button(
                                onClick = { onUpgradeShip(type) },
                                enabled = character.gold >= cost
                            ) {
                                Text("Upgrade")
                            }
                        }
                    }
                )
            }
        }
    }
}
