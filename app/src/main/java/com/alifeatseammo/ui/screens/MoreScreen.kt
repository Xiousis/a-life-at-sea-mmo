package com.alifeatseammo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoreScreen(
    onMenuItemClick: (MoreMenuItem) -> Unit
) {
    val menuItems = listOf(
        MoreMenuItem("Character", "👤"),
        MoreMenuItem("Inventory", "📦"),
        MoreMenuItem("Skills", "📜"),
        MoreMenuItem("Leaderboard", "🏆"),
        MoreMenuItem("Chat", "💬"),
        MoreMenuItem("Settings", "⚙"),
        MoreMenuItem("Help", "❓")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "☰ MORE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(menuItems) { item ->
                MoreMenuRow(item) { onMenuItemClick(item) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun MoreMenuRow(
    item: MoreMenuItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = item.icon, fontSize = 24.sp, modifier = Modifier.width(40.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

data class MoreMenuItem(
    val label: String,
    val icon: String
)
