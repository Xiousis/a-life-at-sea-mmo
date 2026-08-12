package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.alifeatseammo.R
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Faction
import com.alifeatseammo.util.MusicManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvPScreen(
    character: Character,
    potentialTargets: List<Character>,
    onAttackClick: (Character) -> Unit,
    onPlayerClick: (Character) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        MusicManager.play(context, R.raw.life_at_sea_menu_sound)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PvP - ${character.currentLocation}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(potentialTargets) { target ->
                val isBounty = character.faction == Faction.Navy && target.faction == Faction.Pirate && target.bounty > 1000
                
                OutlinedCard(
                    onClick = { onPlayerClick(target) },
                    border = if (isBounty) 
                        androidx.compose.foundation.BorderStroke(2.dp, Color.Red) 
                    else 
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    ListItem(
                        headlineContent = { 
                            Row {
                                Text(target.name)
                                if (isBounty) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = Color.Red) { Text("BOUNTY", color = Color.White) }
                                }
                            }
                        },
                        supportingContent = { Text("Level ${target.level} | Bounty: ${target.bounty} | ${target.faction}") },
                        trailingContent = {
                            Button(
                                onClick = { onAttackClick(target) },
                                colors = if (isBounty) ButtonDefaults.buttonColors(containerColor = Color.Red) else ButtonDefaults.buttonColors()
                            ) {
                                Text("Attack")
                            }
                        }
                    )
                }
            }
        }
    }
}
