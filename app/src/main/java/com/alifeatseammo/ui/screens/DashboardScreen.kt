package com.alifeatseammo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.alifeatseammo.data.model.*
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
    onActionClick: (ActionType) -> Unit,
    onPlayerClick: (Character) -> Unit,
    onMissionsClick: () -> Unit,
    onMailClick: () -> Unit,
    onJoinFaction: (Faction) -> Unit
) {
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
            Text(
                text = "☠ A LIFE AT SEA",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                BadgedBox(badge = { if (mailCount > 0) Badge { Text("$mailCount") } }) {
                    IconButton(onClick = onMailClick) {
                        Text("✉", fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Player Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = character.name.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Lv. ${character.level} • ${character.race} • ${character.faction}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Bounty: ${character.bounty} B",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "💰 ${character.gold} Gold",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (character.infamy > 0) {
                    Text(
                        text = "⚖ Infamy: ${character.infamy}/100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                StatusBar("HP", character.hp, character.maxHp, Color(0xFFE57373))
                Spacer(modifier = Modifier.height(8.dp))
                StatusBar("Energy", character.getCurrentEnergy(), character.maxEnergy, Color(0xFF64B5F6))
                Spacer(modifier = Modifier.height(8.dp))
                if (character.level < 300) {
                    val xpNeeded = character.getXpNeeded()
                    StatusBar("XP", character.xp, xpNeeded, Color(0xFF81C784))
                } else {
                    StatusBar("XP", 1, 1, Color(0xFF81C784), labelOverride = "MAX")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${it.region} • ${if (it.isSafe) "Safe" else "Danger"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.isSafe) Color(0xFF81C784) else MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "\"${it.description}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Weather: ${it.weather} | Players here: $playerCount",
                    style = MaterialTheme.typography.labelMedium
                )

                if (playersNearby.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PLAYERS AT ${it.name}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playersNearby) { other ->
                            if (other.id != character.id) {
                                FilterChip(
                                    selected = false,
                                    onClick = { onPlayerClick(other) },
                                    label = { Text(other.name) },
                                    leadingIcon = { Text("☠") }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mission Board
        OutlinedCard(
            onClick = onMissionsClick,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚔ MISSION BOARD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (missionCount > 0) "$missionCount jobs currently available" else "No jobs currently available",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Grid
        location?.let { loc ->
            val chunkedActions = loc.actions.chunked(2)
            chunkedActions.forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowActions.forEach { action ->
                        ActionCard(
                            label = action.label,
                            icon = action.icon,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionClick(action.type) }
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

        // World Activity
        if (playersNearby.isNotEmpty()) {
            Text(
                text = "WORLD ACTIVITY",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    playersNearby.take(3).forEach { player ->
                        ActivityItem("${player.name} is currently at ${location?.name ?: "the docks"}")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBar(label: String, current: Int, max: Int, color: Color, labelOverride: String? = null) {
    val progress = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.padEnd(8),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(60.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = labelOverride ?: "$current/$max",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ActionCard(label: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
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
