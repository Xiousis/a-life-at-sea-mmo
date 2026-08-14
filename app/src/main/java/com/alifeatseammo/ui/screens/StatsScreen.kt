package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    character: Character,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Stats") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Core Attributes Section
            item {
                StatSectionHeader("Core Attributes", "👤")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatAttributeRow("Strength", character.stats.strength)
                    StatAttributeRow("Endurance", character.stats.endurance)
                    StatAttributeRow("Agility", character.stats.agility)
                    StatAttributeRow("Perception", character.stats.perception)
                    StatAttributeRow("Willpower", character.stats.willpower)
                    StatAttributeRow("Luck", character.stats.luck)
                }
            }

            // Combat Proficiencies Section
            item {
                StatSectionHeader("Combat Stats", "⚔️")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatAttributeRow("Swordsmanship", character.stats.swordsmanship)
                    StatAttributeRow("Brawling", character.stats.brawling)
                    StatAttributeRow("Gunslinging", character.stats.gunslinging)
                    StatAttributeRow("Spear Mastery", character.stats.spear)
                    StatAttributeRow("Martial Arts", character.stats.martialArts)
                    StatAttributeRow("Sniper", character.stats.sniper)
                    StatAttributeRow("Mystic Arts", character.stats.mysticArts)
                }
            }

            // Professions Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatSectionHeader("Professions", "⚓")
                        Spacer(modifier = Modifier.height(12.dp))
                        StatAttributeRow("Cooking", character.professionStats.cooking)
                        StatAttributeRow("Navigating", character.professionStats.navigating)
                        StatAttributeRow("Treasure Hunting", character.professionStats.treasureHunting)
                        StatAttributeRow("Blacksmith", character.professionStats.blacksmith)
                        StatAttributeRow("Fishing", character.professionStats.fishing)
                        StatAttributeRow("Medical", character.professionStats.medical)
                    }
                }
            }
            
            // Bottom spacer for scrolling comfort
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun StatSectionHeader(title: String, icon: String) {
    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun StatAttributeRow(label: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val progress = (value % 100) / 100f
        LinearProgressIndicator(
            progress = { if (value > 0) progress.coerceAtLeast(0.05f) else 0f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
