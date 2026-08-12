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
fun SkillsScreen(
    character: Character,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Abilities & Professions") },
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
                SkillSectionHeader("Core Attributes", "👤")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttributeRow("Strength", character.stats.strength)
                    AttributeRow("Endurance", character.stats.endurance)
                    AttributeRow("Agility", character.stats.agility)
                    AttributeRow("Perception", character.stats.perception)
                    AttributeRow("Willpower", character.stats.willpower)
                    AttributeRow("Luck", character.stats.luck)
                }
            }

            // Combat Proficiencies Section
            item {
                SkillSectionHeader("Combat Skills", "⚔️")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttributeRow("Swordsmanship", character.stats.swordsmanship)
                    AttributeRow("Brawling", character.stats.brawling)
                    AttributeRow("Gunslinging", character.stats.gunslinging)
                    AttributeRow("Spear Mastery", character.stats.spear)
                    AttributeRow("Martial Arts", character.stats.martialArts)
                    AttributeRow("Sniper", character.stats.sniper)
                    AttributeRow("Mystic Arts", character.stats.mysticArts)
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
                        SkillSectionHeader("Professions", "⚓")
                        Spacer(modifier = Modifier.height(12.dp))
                        AttributeRow("Cooking", character.professionStats.cooking)
                        AttributeRow("Navigating", character.professionStats.navigating)
                        AttributeRow("Treasure Hunting", character.professionStats.treasureHunting)
                        AttributeRow("Blacksmith", character.professionStats.blacksmith)
                        AttributeRow("Fishing", character.professionStats.fishing)
                    }
                }
            }
            
            // Bottom spacer for scrolling comfort
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SkillSectionHeader(title: String, icon: String) {
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
fun AttributeRow(label: String, value: Int) {
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
        // Progress bar representing sub-level progress (if applicable) 
        // or just a visual accent. Using value % 100 for visual feedback.
        val progress = (value % 100) / 100f
        LinearProgressIndicator(
            progress = { if (value > 0) progress.coerceAtLeast(0.05f) else 0f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
