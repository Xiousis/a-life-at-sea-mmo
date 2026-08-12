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
import com.alifeatseammo.data.model.Faction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    players: List<Character>,
    selectedFaction: Faction?,
    onFactionSelected: (Faction?) -> Unit,
    onBackClick: () -> Unit,
    onPlayerClick: (Character) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboards") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val tabs = listOf(Faction.Pirate, Faction.Navy, null)
            val tabTitles = listOf("Pirates", "Navy", "Global")
            
            val selectedIndex = tabs.indexOf(selectedFaction).coerceAtLeast(0)

            SecondaryTabRow(selectedTabIndex = selectedIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { onFactionSelected(tabs[index]) },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(players) { index, player ->
                    ListItem(
                        headlineContent = { Text("${index + 1}. ${player.name}") },
                        supportingContent = { Text("Level ${player.level} | ${player.faction}") },
                        trailingContent = { 
                            if (selectedFaction == Faction.Pirate) {
                                Text("${player.bounty} B")
                            } else {
                                Text("Lvl ${player.level}")
                            }
                        },
                        modifier = Modifier.clickable { onPlayerClick(player) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
