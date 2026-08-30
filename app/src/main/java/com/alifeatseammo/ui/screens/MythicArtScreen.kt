package com.alifeatseammo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.MythicArt
import com.alifeatseammo.data.model.ElementType
import com.alifeatseammo.ui.components.getElementColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MythicArtScreen(
    character: Character,
    actionState: com.alifeatseammo.ui.UIActionState,
    onRollClick: () -> Unit,
    onAdminGrantTestItems: () -> Unit,
    onBackClick: () -> Unit
) {
    val isLoading = actionState is com.alifeatseammo.ui.UIActionState.Loading
    val isRolling = actionState is com.alifeatseammo.ui.UIActionState.Loading && actionState.label.contains("Rolling")
    
    var rollButtonEnabled by remember { mutableStateOf(true) }
    
    val tiers = listOf("F", "E", "D", "C", "B", "A", "S", "SS", "SSS", "Z")
    var displayTierIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            while (true) {
                displayTierIndex = (displayTierIndex + 1) % tiers.size
                kotlinx.coroutines.delay(80)
            }
        }
    }
    
    LaunchedEffect(isRolling) {
        if (!isRolling) {
            // Re-enable button after 1.2 seconds if it was just rolling
            kotlinx.coroutines.delay(1200)
            rollButtonEnabled = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ancient Altar") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (rest of the UI remains same)
            Text(
                text = "Island of World Secrets",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "The altar whispers of ancient powers...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Current Mythic Art Display
            CurrentMythicArtCard(character.mythicArt)

            if (actionState is com.alifeatseammo.ui.UIActionState.Error) {
                Text(
                    text = actionState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (actionState is com.alifeatseammo.ui.UIActionState.Success) {
                Text(
                    text = actionState.label,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Roll Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (character.isAdmin) {
                        Column {
                            Button(
                                onClick = onAdminGrantTestItems,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text(if (isLoading && actionState.label.contains("Granting")) "GRANTING..." else "ADMIN: GRANT TEST ARTIFACTS")
                            }
                        }
                    }

                    val freeRolls = character.freeMythicRolls
                    val canAfford = (freeRolls > 0 || character.gold >= 1000000) && !isLoading
                    
                    if (freeRolls > 0) {
                        Text(
                            text = "Free Rolls Remaining: $freeRolls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Text(
                            text = "Roll Cost: 1,000,000 Gold",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            rollButtonEnabled = false
                            onRollClick()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = canAfford && character.inventory.size < character.inventoryCapacity && rollButtonEnabled,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRolling) {
                            Text(
                                text = tiers[displayTierIndex],
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("AWAKENING...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        } else {
                            Text(
                                text = if (freeRolls > 0) "USE FREE ROLL" else "ROLL FOR 1M GOLD",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    if (character.inventory.size >= character.inventoryCapacity) {
                        Text(
                            text = "Inventory is full! Free up space first.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentMythicArtCard(mythicArt: MythicArt?) {
    val tierColor = when (mythicArt?.tier) {
        "Z" -> Color(0xFF00FFFF)   // Cyan / Divine
        "SSS" -> Color(0xFFFFD700) // Gold
        "SS" -> Color(0xFFE5E4E2)  // Platinum
        "S" -> Color(0xFFC0C0C0)   // Silver
        "A" -> Color(0xFFFF4500)   // OrangeRed
        "B" -> Color(0xFF9370DB)   // Purple
        "C" -> Color(0xFF1E90FF)   // DodgerBlue
        "D" -> Color(0xFF4CAF50)   // Green
        "E" -> Color(0xFF8B4513)   // Brown/Bronze
        "F" -> Color.Gray
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(tierColor.copy(alpha = 0.2f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(2.dp, tierColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (mythicArt != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "[ ${mythicArt.tier} ]",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = tierColor
                )
                Text(
                    text = mythicArt.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mythicArt.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (mythicArt.elements.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Elements:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        mythicArt.elements.forEach { element ->
                            Text(
                                text = "${element.symbol} $element",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = getElementColor(element)
                            )
                        }
                    }
                }

                if (mythicArt.elementalWeaknesses.isNotEmpty()) {
                    Text(
                        text = "Elemental Weakness: ${mythicArt.elementalWeaknesses.joinToString(", ") { "${it.symbol} $it" }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (mythicArt.skillMultiplier > 1.0f) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${mythicArt.multipliedSkill} Buff: +${((mythicArt.skillMultiplier - 1.0f) * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                if (mythicArt.hugeBuffValue > 0 && mythicArt.hugeBuffType != null) {
                    Text(
                        text = "${mythicArt.hugeBuffType}: +${(mythicArt.hugeBuffValue * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                if (mythicArt.debuffPercentage > 0f) {
                    Text(
                        text = "Debuff: -${(mythicArt.debuffPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (mythicArt.energyRegainMultiplier > 1.0f) {
                    Text(
                        text = "Energy Regain: x${mythicArt.energyRegainMultiplier}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }

                if (mythicArt.travelTimeMultiplier != 1.0f) {
                    val isBuff = mythicArt.travelTimeMultiplier < 1.0f
                    val displayValue = if (isBuff) (1.0f / mythicArt.travelTimeMultiplier).toInt() else mythicArt.travelTimeMultiplier
                    Text(
                        text = if (isBuff) "Travel Speed: x$displayValue" else "Travel Time: x$displayValue",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBuff) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }

                if (mythicArt.weakAgainst.isNotEmpty()) {
                    Text(
                        text = "Weak Against: ${mythicArt.weakAgainst.joinToString(", ")}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (!mythicArt.canLearnNonCombatSkills) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DEBUFF: NO NON-COMBAT ACTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (mythicArt.restrictedSkillTypes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "RESTRICTIONS:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Disables: ${mythicArt.restrictedSkillTypes.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                if (mythicArt.techniques.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Special Techniques:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                    Text(
                        text = mythicArt.techniques.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
                Text(
                    text = "No Mythic Art Awakened",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
