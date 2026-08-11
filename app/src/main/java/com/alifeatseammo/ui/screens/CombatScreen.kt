package com.alifeatseammo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.CombatAction

@Composable
fun CombatScreen(
    character: Character,
    onActionClick: (CombatAction) -> Unit
) {
    val combatState = character.combatState ?: return
    val enemy = combatState.enemy
    val listState = rememberLazyListState()

    LaunchedEffect(combatState.logs.size) {
        if (combatState.logs.isNotEmpty()) {
            listState.animateScrollToItem(combatState.logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Text(
            text = "BATTLE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Player Section
        CombatantStatus(
            name = "YOU",
            hp = character.hp,
            maxHp = character.maxHp,
            barColor = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "vs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))

        // Enemy Section
        CombatantStatus(
            name = enemy.name.uppercase(),
            hp = enemy.hp,
            maxHp = enemy.maxHp,
            barColor = Color(0xFFF44336)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Narrative Log
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                .padding(8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(combatState.logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Grid
        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CombatButton("Attack", Modifier.weight(1f)) { onActionClick(CombatAction.Attack) }
                CombatButton("Technique", Modifier.weight(1f)) { onActionClick(CombatAction.Technique) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CombatButton("Defend", Modifier.weight(1f)) { onActionClick(CombatAction.Defend) }
                CombatButton("Item", Modifier.weight(1f)) { onActionClick(CombatAction.Item) }
            }
            CombatButton("Flee", Modifier.fillMaxWidth()) { onActionClick(CombatAction.Flee) }
        }

        Text(
            text = "────────────────────────",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CombatantStatus(name: String, hp: Int, maxHp: Int, barColor: Color) {
    val percentage = (hp.toFloat() / maxHp.toFloat() * 100).toInt()
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(text = name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "HP ", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { hp.toFloat() / maxHp.toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CombatButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = PaddingValues(12.dp)
    ) {
        Text(
            text = "[ $label ]",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
