package com.alifeatseammo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val mapMinX = Math.min(minX - padding, -500f)
    val mapMaxX = Math.max(maxX + padding, 500f)
    val mapMinY = Math.min(minY - padding, -500f)
    val mapMaxY = Math.max(maxY + padding, 500f)

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
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                    .height(150.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (selectedLocation != null) {
                        Text(text = selectedLocation!!.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = selectedLocation!!.region, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedLocation!!.name != character.currentLocation) {
                            Button(
                                onClick = { onLocationClick(selectedLocation!!) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Travel to ${selectedLocation!!.name}")
                            }
                        } else {
                            Text(text = "You are currently here", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        Text(text = "Select an island on the map", color = Color.Gray)
                    }
                }
            }
        }
    }
}
