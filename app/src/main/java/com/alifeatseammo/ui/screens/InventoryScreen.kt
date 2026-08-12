package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onEquipItem: (Item) -> Unit,
    onUnequipItem: (String) -> Unit,
    onUseItem: (Item) -> Unit,
    onCookItem: (Item) -> Unit,
    onSellItem: (Item) -> Unit,
    onBackClick: () -> Unit
) {
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
            Text(
                text = "EQUIPMENT",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            EquipmentGrid(character.equipment, onUnequipItem)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Inventory Section
            Text(
                text = "BACKPACK (${character.inventory.size})",
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(character.inventory) { item ->
                        InventoryItemCard(
                            item = item,
                            onEquip = { onEquipItem(item) },
                            onUse = { onUseItem(item) },
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
fun EquipmentGrid(equipment: Map<String, Item?>, onUnequip: (String) -> Unit) {
    val slots = listOf("Weapon", "Armor", "Accessory")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        slots.forEach { slot ->
            EquipmentSlot(
                slotName = slot,
                item = equipment[slot],
                modifier = Modifier.weight(1f),
                onUnequip = { onUnequip(slot) }
            )
        }
    }
}

@Composable
fun EquipmentSlot(slotName: String, item: Item?, modifier: Modifier = Modifier, onUnequip: () -> Unit) {
    OutlinedCard(
        modifier = modifier.height(100.dp),
        onClick = { if (item != null) onUnequip() },
        enabled = item != null,
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
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (item.rarity != Rarity.Common) androidx.compose.foundation.BorderStroke(1.dp, rarityColor) else null
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
                Text(text = item.description, style = MaterialTheme.typography.bodySmall)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                when (item.type) {
                    ItemType.Weapon, ItemType.Armor, ItemType.Accessory -> {
                        Button(onClick = onEquip, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Text("Equip", fontSize = 12.sp)
                        }
                    }
                    ItemType.Consumable, ItemType.Food -> {
                        Button(onClick = onUse, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(if (item.type == ItemType.Food) "Eat" else "Use", fontSize = 12.sp)
                        }
                    }
                    ItemType.Fish -> {
                        Button(onClick = onCook, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Text("Cook", fontSize = 12.sp)
                        }
                    }
                    else -> {}
                }
                TextButton(onClick = onSell, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Sell (${item.price / 2} G)", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
