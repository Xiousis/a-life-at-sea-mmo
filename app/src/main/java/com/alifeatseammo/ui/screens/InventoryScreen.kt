package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Item
import com.alifeatseammo.data.model.ItemType
import com.alifeatseammo.data.model.Rarity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    character: Character,
    actionState: com.alifeatseammo.ui.UIActionState,
    onEquipItem: (Item) -> Unit,
    onUnequipItem: (String) -> Unit,
    onUseItem: (Item) -> Unit,
    onCookItem: (Item) -> Unit,
    onSellItem: (Item) -> Unit,
    onBackClick: () -> Unit
) {
    var itemToUseWithWarning by remember { mutableStateOf<Item?>(null) }
    val isLoading = actionState is com.alifeatseammo.ui.UIActionState.Loading

    if (itemToUseWithWarning != null) {
        AlertDialog(
            onDismissRequest = { itemToUseWithWarning = null },
            title = { Text("Replace Mythic Art?") },
            text = { Text("You already have an active Mythic Art. Awakening this artifact will permanently replace your old Mythic Art and its techniques. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToUseWithWarning?.let { onUseItem(it) }
                        itemToUseWithWarning = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("REPLACE")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToUseWithWarning = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Equipment Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EQUIPMENT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            EquipmentGrid(character.equipment, onUnequipItem, isLoading)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Inventory Section
            Text(
                text = "BACKPACK (${character.inventory.size}/${character.calculateMaxCapacity()})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (character.inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your backpack is empty.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(character.inventory) { item ->
                        InventoryItemCard(
                            item = item,
                            character = character,
                            isLoading = isLoading,
                            onEquip = { onEquipItem(item) },
                            onUse = {
                                if (item.type == ItemType.Artifact && character.mythicArt != null) {
                                    itemToUseWithWarning = item
                                } else {
                                    onUseItem(item)
                                }
                            },
                            onCook = { onCookItem(item) },
                            onSell = { onSellItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EquipmentGrid(equipment: Map<String, Item?>, onUnequip: (String) -> Unit, isLoading: Boolean) {
    val topSlots = listOf("Weapon", "Armor", "Accessory")
    val bottomSlots = listOf("Helmet", "Boots", "Gloves", "Bag")
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topSlots.forEach { slot ->
                EquipmentSlot(
                    slotName = slot,
                    item = equipment[slot],
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    onUnequip = { onUnequip(slot) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bottomSlots.forEach { slot ->
                EquipmentSlot(
                    slotName = slot,
                    item = equipment[slot],
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    onUnequip = { onUnequip(slot) }
                )
            }
        }
    }
}

@Composable
fun EquipmentSlot(slotName: String, item: Item?, modifier: Modifier = Modifier, enabled: Boolean, onUnequip: () -> Unit) {
    OutlinedCard(
        modifier = modifier.height(100.dp),
        onClick = { if (item != null) onUnequip() },
        enabled = item != null && enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (item != null) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = slotName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            if (item != null) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Text(text = "Empty", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    item: Item,
    character: Character,
    isLoading: Boolean,
    onEquip: () -> Unit,
    onUse: () -> Unit,
    onCook: () -> Unit,
    onSell: () -> Unit
) {
    val rarityColor = when (item.rarity) {
        Rarity.Common -> Color.Gray
        Rarity.Uncommon -> Color(0xFF4CAF50)
        Rarity.Rare -> Color(0xFF2196F3)
        Rarity.Epic -> Color(0xFF9C27B0)
        Rarity.Legendary -> Color(0xFFFF9800)
        Rarity.Mythic -> Color(0xFF00E5FF)
    }

    val canEquip = character.canEquip(item)
    val missingRequirements = character.getMissingRequirements(item)

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (item.rarity != Rarity.Common) androidx.compose.foundation.BorderStroke(1.dp, rarityColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (canEquip || item.type !in listOf(ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag)) 
                MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.rarity != Rarity.Common) rarityColor else Color.Unspecified
                )
                Text(text = "${item.rarity} ${item.type.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                
                if (missingRequirements.isNotEmpty() && item.type in listOf(ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag)) {
                    Text(
                        text = "Req: ${missingRequirements.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(text = item.description, style = MaterialTheme.typography.bodySmall)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                when (item.type) {
                    ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag -> {
                        Button(
                            onClick = onEquip,
                            enabled = !isLoading && canEquip,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = if (!canEquip) ButtonDefaults.buttonColors(containerColor = Color.Gray) else ButtonDefaults.buttonColors()
                        ) {
                            Text("Equip", fontSize = 12.sp)
                        }
                    }
                    ItemType.Consumable, ItemType.Food, ItemType.Artifact -> {
                        Button(
                            onClick = onUse,
                            enabled = !isLoading,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(when(item.type) {
                                ItemType.Food -> "Eat"
                                ItemType.Artifact -> "Awaken"
                                else -> "Use"
                            }, fontSize = 12.sp)
                        }
                    }
                    ItemType.Fish -> {
                        Button(
                            onClick = onCook,
                            enabled = !isLoading,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Cook", fontSize = 12.sp)
                        }
                    }
                    else -> {}
                }
                TextButton(
                    onClick = onSell,
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Sell (${item.price / 2} G)", fontSize = 10.sp, color = if (isLoading) Color.Gray else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
