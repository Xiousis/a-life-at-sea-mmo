package com.alifeatseammo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.*

@Composable
fun CombatScreen(
    character: Character,
    onActionClick: (CombatAction, String?, String?) -> Unit
) {
    val combatState = character.combatState ?: return
    val enemy = combatState.enemy
    val listState = rememberLazyListState()
    
    var showTechniques by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }

    LaunchedEffect(combatState.logs.size) {
        if (combatState.logs.isNotEmpty()) {
            listState.animateScrollToItem(combatState.logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Text(
            text = "BATTLE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Player Section
        CombatantStatus(
            name = "YOU",
            hp = character.hp,
            maxHp = character.maxHp,
            barColor = Color(0xFF4CAF50),
            effects = combatState.playerEffects
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "vs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))

        // Enemy Section
        CombatantStatus(
            name = enemy.name.uppercase(),
            hp = enemy.hp,
            maxHp = enemy.maxHp,
            barColor = Color(0xFFF44336),
            effects = combatState.enemyEffects
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Narrative Log
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                .padding(8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(combatState.logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Grid
        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isPlayerTurn = combatState.playerTurn
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CombatButton("Attack", Modifier.weight(1f), enabled = isPlayerTurn) { onActionClick(CombatAction.Attack, null, null) }
                CombatButton("Technique", Modifier.weight(1f), enabled = isPlayerTurn) { showTechniques = true }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CombatButton("Defend", Modifier.weight(1f), enabled = isPlayerTurn) { onActionClick(CombatAction.Defend, null, null) }
                CombatButton("Item", Modifier.weight(1f), enabled = isPlayerTurn) { showItems = true }
            }
            CombatButton("Flee", Modifier.fillMaxWidth(), enabled = isPlayerTurn) { onActionClick(CombatAction.Flee, null, null) }
        }

        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }

    if (showTechniques) {
        AlertDialog(
            onDismissRequest = { showTechniques = false },
            title = { Text("Choose Technique") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (character.learnedTechniques.isEmpty()) {
                        Text("No techniques learned.")
                    } else {
                        val restrictedTypes = character.mythicArt?.restrictedSkillTypes ?: emptyList()
                        val availableTechniques = character.learnedTechniques.filter { techId ->
                            val techType = TechniqueRegistry.getTypeFor(techId)
                            techType == null || techType !in restrictedTypes
                        }
                        
                        if (availableTechniques.isEmpty()) {
                            Text("All your techniques are restricted by your Mythic Art!", color = MaterialTheme.colorScheme.error)
                        } else {
                            availableTechniques.forEach { techId ->
                                Button(
                                    onClick = {
                                        onActionClick(CombatAction.Technique, techId, null)
                                        showTechniques = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(techId.replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTechniques = false }) { Text("Cancel") }
            }
        )
    }

    if (showItems) {
        AlertDialog(
            onDismissRequest = { showItems = false },
            title = { Text("Use Item") },
            text = {
                val consumables = character.inventory.filter { it.type == ItemType.Consumable }
                Column {
                    if (consumables.isEmpty()) {
                        Text("No consumables available.")
                    } else {
                        consumables.forEach { item ->
                            Button(
                                onClick = {
                                    onActionClick(CombatAction.Item, null, item.id)
                                    showItems = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(item.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showItems = false }) { Text("Cancel") }
            }
        )
    }

    if (combatState.isFinished && combatState.playerWon) {
        AlertDialog(
            onDismissRequest = { }, // Force clicking the button
            title = { 
                Text(
                    "VICTORY", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("You have defeated ${enemy.name}!", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        RewardItem("XP", "+${combatState.xpEarned}", Color(0xFF2196F3))
                        RewardItem("Gold", "+${combatState.goldEarned}", Color(0xFFFFC107))
                    }

                    if (combatState.loot.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Loot Dropped:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        combatState.loot.forEach { item ->
                            Text(
                                text = "• ${item.name} (${item.type})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when(item.rarity) {
                                    Rarity.Common -> Color.LightGray
                                    Rarity.Uncommon -> Color(0xFF4CAF50)
                                    Rarity.Rare -> Color(0xFF2196F3)
                                    Rarity.Epic -> Color(0xFF9C27B0)
                                    Rarity.Legendary -> Color(0xFFFF9800)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onActionClick(CombatAction.Flee, null, null) }, // Using Flee as "Finish/Exit" combat
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Claim & Continue")
                }
            }
        )
    }
}

@Composable
fun RewardItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CombatantStatus(name: String, hp: Int, maxHp: Int, barColor: Color, effects: List<StatusEffect> = emptyList()) {
    val percentage = (hp.toFloat() / maxHp.toFloat() * 100).toInt()
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                effects.forEach { effect ->
                    val color = when (effect.type) {
                        EffectType.Bleed -> Color(0xFFD32F2F)
                        EffectType.Stun -> Color(0xFFFFEB3B)
                        EffectType.Weaken -> Color(0xFF7B1FA2)
                        EffectType.Fortify -> Color(0xFF1976D2)
                        EffectType.Burn -> Color(0xFFF57C00)
                        EffectType.Haste -> Color(0xFF388E3C)
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color, RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = effect.duration.toString(), fontSize = 10.sp, color = if (effect.type == EffectType.Stun) Color.Black else Color.White)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "HP ", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { hp.toFloat() / maxHp.toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CombatButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = PaddingValues(12.dp)
    ) {
        Text(
            text = "[ $label ]",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
