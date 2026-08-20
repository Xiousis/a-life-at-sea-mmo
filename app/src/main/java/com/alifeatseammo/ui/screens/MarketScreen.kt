package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Item
import com.alifeatseammo.data.model.Rarity
import com.alifeatseammo.ui.components.getRarityColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    character: Character,
    actionState: com.alifeatseammo.ui.UIActionState,
    marketItems: List<Item>,
    onBuyItem: (Item) -> Unit,
    onSellItem: (Item) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isLoading = actionState is com.alifeatseammo.ui.UIActionState.Loading
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text("Gold: ${character.gold}", modifier = Modifier.padding(end = 16.dp))
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Buy", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Sell", modifier = Modifier.padding(16.dp))
                }
            }
            
            if (selectedTab == 0) {
                BuyTab(marketItems, character, isLoading, onBuyItem)
            } else {
                SellTab(character.inventory, isLoading, onSellItem)
            }
        }
    }
}

@Composable
fun BuyTab(items: List<Item>, character: Character, isLoading: Boolean, onBuyItem: (Item) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No items for sale here")
        }
    } else {
        LazyColumn {
            items(items) { item ->
                MarketItemRow(item, character, !isLoading && character.gold >= item.price, "Buy", onBuyItem)
            }
        }
    }
}

@Composable
fun SellTab(inventory: List<Item>, isLoading: Boolean, onSellItem: (Item) -> Unit) {
    if (inventory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inventory is empty")
        }
    } else {
        LazyColumn {
            items(inventory) { item ->
                // Character is null here for simplicity as we don't need reqs for selling
                MarketItemRow(item, null, !isLoading, "Sell (${item.price / 2})", onSellItem)
            }
        }
    }
}

@Composable
fun MarketItemRow(item: Item, character: Character?, isEnabled: Boolean, actionLabel: String, onAction: (Item) -> Unit) {
    val missingRequirements = character?.getMissingRequirements(item) ?: emptyList()
    val canEquip = character?.canEquip(item) ?: true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, color = getRarityColor(item.rarity))
                Text(text = item.description, style = MaterialTheme.typography.bodySmall)
                
                if (missingRequirements.isNotEmpty()) {
                    Text(
                        text = "Req: ${missingRequirements.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (item.levelRequirement > 1) {
                    Text(text = "Level Req: ${item.levelRequirement}", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { onAction(item) },
                enabled = isEnabled
            ) {
                Text(actionLabel)
            }
        }
    }
}
