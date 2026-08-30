package com.alifeatseammo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import com.alifeatseammo.data.model.*
import com.alifeatseammo.ui.components.*
import com.alifeatseammo.R
import com.alifeatseammo.util.MusicManager

@Composable
fun DashboardScreen(
    character: Character,
    location: LocationDef?,
    playersNearby: List<Character>,
    playerCount: Int,
    missionCount: Int,
    mailCount: Int,
    activeRaids: List<RaidBoss> = emptyList(),
    travelResult: String? = null,
    warState: WarState? = null,
    onActionClick: (ActionType, String?) -> Unit,
    onPlayerClick: (Character) -> Unit,
    onMissionsClick: () -> Unit,
    onQuestsClick: () -> Unit,
    onMailClick: () -> Unit,
    onMapClick: () -> Unit,
    onJoinFaction: (Faction) -> Unit,
    onClearTravelResult: () -> Unit = {}
) {
    if (travelResult != null) {
        AlertDialog(
            onDismissRequest = onClearTravelResult,
            title = { Text("LANDFALL! 🏝️") },
            text = { Text("We've dropped anchor at $travelResult. The crew is awaiting your command, Captain!") },
            confirmButton = {
                Button(onClick = onClearTravelResult) {
                    Text("Steady as she goes!")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle Drifting Background Animation
        val infiniteTransition = rememberInfiniteTransition(label = "BackgroundDrift")
        val driftOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DriftOffset"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        ),
                        start = androidx.compose.ui.geometry.Offset(driftOffset, driftOffset),
                        end = androidx.compose.ui.geometry.Offset(driftOffset + 1000f, driftOffset + 1000f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚓ A LIFE AT SEA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "THE GRAND VOYAGE 🌊",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    BadgedBox(badge = { if (mailCount > 0) Badge { Text(mailCount.toString()) } }) {
                        IconButton(onClick = onMailClick) {
                            Icon(Icons.Default.Email, contentDescription = "Mail", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = warState?.isActive == true,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    warState?.let { WarBanner(it) }
                }
            }

            val activeRaid = activeRaids.firstOrNull { it.status == RaidStatus.Active }
            AnimatedVisibility(
                visible = activeRaid != null,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    activeRaid?.let { RaidAlertBanner(it, onMapClick) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Player Card
            ThemedCard(
                borderColor = if (character.faction == Faction.Navy) Color(0xFF2196F3) else if (character.faction == Faction.Pirate) Color(0xFFE57373) else MaterialTheme.colorScheme.outline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = character.name.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        if (character.title.isNotEmpty()) {
                            Text(
                                text = "« ${character.title} »",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    LevelBadge(character.level)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val factionIcon = when (character.faction) {
                        Faction.Navy -> "⚓"
                        Faction.Pirate -> "🏴‍☠️"
                        else -> "⛵"
                    }
                    Text(
                        text = "$factionIcon ${character.rank} • ${character.race}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val nextRank = RankDefinitions.getNextRank(character)
                if (nextRank != null && character.level >= nextRank.levelRequired) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⭐ PROMOTION READY: ${nextRank.rank}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BOUNTY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "${character.bounty} B",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GOLD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = "💰 ${character.gold}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                StatusBar("HP", character.hp, character.maxHp, Color(0xFFE57373))
                
                var currentEnergy by remember { mutableIntStateOf(character.getCurrentEnergy()) }
                var currentMp by remember { mutableIntStateOf(character.getCurrentMythicMana()) }
                
                LaunchedEffect(character) {
                    while (true) {
                        currentEnergy = character.getCurrentEnergy()
                        currentMp = character.getCurrentMythicMana()
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                StatusBar("ENERGY", currentEnergy, character.maxEnergy, Color(0xFF64B5F6))
                
                if (character.mythicArt != null) {
                    StatusBar("MYTHIC MANA", currentMp, character.maxMythicMana, Color(0xFF9575CD))
                }
                
                if (character.level < 300) {
                    StatusBar("EXP", character.xp, character.getXpNeeded(), Color(0xFF81C784))
                } else {
                    StatusBar("EXP", 1, 1, Color(0xFF81C784), labelOverride = "MAX LEVEL REACHED")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            NauticalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Faction Recruitment
            if (character.faction == Faction.Neutral && location != null) {
                val recruitment = when {
                    location.name.startsWith("Navy Outpost") -> Faction.Navy
                    location.name == "Pirate\u0027s Den" -> Faction.Pirate
                    else -> null
                }

                recruitment?.let { faction ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (faction == Faction.Pirate) "JOIN THE PIRATES?" else "ENLIST IN THE NAVY?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (faction == Faction.Pirate) 
                                    "• Build Bounty\n• Form a Crew\n• Raid islands\n• Attack Marines\n• Become a Yonko-like sea emperor" 
                                    else "• Gain Rank\n• Hunt wanted Pirates\n• Join Marine divisions\n• Capture criminals\n• Become Fleet Admiral",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { onJoinFaction(faction) }) {
                                Text(if (faction == Faction.Pirate) "Join Pirates" else "Enlist Now")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Location Centerpiece
            location?.let {
                ThemedCard(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    borderColor = if (it.isSafe) Color(0xFF81C784) else MaterialTheme.colorScheme.error
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = it.name.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${it.region} • ${if (it.isSafe) "SAFE ZONE" else "DANGER ZONE"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (it.isSafe) Color(0xFF81C784) else MaterialTheme.colorScheme.error
                        )

                        if (it.controlledBy != Faction.Neutral) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (it.controlledBy == Faction.Navy) Color(0xFF2196F3).copy(alpha = 0.2f) else Color(0xFFE57373).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "OWNED BY ${it.controlledBy.name.uppercase()}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (it.controlledBy == Faction.Navy) Color(0xFF2196F3) else Color(0xFFE57373)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "\"${it.description}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfoBit("Weather", it.weather)
                            VerticalDivider(modifier = Modifier.height(20.dp), thickness = 1.dp)
                            InfoBit("Players", playerCount.toString())
                        }
                    }
                }

                if (playersNearby.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "PLAYERS AT THIS LOCATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playersNearby) { other ->
                            if (other.id != character.id) {
                                AssistChip(
                                    onClick = { onPlayerClick(other) },
                                    label = { Text(other.name, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Text("☠") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            NauticalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Mission and Quest Boards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GameFeatureCard(
                    label = "MISSIONS",
                    count = "$missionCount",
                    icon = "⚔",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onMissionsClick,
                    modifier = Modifier.weight(1f)
                )

                GameFeatureCard(
                    label = "QUESTS",
                    count = "Story",
                    icon = "📜",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onQuestsClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Grid Header
            Text(
                text = "ISLAND ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Action Grid
            location?.let { loc ->
                val chunkedActions = loc.actions.chunked(2)
                chunkedActions.forEachIndexed { rowIndex, rowActions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowActions.forEachIndexed { colIndex, action ->
                            val isRestricted = character.mythicArt?.canLearnNonCombatSkills == false && 
                                               isNonCombatAction(action.type)
                            
                            ActionCard(
                                label = if (isRestricted) "RESTRICTED" else action.label.uppercase(),
                                icon = if (isRestricted) "🚫" else action.icon,
                                type = action.type,
                                modifier = Modifier.weight(1f),
                                enabled = !isRestricted,
                                onClick = { onActionClick(action.type, action.parameter) }
                            )
                        }
                        if (rowActions.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            NauticalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // World Activity
            Text(
                text = "📜 SHIP'S LOG",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            ThemedCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Daily Stats / Rewards Summary
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LogStat("Level", character.level.toString(), MaterialTheme.colorScheme.primary)
                        LogStat("Gold", character.gold.toString(), Color(0xFFFFD700))
                        LogStat("Bounty", character.bounty.toString(), MaterialTheme.colorScheme.error)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    if (playersNearby.isNotEmpty()) {
                        playersNearby.take(3).forEach { player ->
                            ActivityItem("${player.name} spotted at ${location?.name ?: "the horizon"}")
                        }
                    } else {
                        ActivityItem("The sea is calm today, Captain.")
                    }
                }
            }
        }
        
        LowHpWarning(hpRatio = character.hp.toFloat() / character.maxHp.toFloat())
        
        // Weather Overlays
        location?.let { loc ->
            when (loc.weather) {
                "Rainy", "Stormy" -> RainOverlay(isStormy = loc.weather == "Stormy")
                "Foggy" -> FogOverlay()
                "Snowy" -> SnowOverlay()
            }
        }
    }
}

@Composable
fun RainOverlay(isStormy: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "Rain")
    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isStormy) 500 else 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RainOffset"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f)) {
        val strokeWidth = 2f
        val lineLength = if (isStormy) 40f else 25f
        
        for (i in 0..100) {
            val x = (i * 47) % size.width
            val y = ((i * 123) + rainOffset) % size.height
            
            drawLine(
                color = Color.LightGray,
                start = Offset(x, y),
                end = Offset(x - (if (isStormy) 10f else 5f), y + lineLength),
                strokeWidth = strokeWidth
            )
        }
    }
    
    if (isStormy) {
        val lightningTransition = rememberInfiniteTransition(label = "Lightning")
        val alpha by lightningTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 5000
                    0f at 0
                    0f at 4800
                    0.8f at 4850
                    0f at 4900
                    0.6f at 4950
                    0f at 5000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "LightningAlpha"
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = alpha * 0.2f)))
    }
}

@Composable
fun FogOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "Fog")
    val fogOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FogOffset"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().alpha(0.4f)) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.1f), Color.LightGray.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f)),
                start = Offset(fogOffset - 1000f, 0f),
                end = Offset(fogOffset, 500f)
            )
        )
    }
}

@Composable
fun SnowOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "Snow")
    val snowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SnowOffset"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().alpha(0.5f)) {
        for (i in 0..50) {
            val x = (i * 89 + (snowOffset / 5)) % size.width
            val y = ((i * 157) + snowOffset) % size.height
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}

fun isNonCombatAction(type: ActionType): Boolean {
    return when (type) {
        ActionType.Training, ActionType.Market, ActionType.BlackMarket, 
        ActionType.Shipyard, ActionType.Fishing, ActionType.Work,
        ActionType.Kitchen, ActionType.Forge, ActionType.Observatory, 
        ActionType.Infirmary -> true
        else -> false
    }
}

@Composable
fun InfoBit(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
fun GameFeatureCard(
    label: String,
    count: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActionCard(label: String, icon: String, modifier: Modifier = Modifier, enabled: Boolean = true, type: ActionType? = null, onClick: () -> Unit) {
    val categoryColor = when (type) {
        ActionType.Training -> Color(0xFF81C784)
        ActionType.Market, ActionType.BlackMarket, ActionType.Smuggler -> Color(0xFFFFD54F)
        ActionType.Shipyard -> Color(0xFF64B5F6)
        ActionType.Fishing, ActionType.Work, ActionType.Kitchen, ActionType.Forge -> Color(0xFFAED581)
        ActionType.Infirmary -> Color(0xFFE57373)
        ActionType.Tavern -> Color(0xFFFFB74D)
        ActionType.Arena, ActionType.Grind -> Color(0xFFE57373)
        ActionType.MythicRoll -> Color(0xFF9575CD)
        else -> MaterialTheme.colorScheme.outline
    }

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            brush = if (enabled) Brush.verticalGradient(listOf(categoryColor, categoryColor.copy(alpha = 0.3f))) 
                    else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ),
        colors = if (!enabled) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) 
                 else CardDefaults.outlinedCardColors(containerColor = categoryColor.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon, 
                fontSize = 28.sp, 
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = if (enabled) categoryColor else MaterialTheme.colorScheme.error,
                modifier = Modifier.alpha(if (enabled) 1f else 0.7f),
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun WarBanner(war: WarState) {
    val totalScore = (war.navyScore + war.pirateScore).coerceAtLeast(1)
    val navyProgress = war.navyScore.toFloat() / totalScore
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚔️ FACTION WAR: ${war.targetLocation}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.weight(1f))
                val remaining = (war.endTime - System.currentTimeMillis()) / (60 * 1000)
                Text(text = "${remaining}m left", style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tug of War Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxHeight().weight(navyProgress.coerceAtLeast(0.01f)).clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)).background(Color(0xFF2196F3)))
                    Box(modifier = Modifier.fillMaxHeight().weight((1f - navyProgress).coerceAtLeast(0.01f)).clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)).background(Color(0xFFE57373)))
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "NAVY: ${war.navyScore}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                Text(text = "PIRATES: ${war.pirateScore}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RaidAlertBanner(raid: RaidBoss, onBannerClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "RaidGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    ElevatedCard(
        onClick = onBannerClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Red.copy(alpha = glowAlpha), RoundedCornerShape(12.dp)),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💀", fontSize = 32.sp, modifier = Modifier.alpha(glowAlpha))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WORLD BOSS ACTIVE!",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Red,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = raid.enemy.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Location: (${raid.x.toInt()}, ${raid.y.toInt()}) | HP: ${raid.enemy.hp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun ActivityItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun LogStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}
