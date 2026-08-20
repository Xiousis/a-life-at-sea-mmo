package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.CombatState
import com.alifeatseammo.data.model.Rarity
import com.alifeatseammo.ui.components.RewardCard

@Composable
fun VictoryScreen(
    combatState: CombatState,
    onClaimRewards: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "VICTORY",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4CAF50),
                letterSpacing = 8.sp
            )
            
            Text(
                text = "────────────────────────",
                color = Color(0xFF4CAF50).copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "You have defeated",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = combatState.enemy.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RewardCard("XP Gained", "+${combatState.xpEarned}", Color(0xFF2196F3))
                RewardCard("Gold Earned", "+${combatState.goldEarned}", Color(0xFFFFC107))
            }

            if (combatState.loot.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "LOOT ACQUIRED",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    combatState.loot.forEach { item ->
                        Text(
                            text = "• ${item.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = when(item.rarity) {
                                Rarity.Common -> Color.Gray
                                Rarity.Uncommon -> Color(0xFF4CAF50)
                                Rarity.Rare -> Color(0xFF2196F3)
                                Rarity.Epic -> Color(0xFF9C27B0)
                                Rarity.Legendary -> Color(0xFFFF9800)
                                Rarity.Mythic -> Color(0xFF00E5FF)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onClaimRewards,
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("CLAIM REWARDS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
