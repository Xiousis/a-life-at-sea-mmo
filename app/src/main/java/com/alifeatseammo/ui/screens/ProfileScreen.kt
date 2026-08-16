package com.alifeatseammo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Crew
import com.alifeatseammo.data.model.getXpNeeded
import com.alifeatseammo.data.model.ElementType
import com.alifeatseammo.ui.components.getElementColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    character: Character,
    crew: Crew? = null,
    isOwnProfile: Boolean = false,
    onBackClick: () -> Unit,
    onAttackClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onViewCrewClick: () -> Unit = {},
    onAddFriendClick: () -> Unit = {}
) {
    val locale = Locale.US
    var showMythicArtDetails by remember { mutableStateOf(false) }

    if (showMythicArtDetails && character.mythicArt != null) {
        val mythicArt = character.mythicArt
        AlertDialog(
            onDismissRequest = { showMythicArtDetails = false },
            title = {
                Text(
                    text = "${mythicArt.name} [${mythicArt.tier}]",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = mythicArt.description, style = MaterialTheme.typography.bodyMedium)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    if (mythicArt.elements.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Elements:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            mythicArt.elements.forEach { element ->
                                Text(
                                    text = "${element.symbol} $element",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = getElementColor(element)
                                )
                            }
                        }
                    }

                    if (mythicArt.elementalWeaknesses.isNotEmpty()) {
                        Text(
                            text = "Weak Against: ${mythicArt.elementalWeaknesses.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(text = "BUFFS:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                    
                    if (mythicArt.skillMultiplier > 1.0f) {
                        Text(text = "• ${mythicArt.multipliedSkill} Buff: +${((mythicArt.skillMultiplier - 1.0f) * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (mythicArt.hugeBuffValue > 0 && mythicArt.hugeBuffType != null) {
                        Text(text = "• ${mythicArt.hugeBuffType}: +${(mythicArt.hugeBuffValue * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (mythicArt.energyRegainMultiplier > 1.0f) {
                        Text(text = "• Energy Regain: x${mythicArt.energyRegainMultiplier}", style = MaterialTheme.typography.bodySmall)
                    }

                    // Stats from bonusStats
                    val bonusStats = mythicArt.bonusStats
                    if (bonusStats.strength > 0) Text(text = "• Strength: +${bonusStats.strength}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.endurance > 0) Text(text = "• Endurance: +${bonusStats.endurance}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.agility > 0) Text(text = "• Agility: +${bonusStats.agility}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.perception > 0) Text(text = "• Perception: +${bonusStats.perception}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.willpower > 0) Text(text = "• Willpower: +${bonusStats.willpower}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.luck > 0) Text(text = "• Luck: +${bonusStats.luck}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.mysticArts > 0) Text(text = "• Mystic Arts: +${bonusStats.mysticArts}", style = MaterialTheme.typography.bodySmall)

                    if (mythicArt.debuffPercentage > 0f || mythicArt.restrictedSkillTypes.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "DEBUFFS / RESTRICTIONS:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        
                        if (mythicArt.debuffPercentage > 0f) {
                            Text(text = "• Global Power Debuff: -${(mythicArt.debuffPercentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        if (mythicArt.restrictedSkillTypes.isNotEmpty()) {
                            Text(text = "• Disables Skills: ${mythicArt.restrictedSkillTypes.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (mythicArt.techniques.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "GRANTED TECHNIQUES:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(text = mythicArt.techniques.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMythicArtDetails = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "☠ ${character.name.uppercase()}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Level ${character.level}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            // XP Bar
            val xpNeeded = character.getXpNeeded()
            val progress = character.xp.toFloat() / xpNeeded.toFloat()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    text = "${character.xp} / $xpNeeded XP",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = character.race.name,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatRow("Bounty:", String.format(locale, "%,d", character.bounty), color = MaterialTheme.colorScheme.error)
                ProfileStatRow("Faction:", character.faction.name)
                if (character.infamy > 0) {
                    ProfileStatRow("Infamy:", "${character.infamy}/100", color = MaterialTheme.colorScheme.error)
                }
                ProfileStatRow("Crew:", crew?.name ?: "None")
                ProfileStatRow("Rank:", character.rank)
                if (character.title.isNotEmpty()) {
                    ProfileStatRow("Title:", character.title)
                }
                ProfileStatRow(
                    label = "Mystic Art:",
                    value = character.mythicArt?.name ?: "None",
                    color = if (character.mythicArt != null) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = if (character.mythicArt != null) Modifier.clickable { showMythicArtDetails = true } else Modifier
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ProfileStatRow("PvP:", "${character.pvpWins}W / ${character.pvpLosses}L")
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "EQUIPMENT:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                val equippedNames = character.equipment.values.filterNotNull().joinToString { it.name }
                Text(
                    text = if (equippedNames.isNotEmpty()) equippedNames else "No items equipped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (!isOwnProfile) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAttackClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("ATTACK", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onMessageClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("MESSAGE", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onViewCrewClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall,
                        enabled = character.crewId != null
                    ) {
                        Text("VIEW CREW", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAddFriendClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("ADD FRIEND", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
