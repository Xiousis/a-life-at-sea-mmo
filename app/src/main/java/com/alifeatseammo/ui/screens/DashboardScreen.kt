package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.StatType

@Composable
fun DashboardScreen(
    character: Character,
    onTrainClick: (StatType) -> Unit,
    onMissionsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "${character.name} of ${character.originIsland}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Level ${character.level} ${character.style}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoCard("Gold", character.gold.toString(), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            InfoCard("Energy", "${character.energy}/${character.maxEnergy}", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            InfoCard("XP", character.xp.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Stats & Training", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        val statList = listOf(
            "STR" to character.stats.strength to StatType.Strength,
            "END" to character.stats.endurance to StatType.Endurance,
            "AGI" to character.stats.agility to StatType.Agility,
            "PER" to character.stats.perception to StatType.Perception,
            "WIL" to character.stats.willpower to StatType.Willpower,
            "LUK" to character.stats.luck to StatType.Luck
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(statList) { (statInfo, type) ->
                val (label, value) = statInfo
                StatCard(label, value, onClick = { onTrainClick(type) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onMissionsClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Missions")
        }
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun StatCard(label: String, value: Int, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(text = "Train (10 E)", style = MaterialTheme.typography.labelSmall)
        }
    }
}
