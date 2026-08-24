package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ItemType?>(null) }
    var isBulkSellMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<String>() }

    val filteredItems = character.inventory.filter { item ->
        val matchesSearch = item.name.contains(searchText, ignoreCase = true)
        val matchesCategory = selectedCategory == null || item.type == selectedCategory
        matchesSearch && matchesCategory
    }


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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "BACKPACK (${character.inventory.size}/${character.calculateMaxCapacity()})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bulk Sell", style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = isBulkSellMode,
                        onCheckedChange = { 
                            isBulkSellMode = it
                            if (!it) selectedItemIds.clear()
                        },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Search & Filter UI
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Text("✕")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                }
                items(ItemType.entries) { type ->
                    FilterChip(
                        selected = selectedCategory == type,
                        onClick = { selectedCategory = type },
                        label = { Text(type.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (character.inventory.isEmpty()) "Your backpack is empty." else "No items match your search.",
                        style = MaterialTheme.typography.bodyLarge, 
                        color = Color.Gray
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems) { item ->
                            val isSelected = selectedItemIds.contains(item.id)
                            InventoryItemCard(
                                item = item,
                                character = character,
                                isLoading = isLoading,
                                isSelectionMode = isBulkSellMode,
                                isSelected = isSelected,
                                onCardClick = {
                                    if (isBulkSellMode) {
                                        if (isSelected) selectedItemIds.remove(item.id)
                                        else selectedItemIds.add(item.id)
                                    }
                                },
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
                    
                    if (isBulkSellMode && selectedItemIds.isNotEmpty()) {
                        val totalValue = filteredItems.filter { selectedItemIds.contains(it.id) }.sumOf { it.price / 2 }
                        Button(
                            onClick = {
                                filteredItems.filter { selectedItemIds.contains(it.id) }.forEach { 
                                    onSellItem(it)
                                }
                                selectedItemIds.clear()
                                isBulkSellMode = false
                            },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("SELL SELECTED (${selectedItemIds.size}) FOR $totalValue G")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EquipmentGrid(equipment: Map<String, Item?>, onUnequip: (String) -> Unit, isLoading: Boolean) {
    val topSlots = listOf("Weapon", "Armor", "Accessory")
    val midSlots = listOf("Helmet", "Boots", "Gloves")
    val bottomSlots = listOf("Bag", "Ship")
    
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
            midSlots.forEach { slot ->
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
            Spacer(modifier = Modifier.weight(1f))
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onCardClick: () -> Unit = {},
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
        onClick = onCardClick,
        enabled = isSelectionMode,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else if (item.rarity != Rarity.Common) androidx.compose.foundation.BorderStroke(1.dp, rarityColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else if (canEquip || item.type !in listOf(ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag)) 
                MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

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
            
            if (!isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    when (item.type) {
                        ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag, ItemType.Ship -> {
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
}
