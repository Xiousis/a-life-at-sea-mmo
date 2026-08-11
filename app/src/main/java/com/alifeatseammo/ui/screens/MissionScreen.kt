package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Faction
import com.alifeatseammo.data.model.Mission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionScreen(
    character: Character,
    missions: List<Mission>,
    onMissionClick: (Mission) -> Unit,
    onBackClick: () -> Unit
) {
    val filteredMissions = missions.filter { 
        it.factionRequirement == Faction.Neutral || it.factionRequirement == character.faction 
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Missions") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredMissions) { mission ->
                val isLevelOk = character.level >= mission.minLevel
                val isEnergyOk = character.getCurrentEnergy() >= mission.energyCost
                val isLocationOk = mission.locationId.isEmpty() || character.currentLocation == mission.locationId
                
                val isLocked = !isLevelOk || !isEnergyOk || !isLocationOk
                val lockReason = when {
                    !isLevelOk -> "Required Level: ${mission.minLevel}"
                    !isEnergyOk -> "Not enough Energy"
                    !isLocationOk -> "Required Location: ${mission.locationId}"
                    else -> ""
                }

                MissionItem(mission, isLocked, lockReason, onMissionClick)
            }
        }
    }
}

@Composable
fun MissionItem(mission: Mission, isLocked: Boolean, lockReason: String, onClick: (Mission) -> Unit) {
    Card(
        onClick = { if (!isLocked) onClick(mission) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLocked,
        colors = if (isLocked) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = mission.title, 
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.Unspecified
                )
                Text(text = "${mission.energyCost} E", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mission.description, style = MaterialTheme.typography.bodyMedium)
            
            if (isLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LOCKED: $lockReason",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Reward: ${mission.goldReward} Gold, ${mission.xpReward} XP", style = MaterialTheme.typography.labelMedium)
                Text(text = "Diff: ${mission.difficulty}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
