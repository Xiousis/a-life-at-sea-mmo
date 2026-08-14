package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.AuctionListing
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Item
import com.alifeatseammo.ui.UIActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(
    character: Character,
    listings: List<AuctionListing>,
    actionState: UIActionState,
    onListButtonClick: (Item, Int) -> Unit,
    onBuyButtonClick: (AuctionListing) -> Unit,
    onCancelButtonClick: (AuctionListing) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Browse", "My Inventory", "My Listings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auction House") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
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

            when (selectedTab) {
                0 -> BrowseListings(character, listings, onBuyButtonClick)
                1 -> MyInventory(character, onListButtonClick)
                2 -> MyListings(character, listings, onCancelButtonClick)
            }
        }
    }
}

@Composable
fun BrowseListings(
    character: Character,
    listings: List<AuctionListing>,
    onBuyButtonClick: (AuctionListing) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(listings.filter { it.sellerId != character.id }) { listing ->
            AuctionListingCard(listing, onBuyButtonClick, "Buy")
        }
    }
}

@Composable
fun MyInventory(
    character: Character,
    onListButtonClick: (Item, Int) -> Unit
) {
    var showListDialog by remember { mutableStateOf<Item?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(character.inventory) { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = item.type.name, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { showListDialog = item }) {
                        Text("List")
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
                        val price = priceText.toIntOrNull() ?: 0
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
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(listings.filter { it.sellerId == character.id }) { listing ->
            AuctionListingCard(listing, onCancelButtonClick, "Cancel")
        }
    }
}

@Composable
fun AuctionListingCard(
    listing: AuctionListing,
    onButtonClick: (AuctionListing) -> Unit,
    buttonText: String
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = listing.item.name, style = MaterialTheme.typography.titleLarge)
                Text(text = "Seller: ${listing.sellerName}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${listing.price} Gold", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }
            Button(onClick = { onButtonClick(listing) }) {
                Text(buttonText)
            }
        }
    }
}
