package com.alifeatseammo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.ElementType
import com.alifeatseammo.data.model.ItemType
import com.alifeatseammo.data.model.Rarity
import com.alifeatseammo.ui.UIActionState

@Composable
fun LowHpWarning(hpRatio: Float) {
    if (hpRatio > 0.25f) return

    val infiniteTransition = rememberInfiniteTransition(label = "LowHpPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VignetteAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Red.copy(alpha = alpha)),
                    radius = 2000f
                )
            )
    )
}

fun getItemEmoji(type: ItemType): String {
    return when (type) {
        ItemType.Weapon -> "⚔️"
        ItemType.Armor -> "🛡️"
        ItemType.Accessory -> "💍"
        ItemType.Bag -> "🎒"
        ItemType.Consumable -> "🧪"
        ItemType.Tool -> "⚒️"
        ItemType.Miscellaneous -> "📦"
        ItemType.Fish -> "🐟"
        ItemType.Food -> "🍗"
        ItemType.Artifact -> "✨"
        ItemType.Lure -> "🪱"
        ItemType.Ship -> "⛵"
        ItemType.Ingredient -> "🧂"
    }
}

@Composable
fun ActionOverlay(state: UIActionState) {
    if (state is UIActionState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = state.label, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun getElementColor(element: ElementType?): Color {
    return when (element) {
        ElementType.Physical -> Color(0xFFC0C0C0)
        ElementType.Hybrid -> Color(0xFF9370DB)
        ElementType.Fire -> Color(0xFFF44336)
        ElementType.Water -> Color(0xFF2196F3)
        ElementType.Earth -> Color(0xFF795548)
        ElementType.Air -> Color(0xFFE1F5FE)
        ElementType.Lightning -> Color(0xFFFFEB3B)
        ElementType.Ice -> Color(0xFF80D8FF)
        ElementType.Light -> Color(0xFFFFF9C4)
        ElementType.Dark -> Color(0xFF4527A0)
        ElementType.Void -> Color(0xFF1A237E)
        ElementType.Chaos -> Color(0xFFD32F2F)
        ElementType.Celestial -> Color(0xFFFFD180)
        ElementType.Genesis -> Color(0xFF80CBC4)
        ElementType.Divine -> Color(0xFFFFD700)
        ElementType.Annihilation -> Color(0xFF212121)
        ElementType.Creation -> Color(0xFFF8BBD0)
        else -> Color.Gray
    }
}

@Composable
fun StatusBar(
    label: String,
    current: Int,
    max: Int,
    color: Color,
    modifier: Modifier = Modifier,
    labelOverride: String? = null
) {
    val hpRatio = if (max > 0) current.toFloat() / max.toFloat() else 0f
    val isCritical = label == "HP" && hpRatio < 0.25f

    val infiniteTransition = rememberInfiniteTransition(label = "StatusBarPulse")
    val pulseAlpha by if (isCritical) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val targetProgress = if (max > 0) (current.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "StatusBarProgress"
    )

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = if (isCritical) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(pulseAlpha)
            )
            Text(
                text = labelOverride ?: "$current / $max",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
        ) {
            // Background sheen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent, Color.Black.copy(alpha = 0.05f))
                        )
                    )
            )
            
            // Progress Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .alpha(pulseAlpha)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                color.copy(alpha = 0.9f),
                                color,
                                color.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)),
                        shape = RoundedCornerShape(7.dp)
                    )
            ) {
                // Animated Sheen/Gloss
                val sheenTransition = rememberInfiniteTransition(label = "SheenTransition")
                val sheenOffset by sheenTransition.animateFloat(
                    initialValue = -500f,
                    targetValue = 1500f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "SheenOffset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                start = androidx.compose.ui.geometry.Offset(sheenOffset, 0f),
                                end = androidx.compose.ui.geometry.Offset(sheenOffset + 200f, 200f)
                            )
                        )
                )

                // Inner Glassy Highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
        }
    }
}

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle texture/gradient background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
                            radius = 1000f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(borderColor.copy(alpha = 0.6f), borderColor.copy(alpha = 0.2f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                content = content
            )
            
            // Corner detail (Nautical)
            Text(
                text = "⚓",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .alpha(0.1f),
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun NauticalDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
        ))
        Text(
            text = " ⚓ ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(
            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), Color.Transparent))
        ))
    }
}

@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LEVEL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = level.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun RewardCard(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp)
            .width(100.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun getRarityColor(rarity: Rarity): Color {
    return when (rarity) {
        Rarity.Common -> MaterialTheme.colorScheme.onSurface
        Rarity.Uncommon -> Color(0xFF4CAF50) // Green
        Rarity.Rare -> Color(0xFF2196F3) // Blue
        Rarity.Epic -> Color(0xFF9C27B0) // Purple
        Rarity.Legendary -> Color(0xFFFF9800) // Orange
        Rarity.Mythic -> Color(0xFFF44336) // Red
    }
}
