package com.alifeatseammo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Crew
import com.alifeatseammo.data.model.getXpNeeded
import com.alifeatseammo.data.model.ElementType
import com.alifeatseammo.data.model.RankDefinitions
import com.alifeatseammo.ui.components.getElementColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    character: Character,
    crew: Crew? = null,
    isOwnProfile: Boolean = false,
    canChallenge: Boolean = false,
    onBackClick: () -> Unit,
    onAttackClick: () -> Unit = {},
    onChallengeClick: () -> Unit = {},
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
                    if (bonusStats.strength > 0.0) Text(text = "• Strength: +${"%.1f".format(bonusStats.strength)}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.endurance > 0.0) Text(text = "• Endurance: +${"%.1f".format(bonusStats.endurance)}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.agility > 0.0) Text(text = "• Agility: +${"%.1f".format(bonusStats.agility)}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.perception > 0.0) Text(text = "• Perception: +${"%.1f".format(bonusStats.perception)}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.willpower > 0.0) Text(text = "• Willpower: +${"%.1f".format(bonusStats.willpower)}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.luck > 0.0) Text(text = "• Luck: +${"%.1f".format(bonusStats.luck)}", style = MaterialTheme.typography.bodySmall)
                    if (bonusStats.mysticArts > 0.0) Text(text = "• Mystic Arts: +${"%.1f".format(bonusStats.mysticArts)}", style = MaterialTheme.typography.bodySmall)

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val clipboardManager = LocalClipboardManager.current
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Player ID:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = character.id.take(8) + "...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(character.id)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                ProfileStatRow("Bounty:", String.format(locale, "%,d", character.bounty), color = MaterialTheme.colorScheme.error)
                ProfileStatRow("Faction:", character.faction.name)
                if (character.infamy > 0) {
                    ProfileStatRow("Infamy:", "${character.infamy}/100", color = MaterialTheme.colorScheme.error)
                }
                ProfileStatRow("Crew:", crew?.name ?: "None")
                ProfileStatRow("Rank:", character.rank)
                
                if (isOwnProfile) {
                    val nextRank = RankDefinitions.getNextRank(character)
                    if (nextRank != null) {
                        val requirementMet = character.level >= nextRank.levelRequired
                        ProfileStatRow(
                            label = "Next Rank:",
                            value = "${nextRank.rank} (Lv. ${nextRank.levelRequired})",
                            color = if (requirementMet) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }

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
                    if (canChallenge) {
                        Button(
                            onClick = onChallengeClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("CHALLENGE FOR RANK", fontWeight = FontWeight.Bold)
                        }
                    }
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
