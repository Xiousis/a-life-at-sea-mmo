package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TavernScreen(
    character: Character,
    onBackClick: () -> Unit
) {
    var rumorText by remember { mutableStateOf("The tavern is lively tonight. You might hear something interesting if you stay a while.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Salty Dog Tavern") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🍻", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = rumorText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    rumorText = "You hear whispers of a legendary sea monster near Crystal Cove..."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Listen for Rumors")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = { 
                    rumorText = "You buy a round for the tavern. Everyone cheers! (Spent 50 Gold)"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = character.gold >= 50
            ) {
                Text("Buy a Round (50 Gold)")
            }
        }
    }
}
