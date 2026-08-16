package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.CombatState
import com.alifeatseammo.ui.components.RewardCard

@Composable
fun DefeatScreen(
    combatState: CombatState,
    onRetreat: () -> Unit
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
                text = "DEFEAT",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFD32F2F),
                letterSpacing = 8.sp
            )
            
            Text(
                text = "────────────────────────",
                color = Color(0xFFD32F2F).copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "You were defeated by",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = combatState.enemy.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "You lost some gold and have been sent back to a safe location to recover.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (combatState.goldEarned < 0) {
                Spacer(modifier = Modifier.height(16.dp))
                RewardCard("Gold Lost", "${combatState.goldEarned}", Color(0xFFD32F2F))
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onRetreat,
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("RETREAT", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
