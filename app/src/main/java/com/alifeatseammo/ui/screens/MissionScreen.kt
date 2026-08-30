package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Mission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionScreen(
    character: Character,
    actionState: com.alifeatseammo.ui.UIActionState,
    missions: List<Mission>,
    onMissionClick: (Mission) -> Unit,
    onSetSailClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val isLoading = actionState is com.alifeatseammo.ui.UIActionState.Loading
    
    var currentEnergy by remember(character) { mutableIntStateOf(character.getCurrentEnergy()) }
    LaunchedEffect(character) {
        while (true) {
            currentEnergy = character.getCurrentEnergy()
            kotlinx.coroutines.delay(500) // Slightly faster refresh for smoother UI
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Available Missions", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "⚡ Energy: $currentEnergy / ${character.maxEnergy}", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("Back", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (missions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏜️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No missions available right now.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Check back later or explore other islands.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(missions) { mission ->
                    val isLevelOk = character.level >= mission.minLevel
                    val isEnergyOk = currentEnergy >= mission.energyCost
                    val isLocationOk = mission.locationId.isEmpty() || character.currentLocation.equals(mission.locationId, ignoreCase = true)
                    
                    val isLocked = !isLevelOk || !isEnergyOk || !isLocationOk || isLoading
                    
                    MissionItem(
                        mission = mission, 
                        isLocked = isLocked, 
                        isLocationLocked = !isLocationOk,
                        lockReason = when {
                            isLoading -> "Processing..."
                            !isLevelOk -> "Required Level: ${mission.minLevel}"
                            !isEnergyOk -> "Not enough Energy"
                            !isLocationOk -> "Location: ${mission.locationId}"
                            else -> ""
                        },
                        onClick = onMissionClick,
                        onSetSailClick = onSetSailClick
                    )
                }
            }
        }
    }
}

@Composable
fun MissionItem(
    mission: Mission, 
    isLocked: Boolean, 
    isLocationLocked: Boolean,
    lockReason: String, 
    onClick: (Mission) -> Unit,
    onSetSailClick: (String) -> Unit
) {
    val borderColor = if (mission.isRankUp) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderStroke = if (mission.isRankUp) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null

    Card(
        onClick = { if (!isLocked) onClick(mission) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLocked,
        border = borderStroke,
        colors = if (isLocked) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (mission.isRankUp) {
                Text(
                    text = "⭐ RANK UP MISSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOCKED: $lockReason",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isLocationLocked && !mission.locationId.isNullOrEmpty()) {
                        Button(
                            onClick = { onSetSailClick(mission.locationId) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Set Sail", fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Reward: ${mission.goldReward} Gold, ${mission.xpReward} XP", style = MaterialTheme.typography.labelMedium)
                Text(text = "Diff: ${mission.difficulty}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
