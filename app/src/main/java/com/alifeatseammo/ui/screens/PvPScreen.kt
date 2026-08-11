package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvPScreen(
    character: Character,
    potentialTargets: List<Character>,
    onAttackClick: (Character) -> Unit,
    onBackClick: () -> Unit,
) {
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
                OutlinedCard(onClick = { onAttackClick(target) }) {
                    ListItem(
                        headlineContent = { Text(target.name) },
                        supportingContent = { Text("Level ${target.level} | Bounty: ${target.bounty}") },
                        trailingContent = {
                            Button(onClick = { onAttackClick(target) }) {
                                Text("Attack")
                            }
                        }
                    )
                }
            }
        }
    }
}
