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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                // Simple hit detection
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                locations.forEach { loc ->
                                    val x = (loc.x + 200) * (canvasWidth / 400f)
                                    val y = (loc.y + 200) * (canvasHeight / 400f)
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
                    val step = size.width / 10
                    for (i in 0..10) {
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(i * step, 0f), Offset(i * step, size.height))
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, i * step), Offset(size.width, i * step))
                    }

                    // Draw Locations
                    locations.forEach { loc ->
                        val x = (loc.x + 200) * (size.width / 400f)
                        val y = (loc.y + 200) * (size.height / 400f)
                        
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
