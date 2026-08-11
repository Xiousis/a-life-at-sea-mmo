package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Stats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    character: Character,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skills & Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text("Core Attributes", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                AttributeRow("Strength", character.stats.strength)
                AttributeRow("Endurance", character.stats.endurance)
                AttributeRow("Agility", character.stats.agility)
                AttributeRow("Perception", character.stats.perception)
                AttributeRow("Willpower", character.stats.willpower)
                AttributeRow("Luck", character.stats.luck)
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Combat Proficiencies", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                AttributeRow("Swordsmanship", character.stats.swordsmanship)
                AttributeRow("Brawling", character.stats.brawling)
                AttributeRow("Gunslinging", character.stats.gunslinging)
                AttributeRow("Spear Mastery", character.stats.spear)
                AttributeRow("Martial Arts", character.stats.martialArts)
                AttributeRow("Dual Blades", character.stats.dualBlades)
            }
        }
    }
}

@Composable
fun AttributeRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
    LinearProgressIndicator(
        progress = { (value % 100) / 100f },
        modifier = Modifier.fillMaxWidth().height(4.dp)
    )
}
