package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.*
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
                title = { 
                    Column {
                        Text("Market", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Location: ${character.currentLocation}", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${character.gold} Gold",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0, 
                    onClick = { selectedTab = 0 },
                    text = { Text("PURCHASE", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1, 
                    onClick = { selectedTab = 1 },
                    text = { Text("SELL", fontWeight = FontWeight.Bold) }
                )
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
            Text("No items for sale here", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items) { item ->
                MarketItemRow(
                    item = item, 
                    character = character, 
                    isEnabled = !isLoading && character.gold >= item.price, 
                    actionLabel = "BUY", 
                    onAction = onBuyItem
                )
            }
        }
    }
}

@Composable
fun SellTab(inventory: List<Item>, isLoading: Boolean, onSellItem: (Item) -> Unit) {
    if (inventory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inventory is empty", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(inventory) { item ->
                MarketItemRow(
                    item = item, 
                    character = null, 
                    isEnabled = !isLoading, 
                    actionLabel = "SELL", 
                    onAction = onSellItem,
                    isSell = true
                )
            }
        }
    }
}

@Composable
fun MarketItemRow(
    item: Item, 
    character: Character?, 
    isEnabled: Boolean, 
    actionLabel: String, 
    onAction: (Item) -> Unit,
    isSell: Boolean = false
) {
    val missingRequirements = character?.getMissingRequirements(item) ?: emptyList()
    val isFactionMismatch = item.factionRequirement != Faction.Neutral && character?.faction != item.factionRequirement

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isFactionMismatch) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else if (isEnabled || isSell) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item Icon placeholder (using a generic icon for now)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = getRarityColor(item.rarity).copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getItemIcon(item.type),
                        contentDescription = null,
                        tint = getRarityColor(item.rarity),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                        color = getRarityColor(item.rarity)
                    )
                    if (item.factionRequirement != Faction.Neutral) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FactionBadge(item.factionRequirement)
                    }
                }
                
                Text(
                    text = item.description, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                if (missingRequirements.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock, 
                            contentDescription = null, 
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = missingRequirements.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (item.levelRequirement > 1 && !isSell) {
                    Text(
                        text = "Lvl ${item.levelRequirement}+", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isSell) "${item.price / 2} G" else "${item.price} G",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSell) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onAction(item) },
                    enabled = isEnabled,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun FactionBadge(faction: Faction) {
    val color = when (faction) {
        Faction.Navy -> MaterialTheme.colorScheme.primary
        Faction.Pirate -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = faction.name.uppercase(),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

private fun getItemIcon(type: ItemType): ImageVector {
    // This could be expanded with more specific icons
    return when (type) {
        ItemType.Weapon -> Icons.Default.Info // Should be sword
        ItemType.Armor -> Icons.Default.Info // Should be shield
        ItemType.Consumable -> Icons.Default.Info // Should be food/drink
        else -> Icons.Default.Info
    }
}
