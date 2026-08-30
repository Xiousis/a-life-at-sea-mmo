package com.alifeatseammo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.LocationDef
import com.alifeatseammo.data.model.RaidBoss
import com.alifeatseammo.data.model.SeaEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    character: Character,
    locations: List<LocationDef>,
    activeRaids: List<RaidBoss> = emptyList(),
    seaEvents: List<SeaEvent> = emptyList(),
    onLocationClick: (LocationDef) -> Unit,
    onRaidClick: (RaidBoss) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf<LocationDef?>(null) }
    var selectedEvent by remember { mutableStateOf<SeaEvent?>(null) }
    var selectedRaid by remember { mutableStateOf<RaidBoss?>(null) }

    // Dynamic scaling based on location bounds
    val minX = locations.minOfOrNull { it.x }?.toFloat() ?: -500f
    val maxX = locations.maxOfOrNull { it.x }?.toFloat() ?: 500f
    val minY = locations.minOfOrNull { it.y }?.toFloat() ?: -500f
    val maxY = locations.maxOfOrNull { it.y }?.toFloat() ?: 500f

    val padding = 150f
    val mapMinX = (minX - padding).coerceAtMost(-500f)
    val mapMaxX = (maxX + padding).coerceAtLeast(500f)
    val mapMinY = (minY - padding).coerceAtMost(-500f)
    val mapMaxY = (maxY + padding).coerceAtLeast(500f)

    val mapWidth = mapMaxX - mapMinX
    val mapHeight = mapMaxY - mapMinY

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Map") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val currentLocationInfo = locations.find { it.name == character.currentLocation }
            currentLocationInfo?.let { loc ->
                Text(
                    text = "Current: ${loc.name} (${loc.x}, ${loc.y})",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = "Total Islands: ${locations.size}",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(locations, seaEvents, mapMinX, mapMaxX, mapMinY, mapMaxY) {
                            detectTapGestures { offset ->
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                // Check raids first
                                activeRaids.forEach { raid ->
                                    val x = (raid.x.toFloat() - mapMinX) * (canvasWidth / mapWidth)
                                    val y = (raid.y.toFloat() - mapMinY) * (canvasHeight / mapHeight)
                                    val raidOffset = Offset(x, y)
                                    if ((offset - raidOffset).getDistance() < 50f) {
                                        selectedRaid = raid
                                        selectedLocation = null
                                        selectedEvent = null
                                        return@detectTapGestures
                                    }
                                }

                                // Check events next
                                seaEvents.forEach { event ->
                                    val x = (event.x - mapMinX) * (canvasWidth / mapWidth)
                                    val y = (event.y - mapMinY) * (canvasHeight / mapHeight)
                                    val eventOffset = Offset(x, y)
                                    if ((offset - eventOffset).getDistance() < 40f) {
                                        selectedEvent = event
                                        selectedLocation = null
                                        return@detectTapGestures
                                    }
                                }

                                locations.forEach { loc ->
                                    val x = (loc.x - mapMinX) * (canvasWidth / mapWidth)
                                    val y = (loc.y - mapMinY) * (canvasHeight / mapHeight)
                                    val locOffset = Offset(x, y)
                                    
                                    if ((offset - locOffset).getDistance() < 40f) {
                                        selectedLocation = loc
                                        selectedEvent = null
                                        selectedRaid = null
                                    }
                                }
                            }
                        }
                ) {
                    // Draw Water
                    drawRect(color = Color(0xFFB3E5FC))

                    // Draw Grid
                    val gridSteps = 10
                    val stepW = size.width / gridSteps
                    val stepH = size.height / gridSteps
                    for (i in 0..gridSteps) {
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(i * stepW, 0f), Offset(i * stepW, size.height))
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, i * stepH), Offset(size.width, i * stepH))
                    }

                    // Draw Sea Events Areas
                    seaEvents.forEach { event ->
                        val x = (event.x - mapMinX) * (size.width / mapWidth)
                        val y = (event.y - mapMinY) * (size.height / mapHeight)
                        val radius = event.radius.toFloat() * (size.width / mapWidth)
                        
                        drawCircle(
                            color = Color(android.graphics.Color.parseColor(event.type.color)).copy(alpha = 0.2f),
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }

                    // Draw Locations
                    locations.forEach { loc ->
                        val x = (loc.x - mapMinX) * (size.width / mapWidth)
                        val y = (loc.y - mapMinY) * (size.height / mapHeight)
                        
                        val isCurrent = loc.name == character.currentLocation
                        
                        drawCircle(
                            color = if (isCurrent) Color.Red else if (loc.isSafe) Color(0xFF4CAF50) else Color(0xFF795548),
                            radius = if (isCurrent) 12f else 8f,
                            center = Offset(x, y)
                        )
                    }

                    // Draw Raid Bosses
                    activeRaids.forEach { raid ->
                        val x = (raid.x.toFloat() - mapMinX) * (size.width / mapWidth)
                        val y = (raid.y.toFloat() - mapMinY) * (size.height / mapHeight)
                        
                        // Draw Skull/Boss Icon (Larger)
                        drawCircle(
                            color = Color.Black,
                            radius = 16f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.Red,
                            radius = 12f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = Offset(x - 4f, y - 4f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = Offset(x + 4f, y - 4f)
                        )
                    }

                    // Draw Sea Event Icons
                    seaEvents.forEach { event ->
                        val x = (event.x - mapMinX) * (size.width / mapWidth)
                        val y = (event.y - mapMinY) * (size.height / mapHeight)
                        
                        drawCircle(
                            color = Color(android.graphics.Color.parseColor(event.type.color)),
                            radius = 10f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Selection Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(180.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (selectedRaid != null) {
                        val raid = selectedRaid!!
                        Text(
                            text = "💀 WORLD BOSS: ${raid.enemy.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Lv. ${raid.enemy.level} | HP: ${raid.enemy.hp}/${raid.enemy.maxHp} | Pos: (${raid.x.toInt()}, ${raid.y.toInt()})",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val currentLocInfo = locations.find { it.name == character.currentLocation }
                        val distance = currentLocInfo?.let { loc ->
                            kotlin.math.sqrt(Math.pow(raid.x - loc.x, 2.0) + Math.pow(raid.y - loc.y, 2.0))
                        } ?: 1000.0

                        if (distance <= 1000.0) {
                            Button(
                                onClick = { onRaidClick(raid) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("BATTLE RAID BOSS")
                            }
                        } else {
                            Text(
                                text = "You are too far away (${distance.toInt()} units) to engage. Sail to a closer island.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else if (selectedEvent != null) {
                        val event = selectedEvent!!
                        Text(
                            text = "${event.type.icon} ${event.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Effect: ${event.effectDescription}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val timeLeft = (event.endTime - System.currentTimeMillis()) / 1000 / 60
                        Text(
                            text = "Ends in: ${timeLeft.coerceAtLeast(0)} mins",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    } else {
                        selectedLocation?.let { loc ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = loc.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${loc.region} • Rec. Lv. ${loc.recommendedLevel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (loc.isSafe) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "SAFE",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (loc.name != character.currentLocation) {
                            Button(
                                onClick = { onLocationClick(loc) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Set Course for ${loc.name}")
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "You are currently here",
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } ?: run {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Select an island on the nautical chart to view details",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
}

