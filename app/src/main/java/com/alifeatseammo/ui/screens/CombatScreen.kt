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
import com.alifeatseammo.ui.components.StatusBar

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
                            val isRestricted = techType != null && techType in restrictedTypes
                            if (isRestricted) return@filter false
                            
                            val mythicArt = character.mythicArt ?: return@filter true
                            
                            val combatSkills = listOf(
                                StatType.Swordsmanship, StatType.Brawling, StatType.Gunslinging,
                                StatType.Spear, StatType.MartialArts, StatType.Sniper, StatType.MysticArts
                            )
                            
                            // If the Mythic Art doesn't focus on a combat skill, don't restrict combat techniques
                            if (mythicArt.multipliedSkill !in combatSkills) return@filter true
                            
                            // If it DOES focus on a combat skill, only show matching ones or explicitly granted ones
                            (techType == mythicArt.multipliedSkill) || (techId in mythicArt.techniques) || (techType == null)
                        }
                        
                        if (availableTechniques.isEmpty()) {
                            Text("No techniques available with your current Mythic Art focus.", color = MaterialTheme.colorScheme.error)
                        } else {
                            availableTechniques.forEach { techId ->
                                Button(
                                    onClick = {
                                        onActionClick(CombatAction.Technique, techId, null)
                                        showTechniques = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    val symbol = character.mythicArt?.element?.symbol ?: ""
                                    Text("$techId $symbol".trim().uppercase())
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

    if (combatState.isFinished) {
        if (combatState.playerWon) {
            VictoryScreen(
                enemyName = enemy.name,
                xpEarned = combatState.xpEarned,
                goldEarned = combatState.goldEarned,
                loot = combatState.loot,
                onClose = { onActionClick(CombatAction.Flee, null, null) }
            )
        } else {
            // Basic Defeat handling
            AlertDialog(
                onDismissRequest = { },
                title = { Text("DEFEAT", color = Color.Red) },
                text = { Text("You have been defeated by ${enemy.name}...") },
                confirmButton = {
                    Button(onClick = { onActionClick(CombatAction.Flee, null, null) }) {
                        Text("Retreat")
                    }
                }
            )
        }
    }
}

@Composable
fun VictoryScreen(
    enemyName: String,
    xpEarned: Int,
    goldEarned: Int,
    loot: List<Item>,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "VICTORY",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4CAF50),
                letterSpacing = 8.sp
            )
            
            Text(
                text = "────────────────────────",
                color = Color(0xFF4CAF50).copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "You have defeated",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = enemyName.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RewardCard("XP Gained", "+$xpEarned", Color(0xFF2196F3))
                RewardCard("Gold Earned", "+$goldEarned", Color(0xFFFFC107))
            }

            if (loot.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "LOOT ACQUIRED",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    loot.forEach { item ->
                        Text(
                            text = "• ${item.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = when(item.rarity) {
                                Rarity.Common -> Color.Gray
                                Rarity.Uncommon -> Color(0xFF4CAF50)
                                Rarity.Rare -> Color(0xFF2196F3)
                                Rarity.Epic -> Color(0xFF9C27B0)
                                Rarity.Legendary -> Color(0xFFFF9800)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("CLAIM REWARDS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun RewardCard(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp)
            .width(100.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = color)
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
            StatusBar(
                label = "HP",
                current = hp,
                max = maxHp,
                color = barColor,
                labelOverride = "$percentage%"
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
