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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    character: Character,
    locations: List<LocationDef>,
    onLocationClick: (LocationDef) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf<LocationDef?>(null) }

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
                        .pointerInput(locations, mapMinX, mapMaxX, mapMinY, mapMaxY) {
                            detectTapGestures { offset ->
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                locations.forEach { loc ->
                                    val x = (loc.x - mapMinX) * (canvasWidth / mapWidth)
                                    val y = (loc.y - mapMinY) * (canvasHeight / mapHeight)
                                    val locOffset = Offset(x, y)
                                    
                                    if ((offset - locOffset).getDistance() < 40f) {
                                        selectedLocation = loc
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
                }
            }

            // Selection Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(160.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
