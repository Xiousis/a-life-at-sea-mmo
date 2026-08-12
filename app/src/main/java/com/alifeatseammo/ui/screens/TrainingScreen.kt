package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.alifeatseammo.R
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.StatType
import com.alifeatseammo.util.MusicManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    character: Character,
    onTrainClick: (StatType) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        MusicManager.play(context, R.raw.life_at_sea_menu_sound)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dojo Training") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Energy Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Energy", style = MaterialTheme.typography.labelMedium)
                        Text("${character.getCurrentEnergy()} / ${character.maxEnergy}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("⚡", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Attribute to Train", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Each session costs 10 Energy and grants 1 point.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(16.dp))

            val stats = listOf(
                StatType.Strength to "Physical power and damage.",
                StatType.Endurance to "Health and defense.",
                StatType.Agility to "Speed and dodge chance.",
                StatType.Perception to "Accuracy and critical hits.",
                StatType.Willpower to "Energy recovery and resistance.",
                StatType.Luck to "Loot find and critical chance."
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats) { (type, desc) ->
                    TrainingRow(
                        label = type.name,
                        value = getStatValue(character, type),
                        description = desc,
                        canAfford = character.getCurrentEnergy() >= 10,
                        onTrain = { onTrainClick(type) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrainingRow(
    label: String,
    value: Int,
    description: String,
    canAfford: Boolean,
    onTrain: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Current: $value", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Button(
                onClick = onTrain,
                enabled = canAfford,
                shape = MaterialTheme.shapes.small
            ) {
                Text("TRAIN")
            }
        }
    }
}

fun getStatValue(character: Character, type: StatType): Int {
    return when(type) {
        StatType.Strength -> character.stats.strength
        StatType.Endurance -> character.stats.endurance
        StatType.Agility -> character.stats.agility
        StatType.Perception -> character.stats.perception
        StatType.Willpower -> character.stats.willpower
        StatType.Luck -> character.stats.luck
        else -> 0
    }
}
