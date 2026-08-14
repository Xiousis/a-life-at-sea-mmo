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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.StatType
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    character: Character,
    actionState: com.alifeatseammo.ui.UIActionState,
    onTrainClick: (StatType) -> Unit,
    onBackClick: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(character.trainingState) {
        while (character.trainingState != null) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000.milliseconds)
        }
    }

    val trainingEndTime = character.trainingState?.endTime ?: 0
    val remainingMs = (trainingEndTime - currentTime).coerceAtLeast(0)
    val isTraining = character.trainingState != null
    val isActionLoading = actionState is com.alifeatseammo.ui.UIActionState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training & Professions") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Energy Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Status", style = MaterialTheme.typography.labelMedium)
                        Text("⚡ ${character.getCurrentEnergy()} / ${character.maxEnergy}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("💰 ${character.gold} Gold", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (isTraining) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (remainingMs > 0) "Training..." else "Completing...",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = String.format(Locale.US, "%02ds", remainingMs / 1000),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = if (remainingMs > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text("🥋", fontSize = 32.sp)
                    }
                }
                if (isTraining) {
                    LinearProgressIndicator(
                        progress = { 1f - (remainingMs.toFloat() / 20000f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Attribute to Train", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Each session costs 10 Energy + 50 Gold and takes 20s.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(16.dp))

            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("Attributes", "Combat")

            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val stats = when (selectedTab) {
                0 -> listOf(
                    StatType.Strength to "Physical power and damage.",
                    StatType.Endurance to "Health and defense.",
                    StatType.Agility to "Speed and dodge chance.",
                    StatType.Perception to "Accuracy and critical hits.",
                    StatType.Willpower to "Energy recovery and resistance.",
                    StatType.Luck to "Loot find and critical chance."
                )
                else -> {
                    val combatSkills = mutableListOf<Pair<StatType, String>>()
                    val loc = character.currentLocation
                    
                    val townsWithDojo = setOf(
                        "Fogi Tail Island",
                        "Ironcrest Isle",
                        "Tortuga Bay",
                        "Navy Outpost Aqua",
                        "Navy Outpost Terra",
                        "Navy Outpost Ignis",
                        "Island of World Secrets"
                    )

                    if (townsWithDojo.contains(loc)) {
                        combatSkills.add(StatType.Swordsmanship to "Mastery of the blade.")
                        combatSkills.add(StatType.Brawling to "Unarmed combat skills.")
                        combatSkills.add(StatType.Gunslinging to "Pistols and revolvers.")
                        combatSkills.add(StatType.Spear to "Polearms and spears.")
                        combatSkills.add(StatType.MartialArts to "Advanced fighting techniques.")
                        combatSkills.add(StatType.Sniper to "Long-range precision.")
                    }
                    
                    combatSkills
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats) { (type, desc) ->
                    val mythicArt = character.mythicArt
                    var buffText = ""
                    var debuffText = ""

                    if (mythicArt != null) {
                        // Global Debuff (Attributes only)
                        val attributes = setOf(StatType.Strength, StatType.Endurance, StatType.Agility, StatType.Perception, StatType.Willpower, StatType.Luck)
                        if (attributes.contains(type) && mythicArt.debuffPercentage > 0f) {
                            debuffText = "-${(mythicArt.debuffPercentage * 100).toInt()}%"
                        }

                        // Specific Skill Buffs
                        if (mythicArt.multipliedSkill == type && mythicArt.skillMultiplier > 1.0f) {
                            val percent = ((mythicArt.skillMultiplier - 1.0f) * 100).toInt()
                            buffText = "+$percent%"
                        }

                        // Huge Buff
                        if (mythicArt.hugeBuffType == type && mythicArt.hugeBuffValue > 0f) {
                            val percent = (mythicArt.hugeBuffValue * 100).toInt()
                            buffText = if (buffText.isEmpty()) "+$percent%" else "$buffText / +$percent%"
                        }
                        
                        // Restrictions
                        if (mythicArt.restrictedSkillTypes.contains(type)) {
                            debuffText = "DISABLED"
                        }
                    }

                    TrainingRow(
                        label = type.name,
                        value = getStatValue(character, type),
                        description = desc,
                        buffText = buffText,
                        debuffText = debuffText,
                        canAfford = !isActionLoading && character.getCurrentEnergy() >= 10 && character.gold >= 50 && !isTraining && 
                                   !character.mythicArt?.restrictedSkillTypes?.contains(type).let { it ?: false } &&
                                   (type in setOf(StatType.Strength, StatType.Endurance, StatType.Agility, StatType.Perception, StatType.Willpower, StatType.Luck, StatType.Swordsmanship, StatType.Brawling, StatType.Gunslinging, StatType.Spear, StatType.MartialArts, StatType.Sniper, StatType.MysticArts) || (character.mythicArt?.canLearnNonCombatSkills ?: true)),
                        onTrain = { onTrainClick(type) },
                        isLockedByMythic = !(character.mythicArt?.canLearnNonCombatSkills ?: true) && type in setOf(StatType.Cooking, StatType.Navigating, StatType.TreasureHunting, StatType.Blacksmith, StatType.Fishing, StatType.Medical)
                    )
                }
            }
        }
    }
}

@Composable
fun TrainingRow(
    label: String,
    value: Int,
    description: String,
    buffText: String = "",
    debuffText: String = "",
    canAfford: Boolean,
    onTrain: () -> Unit,
    isLockedByMythic: Boolean = false
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (isLockedByMythic) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (buffText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buffText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (debuffText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = debuffText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(text = "Current: $value", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Button(
                onClick = onTrain,
                enabled = canAfford,
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (debuffText == "DISABLED" || isLockedByMythic) "LOCKED" else "TRAIN")
            }
        }
    }
}

fun getStatValue(character: Character, type: StatType): Int {
    return when(type) {
        StatType.Strength -> character.stats.strength
        StatType.Endurance -> character.stats.endurance
        StatType.Agility -> character.stats.agility
        StatType.Perception -> character.stats.perception
        StatType.Willpower -> character.stats.willpower
        StatType.Luck -> character.stats.luck
        StatType.Swordsmanship -> character.stats.swordsmanship
        StatType.Brawling -> character.stats.brawling
        StatType.Gunslinging -> character.stats.gunslinging
        StatType.Spear -> character.stats.spear
        StatType.MartialArts -> character.stats.martialArts
        StatType.Sniper -> character.stats.sniper
        StatType.MysticArts -> character.stats.mysticArts
        StatType.Cooking -> character.professionStats.cooking
        StatType.Navigating -> character.professionStats.navigating
        StatType.TreasureHunting -> character.professionStats.treasureHunting
        StatType.Blacksmith -> character.professionStats.blacksmith
        StatType.Fishing -> character.professionStats.fishing
        StatType.Medical -> character.professionStats.medical
    }
}

@Preview(showBackground = true)
@Composable
fun TrainingScreenPreview() {
    TrainingScreen(
        character = com.alifeatseammo.data.model.Character(name = "Test Pirate"),
        actionState = com.alifeatseammo.ui.UIActionState.Idle,
        onTrainClick = {},
        onBackClick = {}
    )
}
