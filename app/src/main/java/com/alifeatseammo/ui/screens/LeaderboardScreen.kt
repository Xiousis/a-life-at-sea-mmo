package com.alifeatseammo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Faction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    players: List<Character>,
    selectedFaction: Faction?,
    onFactionSelected: (Faction?) -> Unit,
    selectedSort: String,
    onSortSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    onPlayerClick: (Character) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboards") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val factions = listOf(Faction.Pirate, Faction.Navy, null)
            val factionTitles = listOf("Pirates", "Navy", "Global")
            val selectedFactionIndex = factions.indexOf(selectedFaction).coerceAtLeast(0)

            SecondaryTabRow(selectedTabIndex = selectedFactionIndex) {
                factionTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFactionIndex == index,
                        onClick = { onFactionSelected(factions[index]) },
                        text = { Text(title) }
                    )
                }
            }

            val sorts = listOf("level", "bounty", "infamy", "gold")
            val sortTitles = listOf("Level", "Bounty", "Infamy", "Gold")
            val selectedSortIndex = sorts.indexOf(selectedSort).coerceAtLeast(0)

            SecondaryScrollableTabRow(
                selectedTabIndex = selectedSortIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                divider = {}
            ) {
                sortTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSortIndex == index,
                        onClick = { onSortSelected(sorts[index]) },
                        text = { Text(title, style = MaterialTheme.typography.labelSmall) }
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
                            Text(
                                text = when (selectedSort) {
                                    "bounty" -> "${player.bounty} B"
                                    "infamy" -> "${player.infamy} Inf"
                                    "gold" -> "${player.gold} G"
                                    else -> "Lvl ${player.level}"
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { onPlayerClick(player) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
