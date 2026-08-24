package com.alifeatseammo.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.*
import com.alifeatseammo.ui.components.StatusBar
import com.alifeatseammo.ui.components.getElementColor

@Composable
fun CombatScreen(
    character: Character,
    raidBoss: RaidBoss? = null,
    onActionClick: (CombatAction, String?, String?) -> Unit
) {
    val combatState = character.combatState ?: return
    val enemy = combatState.enemy
    val listState = rememberLazyListState()
    
    var showTechniques by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }

    val primaryElementColor = getElementColor(character.mythicArt?.elements?.firstOrNull())


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
        if (combatState.isRankChallenge) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "RANK CHALLENGE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (combatState.isRaid) {
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "WORLD RAID BOSS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            CombatButton(
                label = "Leaderboard",
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Yellow
            ) { showLeaderboard = true }
        }

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
        if (combatState.comboCount > 0) {
            Text(
                text = "COMBO x${combatState.comboCount}",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Player Section
        var currentMp by remember { mutableStateOf(character.getCurrentMythicMana()) }
        LaunchedEffect(character.mythicArt) {
            while (character.mythicArt != null) {
                currentMp = character.getCurrentMythicMana()
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }

        CombatantStatus(
            name = "YOU",
            hp = character.hp,
            maxHp = character.maxHp,
            barColor = Color(0xFF4CAF50),
            effects = combatState.playerEffects,
            elements = character.mythicArt?.elements ?: emptyList()
        )
        
        if (character.mythicArt != null) {
            StatusBar(
                label = "MP",
                current = currentMp,
                max = character.maxMythicMana,
                color = Color(0xFF2196F3)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "vs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))

        // Enemy Section
        CombatantStatus(
            name = if (combatState.isRaid) "WORLD BOSS: ${enemy.name.uppercase()}" else enemy.name.uppercase(),
            hp = enemy.hp,
            maxHp = enemy.maxHp,
            barColor = Color(0xFFF44336),
            effects = combatState.enemyEffects,
            elements = enemy.elements,
            playerArt = character.mythicArt
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
                    val textColor = when {
                        log.contains("CRITICAL", ignoreCase = true) -> Color(0xFFFFC107) // Amber
                        log.contains("DODGED", ignoreCase = true) || log.contains("EVADED", ignoreCase = true) -> Color(0xFF03A9F4) // Light Blue
                        log.contains("WEAKNESS", ignoreCase = true) -> Color(0xFFE91E63) // Pink/Red
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 24.sp,
                        fontWeight = if (textColor != MaterialTheme.colorScheme.onSurface) FontWeight.Bold else FontWeight.Normal
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
                CombatButton(
                    label = "Technique", 
                    modifier = Modifier.weight(1f), 
                    enabled = isPlayerTurn,
                    color = primaryElementColor
                ) { showTechniques = true }
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
                                    val techType = TechniqueRegistry.getTypeFor(techId)
                                    val element = if (character.mythicArt != null && techType == character.mythicArt.multipliedSkill) character.mythicArt.elements.firstOrNull() else null
                                    val elementSymbol = element?.symbol ?: ""
                                    Text("$techId $elementSymbol".trim().uppercase())
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

    if (showLeaderboard && raidBoss != null) {
        AlertDialog(
            onDismissRequest = { showLeaderboard = false },
            title = { Text("RAID LEADERBOARD") },
            text = {
                val sortedParticipants = raidBoss.participants.values.sortedByDescending { it.totalDamage }
                Column {
                    Text(
                        "Only Top 3 damage performers get a 0.1% chance for Exclusive Drops. All participants get standard Gold & XP.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    sortedParticipants.take(10).forEachIndexed { index, participant ->
                        val isSelf = participant.userId == character.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(if (isSelf) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${index + 1}. ${participant.userName.uppercase()}",
                                fontWeight = if (index < 3) FontWeight.Bold else FontWeight.Normal,
                                color = if (index < 3) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${participant.totalDamage} DMG",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelf) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeaderboard = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun CombatantStatus(
    name: String,
    hp: Int,
    maxHp: Int,
    barColor: Color,
    effects: List<StatusEffect> = emptyList(),
    elements: List<ElementType> = emptyList(),
    playerArt: MythicArt? = null
) {
    var previousHp by remember { mutableIntStateOf(hp) }
    var flashActive by remember { mutableStateOf(false) }

    val flashColor by animateColorAsState(
        targetValue = if (flashActive) Color.Red.copy(alpha = 0.4f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "HitFlash",
        finishedListener = { flashActive = false }
    )

    val shakeOffset by animateDpAsState(
        targetValue = if (flashActive) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh),
        label = "Shake"
    )

    LaunchedEffect(hp) {
        if (hp < previousHp) {
            flashActive = true
        }
        previousHp = hp
    }

    val percentage = (hp.toFloat() / maxHp.toFloat() * 100).toInt()
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeOffset)
            .background(flashColor, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            
            // Elements
            elements.forEach { element ->
                val isAdvantage = playerArt?.elements?.any { isEffective(it, element) } == true
                val isWeakness = playerArt?.elementalWeaknesses?.contains(element) == true
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .border(
                            width = 1.dp,
                            color = if (isAdvantage) Color.Green else if (isWeakness) Color.Red else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(2.dp)
                ) {
                    Text(text = element.symbol, fontSize = 14.sp)
                }
            }

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

private fun isEffective(attacker: ElementType, defender: ElementType): Boolean {
    return when (attacker) {
        ElementType.Fire -> defender == ElementType.Ice || defender == ElementType.Air
        ElementType.Water -> defender == ElementType.Fire || defender == ElementType.Earth
        ElementType.Earth -> defender == ElementType.Lightning || defender == ElementType.Air
        ElementType.Air -> defender == ElementType.Earth || defender == ElementType.Fire
        ElementType.Lightning -> defender == ElementType.Water || defender == ElementType.Ice
        ElementType.Ice -> defender == ElementType.Air || defender == ElementType.Water
        ElementType.Light -> defender == ElementType.Dark
        ElementType.Dark -> defender == ElementType.Light
        ElementType.Void -> defender != ElementType.Void && defender != ElementType.Annihilation
        ElementType.Celestial -> defender == ElementType.Dark || defender == ElementType.Chaos
        ElementType.Annihilation -> true // Effective against everything except Creation
        ElementType.Creation -> defender == ElementType.Annihilation
        else -> false
    }
}

@Composable
fun CombatButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Color.Unspecified, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = PaddingValues(12.dp),
        border = if (color != Color.Unspecified) BorderStroke(2.dp, SolidColor(color)) else ButtonDefaults.outlinedButtonBorder(enabled = enabled)
    ) {
        Text(
            text = "[ $label ]",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (color != Color.Unspecified) color else Color.Unspecified
        )
    }
}
