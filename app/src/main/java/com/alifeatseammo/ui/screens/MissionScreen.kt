package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Mission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionScreen(
    missions: List<Mission>,
    onMissionClick: (Mission) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Missions") },
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
            items(missions) { mission ->
                MissionItem(mission, onMissionClick)
            }
        }
    }
}

@Composable
fun MissionItem(mission: Mission, onClick: (Mission) -> Unit) {
    Card(
        onClick = { onClick(mission) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = mission.title, style = MaterialTheme.typography.titleLarge)
                Text(text = "${mission.energyCost} E", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mission.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Reward: ${mission.rewards.gold} Gold, ${mission.rewards.xp} XP", style = MaterialTheme.typography.labelMedium)
                Text(text = "Diff: ${mission.difficulty}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
