package com.alifeatseammo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.ElementType
import com.alifeatseammo.data.model.Rarity
import com.alifeatseammo.ui.UIActionState
import androidx.compose.material3.CircularProgressIndicator

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
    val targetProgress = if (max > 0) (current.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = labelOverride ?: "$current / $max",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
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
