package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.alifeatseammo.data.model.AuctionListing
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Item
import com.alifeatseammo.data.model.ItemType
import com.alifeatseammo.ui.UIActionState
import com.alifeatseammo.ui.components.ActionOverlay
import com.alifeatseammo.ui.components.getRarityColor
import com.alifeatseammo.ui.components.getItemEmoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(
    character: Character,
    listings: List<AuctionListing>,
    actionState: UIActionState,
    onListButtonClick: (Item, Long) -> Unit,
    onBuyButtonClick: (AuctionListing) -> Unit,
    onCancelButtonClick: (AuctionListing) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Browse", "My Inventory", "My Listings")

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ItemType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auction House") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = "💰 ${character.gold}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("Search Items") },
                    singleLine = true
                )

                val categories = ItemType.entries
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    val myListingCount = listings.count { it.sellerId == character.id }
                    val filteredListings = listings.filter { listing ->
                        val matchesSearch = listing.item.name.contains(searchText, ignoreCase = true)
                        val matchesCategory = selectedCategory == null || listing.item.type == selectedCategory
                        matchesSearch && matchesCategory
                    }
                    BrowseListings(character, filteredListings, myListingCount, onBuyButtonClick)
                }
                1 -> {
                    MyInventory(character, onListButtonClick)
                }
                2 -> MyListings(character, listings, onCancelButtonClick)
            }
        }
        
        if (actionState is UIActionState.Loading) {
            ActionOverlay(actionState)
        }
    }
}

@Composable
fun BrowseListings(
    character: Character,
    listings: List<AuctionListing>,
    myListingCount: Int,
    onBuyButtonClick: (AuctionListing) -> Unit
) {
    val otherListings = listings.filter { it.sellerId != character.id }
    
    if (otherListings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text(
                    text = if (listings.isEmpty()) "No listings found matching your criteria." 
                           else "No other players have listings currently.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                if (myListingCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Note: You have $myListingCount active listings, but you cannot buy your own items. They are shown in 'My Listings'.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                if (listings.isEmpty()) {
                    Text(
                        text = "Try clearing filters or check back later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(otherListings) { listing ->
                AuctionListingCard(listing, onBuyButtonClick, "Buy")
            }
        }
    }
}

@Composable
fun MyInventory(
    character: Character,
    onListButtonClick: (Item, Long) -> Unit
) {
    var showListDialog by remember { mutableStateOf<Item?>(null) }
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ItemType?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text("Search My Inventory") },
            singleLine = true
        )

        val categories = ItemType.entries
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.name) }
                )
            }
        }

        val filteredInventory = character.inventory.filter { item ->
            val matchesSearch = item.name.contains(searchText, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.type == selectedCategory
            matchesSearch && matchesCategory
        }

        if (filteredInventory.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (character.inventory.isEmpty()) "Your inventory is empty." else "No items match your search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                items(filteredInventory) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = getRarityColor(item.rarity)
                                )
                                Text(text = item.type.name, style = MaterialTheme.typography.bodySmall)
                                if (item.quantity > 1) {
                                    Text(text = "Qty: ${item.quantity}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Button(onClick = { showListDialog = item }) {
                                Text("List")
                            }
                        }
                    }
                }
            }
        }
    }

    showListDialog?.let { item ->
        var priceText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showListDialog = null },
            title = { Text("List ${item.name}") },
            text = {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) priceText = it },
                    label = { Text("Price (Gold)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = priceText.toLongOrNull() ?: 0L
                        if (price > 0) {
                            onListButtonClick(item, price)
                            showListDialog = null
                        }
                    },
                    enabled = priceText.isNotEmpty()
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showListDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MyListings(
    character: Character,
    listings: List<AuctionListing>,
    onCancelButtonClick: (AuctionListing) -> Unit
) {
    val myListings = listings.filter { it.sellerId == character.id }
    if (myListings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "You have no active listings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(myListings) { listing ->
                AuctionListingCard(listing, onCancelButtonClick, "Cancel")
            }
        }
    }
}

@Composable
fun AuctionListingCard(
    listing: AuctionListing,
    onButtonClick: (AuctionListing) -> Unit,
    buttonText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item Icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getRarityColor(listing.item.rarity).copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = getItemEmoji(listing.item.type), fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listing.item.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getRarityColor(listing.item.rarity)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Seller: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = listing.sellerName.ifBlank { "Unknown Player" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${listing.price} G",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { onButtonClick(listing) },
                        modifier = Modifier.height(36.dp),
                        colors = if (buttonText == "Cancel") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                 else ButtonDefaults.buttonColors()
                    ) {
                        Text(buttonText, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Stats Preview Ribbon (Senior Dev Polish)
            val stats = listing.item.statBonus
            val statStrings = mutableListOf<String>()
            if (stats.strength > 0) statStrings.add("STR +${stats.strength.toInt()}")
            if (stats.swordsmanship > 0) statStrings.add("SWD +${stats.swordsmanship.toInt()}")
            if (stats.gunslinging > 0) statStrings.add("GUN +${stats.gunslinging.toInt()}")
            if (stats.endurance > 0) statStrings.add("END +${stats.endurance.toInt()}")
            if (stats.agility > 0) statStrings.add("AGI +${stats.agility.toInt()}")

            if (statStrings.isNotEmpty() || listing.item.description.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                if (statStrings.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        statStrings.forEach { stat ->
                            Surface(
                                color = Color(0xFF81C784).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = stat,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = listing.item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}
