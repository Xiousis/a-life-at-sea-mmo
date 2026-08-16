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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Technique
import com.alifeatseammo.data.model.ElementType
import com.alifeatseammo.data.model.TechniqueRegistry
import com.alifeatseammo.data.model.StatType
import com.alifeatseammo.ui.components.getElementColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    character: Character,
    allTechniques: List<Technique>,
    onBackClick: () -> Unit
) {
    var selectedTechnique by remember { mutableStateOf<Technique?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learned Techniques") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (character.learnedTechniques.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You haven't learned any techniques yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val restrictedTypes = character.mythicArt?.restrictedSkillTypes ?: emptyList()
                val combatSkills = listOf(
                    StatType.Swordsmanship, StatType.Brawling, StatType.Gunslinging,
                    StatType.Spear, StatType.MartialArts, StatType.Sniper, StatType.MysticArts
                )

                val availableTechniques = character.learnedTechniques.filter { techId ->
                    val techType = TechniqueRegistry.getTypeFor(techId)
                    val isRestricted = techType != null && techType in restrictedTypes
                    if (isRestricted) return@filter false
                    
                    val mythicArt = character.mythicArt ?: return@filter true
                    
                    // If the Mythic Art doesn't focus on a combat skill, don't restrict combat techniques
                    if (mythicArt.multipliedSkill !in combatSkills) return@filter true
                    
                    // If it DOES focus on a combat skill, only show matching ones or explicitly granted ones
                    (techType == mythicArt.multipliedSkill) || (techId in mythicArt.techniques) || (techType == null)
                }

                items(availableTechniques) { techniqueId ->
                    val baseTech = allTechniques.find { it.id == techniqueId } ?: Technique(id = techniqueId, name = techniqueId.replace("_", " ").uppercase())
                    // Override element with Mythic Art element if present
                    val techInfo = if (character.mythicArt?.elements?.isNotEmpty() == true) {
                        baseTech.copy(element = character.mythicArt.elements.first())
                    } else {
                        baseTech
                    }
                    TechniqueItem(techInfo, onClick = { selectedTechnique = techInfo })
                }
            }
        }

        selectedTechnique?.let { tech ->
            AlertDialog(
                onDismissRequest = { selectedTechnique = null },
                title = { Text(tech.name.uppercase(), fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        if (tech.description.isNotEmpty()) {
                            Text(text = tech.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Text(text = "Stat Type: ${tech.type}", style = MaterialTheme.typography.labelMedium)
                        val costLabel = if (character.mythicArt != null) "Mythic Mana Cost" else "Energy Cost"
                        Text(text = "$costLabel: ${tech.energyCost}", style = MaterialTheme.typography.labelMedium)
                        if (tech.cooldown > 0) {
                            Text(text = "Cooldown: ${tech.cooldown} turns", style = MaterialTheme.typography.labelMedium)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val elementColor = getElementColor(tech.element)
                        Text(
                            text = "Element: ${tech.element ?: "Physical"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = elementColor
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTechnique = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun TechniqueItem(technique: Technique, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📜", modifier = Modifier.padding(end = 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${technique.name} ${technique.element?.symbol ?: ""}".trim().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = technique.element?.let { "${it.symbol} $it DMG" } ?: "Physical DMG",
                    style = MaterialTheme.typography.bodySmall,
                    color = getElementColor(technique.element)
                )
            }
            Text(
                text = "INFO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

