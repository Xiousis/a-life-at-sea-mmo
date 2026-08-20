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
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.StatType

import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionsScreen(
    character: Character,
    actionState: com.alifeatseammo.ui.UIActionState,
    skillFilter: String = "all",
    onTrainClick: (StatType) -> Unit,
    onBackClick: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(character.trainingState) {
        while (character.trainingState != null) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000.milliseconds)
        }
    }

    val trainingEndTime = character.trainingState?.endTime ?: 0
    val remainingMs = (trainingEndTime - currentTime).coerceAtLeast(0)
    val isTraining = character.trainingState != null
    val isActionLoading = actionState is com.alifeatseammo.ui.UIActionState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (skillFilter == "all") "Jobs & Professions" else "$skillFilter Practice") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Work Status", style = MaterialTheme.typography.labelMedium)
                        Text("⚡ ${character.getCurrentEnergy()} / ${character.maxEnergy}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("💰 ${character.gold} Gold", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (isTraining) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Working...", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = String.format(java.util.Locale.US, "%02ds", remainingMs / 1000),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text("⚓", fontSize = 32.sp)
                    }
                }
                if (isTraining) {
                    LinearProgressIndicator(
                        progress = { 1f - (remainingMs.toFloat() / 5000f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select a Profession to Practice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Practicing costs 10 Energy + Gold (increases per point) and takes 5s.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(16.dp))

            val professions = listOf(
                StatType.Cooking to "Prepare meals to restore HP.",
                StatType.Navigating to "Faster travel times.",
                StatType.TreasureHunting to "Better loot from chests.",
                StatType.Blacksmith to "Craft and repair gear.",
                StatType.Fishing to "Catch fish in the open sea.",
                StatType.Medical to "Heal others and perform surgeries."
            ).filter { 
                skillFilter == "all" || it.first.name.equals(skillFilter, ignoreCase = true)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(professions) { (type, desc) ->
                    val canLearn = character.mythicArt?.canLearnNonCombatSkills ?: true
                    val currentVal = getProfessionValue(character, type)
                    val practiceCost = 10 + (currentVal.toInt() * 10)

                    ProfessionRow(
                        label = type.name,
                        value = currentVal,
                        cost = practiceCost,
                        description = desc,
                        canAfford = !isActionLoading && character.getCurrentEnergy() >= 10 && character.gold >= practiceCost && !isTraining && canLearn,
                        onPractice = { onTrainClick(type) },
                        isLockedByMythic = !canLearn
                    )
                }
            }
        }
    }
}

private fun getProfessionValue(character: Character, type: StatType): Double {
    return when(type) {
        StatType.Cooking -> character.professionStats.cooking
        StatType.Navigating -> character.professionStats.navigating
        StatType.TreasureHunting -> character.professionStats.treasureHunting
        StatType.Blacksmith -> character.professionStats.blacksmith
        StatType.Fishing -> character.professionStats.fishing
        StatType.Medical -> character.professionStats.medical
        else -> 0.0
    }
}

@Composable
fun ProfessionRow(
    label: String,
    value: Double,
    cost: Int,
    description: String,
    canAfford: Boolean,
    onPractice: () -> Unit,
    isLockedByMythic: Boolean = false
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (isLockedByMythic) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(text = "Level: ${String.format(java.util.Locale.US, "%.1f", value)} | Cost: $cost Gold", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Button(
                onClick = onPractice,
                enabled = canAfford,
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (isLockedByMythic) "DISABLED" else "PRACTICE")
            }
        }
    }
}
