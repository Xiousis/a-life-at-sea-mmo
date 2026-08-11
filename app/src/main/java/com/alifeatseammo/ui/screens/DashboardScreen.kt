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
import com.alifeatseammo.data.model.ActionType
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Location

@Composable
fun DashboardScreen(
    character: Character,
    location: Location?,
    playersNearby: List<Character>,
    onActionClick: (ActionType) -> Unit,
    onPlayerClick: (Character) -> Unit,
    onMissionsClick: () -> Unit,
    onMailClick: () -> Unit
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
                BadgedBox(badge = { Badge { Text("2") } }) {
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
                    text = "Lv. ${character.level} • ${character.race}",
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

                Spacer(modifier = Modifier.height(16.dp))

                StatusBar("HP", character.hp, character.maxHp, Color(0xFFE57373))
                Spacer(modifier = Modifier.height(8.dp))
                StatusBar("Energy", character.getCurrentEnergy(), character.maxEnergy, Color(0xFF64B5F6))
                Spacer(modifier = Modifier.height(8.dp))
                val xpNeeded = character.level * 100
                StatusBar("XP", character.xp, xpNeeded, Color(0xFF81C784))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    text = "Weather: ${it.weather} | Players here: ${playersNearby.size}",
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
                    text = "3 jobs currently available",
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
                ActivityItem("☠ Red Tide defeated a Sea Beast")
                ActivityItem("★ Raven reached Level 25")
                ActivityItem("⚔ Blackwake attacked Port Haven")
            }
        }
    }
}

@Composable
fun StatusBar(label: String, current: Int, max: Int, color: Color) {
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
            text = "$current/$max",
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
