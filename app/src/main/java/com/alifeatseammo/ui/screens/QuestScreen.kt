package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.IslandQuest
import com.alifeatseammo.ui.UIActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestScreen(
    character: Character,
    actionState: UIActionState,
    quests: List<IslandQuest>,
    onQuestClick: (IslandQuest) -> Unit,
    onBackClick: () -> Unit
) {
    val isLoading = actionState is UIActionState.Loading
    
    // Filter quests by island if needed, but usually we show available ones for current location
    val filteredQuests = quests.filter { 
        it.islandId == character.currentLocation || it.islandId.isEmpty()
    }.sortedBy { it.minLevel }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Island Quests") },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredQuests) { quest ->
                val isCompleted = character.completedQuests.contains(quest.id)
                val isLevelOk = character.level >= quest.minLevel
                val isPrereqOk = quest.prerequisiteQuestId == null || character.completedQuests.contains(quest.prerequisiteQuestId)
                
                val isLocked = !isLevelOk || !isPrereqOk || isCompleted || isLoading
                val statusText = when {
                    isCompleted -> "COMPLETED"
                    !isPrereqOk -> "LOCKED: Prerequisite required"
                    !isLevelOk -> "LOCKED: Level ${quest.minLevel}+"
                    else -> "AVAILABLE"
                }

                QuestItem(quest, isLocked, statusText, onQuestClick)
            }
        }
    }
}

@Composable
fun QuestItem(quest: IslandQuest, isLocked: Boolean, statusText: String, onClick: (IslandQuest) -> Unit) {
    Card(
        onClick = { if (!isLocked) onClick(quest) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLocked,
        colors = if (isLocked && statusText != "AVAILABLE") CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (quest.isMainStory) {
                Text(
                    text = "🚩 MAIN STORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = quest.title, 
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isLocked && statusText != "AVAILABLE") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.Unspecified
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (statusText == "COMPLETED") Color(0xFF4CAF50) else if (statusText.startsWith("LOCKED")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = quest.description, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Reward: ${quest.goldReward} Gold, ${quest.xpReward} XP", style = MaterialTheme.typography.labelMedium)
                Text(text = "Lv. ${quest.minLevel}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
