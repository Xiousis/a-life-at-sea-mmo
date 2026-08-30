package com.alifeatseammo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.*
import com.alifeatseammo.ui.components.ThemedCard
import com.alifeatseammo.ui.components.getRarityColor
import com.alifeatseammo.ui.components.getItemEmoji
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)
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
    val navigator = rememberListDetailPaneScaffoldNavigator<Item>()
    val coroutineScope = rememberCoroutineScope()
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

    val selectedItem = navigator.currentDestination?.contentKey
    LaunchedEffect(character.inventory, selectedItem) {
        if (selectedItem != null && character.inventory.none { it.id == selectedItem.id }) {
            coroutineScope.launch { navigator.navigateBack() }
        }
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
                    IconButton(onClick = {
                        if (navigator.canNavigateBack()) {
                            coroutineScope.launch { navigator.navigateBack() }
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val backHandler = navigator.canNavigateBack()
        androidx.activity.compose.BackHandler(enabled = backHandler) {
            coroutineScope.launch { navigator.navigateBack() }
        }

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                AnimatedPane(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                    InventoryListPane(
                        character = character,
                        filteredItems = filteredItems,
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        isLoading = isLoading,
                        isBulkSellMode = isBulkSellMode,
                        onBulkSellModeChange = { isBulkSellMode = it },
                        selectedItemIds = selectedItemIds,
                        onItemClick = { item ->
                            if (isBulkSellMode) {
                                if (selectedItemIds.contains(item.id)) selectedItemIds.remove(item.id)
                                else selectedItemIds.add(item.id)
                            } else {
                                coroutineScope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                                }
                            }
                        },
                        onUnequipItem = onUnequipItem,
                        onSellSelected = {
                            filteredItems.filter { selectedItemIds.contains(it.id) }.forEach { onSellItem(it) }
                            selectedItemIds.clear()
                            isBulkSellMode = false
                        }
                    )
                }
            },
            detailPane = {
                AnimatedPane(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                    navigator.currentDestination?.contentKey?.let { item ->
                        ItemDetailPane(
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
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select an item to view details", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                }
            }
        )
    }
}

@Composable
fun InventoryListPane(
    character: Character,
    filteredItems: List<Item>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: ItemType?,
    onCategoryChange: (ItemType?) -> Unit,
    isLoading: Boolean,
    isBulkSellMode: Boolean,
    onBulkSellModeChange: (Boolean) -> Unit,
    selectedItemIds: MutableList<String>,
    onItemClick: (Item) -> Unit,
    onUnequipItem: (String) -> Unit,
    onSellSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "EQUIPMENT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            EquipmentGrid(character.equipment, onUnequipItem, isLoading)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "BACKPACK (${character.inventory.size}/${character.calculateMaxCapacity()})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isBulkSellMode) {
                        TextButton(
                            onClick = { 
                                selectedItemIds.clear()
                                selectedItemIds.addAll(filteredItems.map { it.id })
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { Text("All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) }
                        
                        TextButton(
                            onClick = { selectedItemIds.clear() },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { Text("None", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Bulk Sell", style = MaterialTheme.typography.labelSmall)
                    Switch(checked = isBulkSellMode, onCheckedChange = onBulkSellModeChange, modifier = Modifier.scale(0.7f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchText, onValueChange = onSearchChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { if (searchText.isNotEmpty()) IconButton(onClick = { onSearchChange("") }) { Text("✕") } },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item { FilterChip(selected = selectedCategory == null, onClick = { onCategoryChange(null) }, label = { Text("All") }) }
                items(ItemType.entries) { type -> FilterChip(selected = selectedCategory == type, onClick = { onCategoryChange(type) }, label = { Text(type.name) }) }
            }
        }
        
        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = if (character.inventory.isEmpty()) "Your backpack is empty." else "No items match your search.", color = Color.Gray)
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { item ->
                        InventoryItemSummaryCard(
                            item = item,
                            isSelected = selectedItemIds.contains(item.id),
                            onClick = { onItemClick(item) }
                        )
                    }
                }
                
                if (isBulkSellMode && selectedItemIds.isNotEmpty()) {
                    Button(
                        onClick = onSellSelected,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("SELL SELECTED (${selectedItemIds.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemSummaryCard(item: Item, isSelected: Boolean, onClick: () -> Unit) {
    val rarityColor = getRarityColor(item.rarity)
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else rarityColor.copy(alpha = 0.3f)),
        colors = CardDefaults.outlinedCardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else rarityColor.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(getItemEmoji(item.type), fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun ItemDetailPane(
    item: Item,
    character: Character,
    isLoading: Boolean,
    onEquip: () -> Unit,
    onUse: () -> Unit,
    onCook: () -> Unit,
    onSell: () -> Unit
) {
    val rarityColor = getRarityColor(item.rarity)
    val canEquip = character.canEquip(item)
    val missingRequirements = character.getMissingRequirements(item)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        ThemedCard(borderColor = rarityColor) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(getItemEmoji(item.type), fontSize = 64.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = rarityColor)
                Text(item.rarity.name, style = MaterialTheme.typography.titleMedium, color = rarityColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats
                StatRow("Strength", item.statBonus.strength)
                StatRow("Endurance", item.statBonus.endurance)
                StatRow("Agility", item.statBonus.agility)
                StatRow("Perception", item.statBonus.perception)
                StatRow("Willpower", item.statBonus.willpower)
                StatRow("Luck", item.statBonus.luck)
                StatRow("Swordsmanship", item.statBonus.swordsmanship)
                StatRow("Gunslinging", item.statBonus.gunslinging)
                StatRow("Brawling", item.statBonus.brawling)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(item.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                
                if (missingRequirements.isNotEmpty()) {
                    Text("REQUIREMENTS NOT MET:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                    Text(missingRequirements.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (item.type) {
                        ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag, ItemType.Ship -> {
                            Button(onClick = onEquip, enabled = !isLoading && canEquip, modifier = Modifier.weight(1f)) {
                                Text("EQUIP")
                            }
                        }
                        ItemType.Consumable, ItemType.Food, ItemType.Artifact -> {
                            Button(onClick = onUse, enabled = !isLoading, modifier = Modifier.weight(1f)) {
                                Text(if (item.type == ItemType.Artifact) "AWAKEN" else "USE")
                            }
                        }
                        ItemType.Fish -> {
                            Button(onClick = onCook, enabled = !isLoading, modifier = Modifier.weight(1f)) {
                                Text("COOK")
                            }
                        }
                        else -> {}
                    }
                    Button(onClick = onSell, enabled = !isLoading, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("SELL (${item.price / 2} G)")
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: Double) {
    if (value != 0.0) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("+$value", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
        }
    }
}

@Composable
fun EquipmentGrid(equipment: Map<String, Item?>, onUnequip: (String) -> Unit, isLoading: Boolean) {
    val leftSlots = listOf("Helmet", "Armor", "Boots")
    val rightSlots = listOf("Accessory", "Bag", "Ship")
    val centerSlot = "Weapon"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            leftSlots.forEach { slot ->
                EquipmentSlot(
                    slotName = slot,
                    item = equipment[slot],
                    enabled = !isLoading,
                    onUnequip = { onUnequip(slot) }
                )
            }
        }
        
        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("☠", fontSize = 48.sp, modifier = Modifier.alpha(0.2f))
                EquipmentSlot(
                    slotName = centerSlot,
                    item = equipment[centerSlot],
                    modifier = Modifier.size(100.dp),
                    enabled = !isLoading,
                    onUnequip = { onUnequip(centerSlot) }
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rightSlots.forEach { slot ->
                EquipmentSlot(
                    slotName = slot,
                    item = equipment[slot],
                    enabled = !isLoading,
                    onUnequip = { onUnequip(slot) }
                )
            }
        }
    }
}

@Composable
fun EquipmentSlot(slotName: String, item: Item?, modifier: Modifier = Modifier, enabled: Boolean, onUnequip: () -> Unit) {
    val rarityColor = item?.let { getRarityColor(it.rarity) } ?: Color.Gray
    
    OutlinedCard(
        modifier = modifier.height(70.dp).fillMaxWidth(),
        onClick = { if (item != null) onUnequip() },
        enabled = item != null && enabled,
        border = if (item != null) BorderStroke(1.dp, rarityColor.copy(alpha = 0.5f)) else CardDefaults.outlinedCardBorder(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (item != null) rarityColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = slotName.uppercase(), 
                style = MaterialTheme.typography.labelSmall, 
                color = if (item != null) rarityColor else Color.Gray,
                fontWeight = FontWeight.Black,
                fontSize = 8.sp
            )
            if (item != null) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    maxLines = 2
                )
            } else {
                Text(text = "---", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.3f))
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
    val rarityColor = getRarityColor(item.rarity)
    val canEquip = character.canEquip(item)
    val missingRequirements = character.getMissingRequirements(item)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick,
        enabled = isSelectionMode,
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            brush = if (isSelected) Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                    else if (item.rarity != Rarity.Common) SolidColor(rarityColor.copy(alpha = 0.5f))
                    else SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else if (item.rarity != Rarity.Common) rarityColor.copy(alpha = 0.03f)
                else MaterialTheme.colorScheme.surface
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

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(rarityColor.copy(alpha = 0.1f))
                    .then(
                        if (item.rarity.ordinal >= Rarity.Epic.ordinal) {
                            Modifier.border(
                                width = 2.dp,
                                brush = Brush.radialGradient(
                                    colors = listOf(rarityColor.copy(alpha = 0.8f), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier.border(1.dp, rarityColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(getItemEmoji(item.type), fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (item.rarity != Rarity.Common) rarityColor else Color.Unspecified
                    )
                    if (item.rarity == Rarity.Mythic) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🔥", fontSize = 12.sp)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.rarity.name, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = rarityColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " • ${item.type.name}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                if (missingRequirements.isNotEmpty() && item.type in listOf(ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag)) {
                    Text(
                        text = "NEED: ${missingRequirements.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            if (!isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    when (item.type) {
                        ItemType.Weapon, ItemType.Armor, ItemType.Accessory, ItemType.Bag, ItemType.Ship -> {
                            Button(
                                onClick = onEquip,
                                enabled = !isLoading && canEquip,
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = if (!canEquip) ButtonDefaults.buttonColors(containerColor = Color.Gray) else ButtonDefaults.buttonColors()
                            ) {
                                Text("EQUIP", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        ItemType.Consumable, ItemType.Food, ItemType.Artifact -> {
                            Button(
                                onClick = onUse,
                                enabled = !isLoading,
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(when(item.type) {
                                    ItemType.Food -> "EAT"
                                    ItemType.Artifact -> "AWAKEN"
                                    else -> "USE"
                                }.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        ItemType.Fish -> {
                            Button(
                                onClick = onCook,
                                enabled = !isLoading,
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("COOK", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        else -> {}
                    }
                    TextButton(
                        onClick = onSell,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("${item.price / 2} G", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isLoading) Color.Gray else MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
