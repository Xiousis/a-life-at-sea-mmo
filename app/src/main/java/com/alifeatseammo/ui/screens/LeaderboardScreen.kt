package com.alifeatseammo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    players: List<Character>,
    onBackClick: () -> Unit,
    onPlayerClick: (Character) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top Pirates") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(players) { index, player ->
                ListItem(
                    headlineContent = { Text("${index + 1}. ${player.name}") },
                    supportingContent = { Text("Level ${player.level} | XP: ${player.xp}") },
                    trailingContent = { Text("${player.gold} Gold") },
                    modifier = Modifier.clickable { onPlayerClick(player) }
                )
                HorizontalDivider()
            }
        }
    }
}
