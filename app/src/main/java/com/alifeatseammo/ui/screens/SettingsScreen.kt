package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alifeatseammo.ui.AuthViewModel
import com.alifeatseammo.ui.GameViewModel

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    authViewModel: AuthViewModel,
    onAdminPanelClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var showLogoutWarning by remember { mutableStateOf(false) }
    
    if (showLogoutWarning) {
        AlertDialog(
            onDismissRequest = { showLogoutWarning = false },
            title = { Text("Logout Warning") },
            text = { Text("You are currently using a guest account. If you logout without upgrading, your progress will be lost forever. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutWarning = false
                    authViewModel.signOut()
                }) { Text("Logout Anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutWarning = false }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Settings Screen")
            val character by viewModel.character.collectAsState()
            val isAdmin by viewModel.isAdmin.collectAsState()
            
            character?.let { char ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Debug Info", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Location: ${char.currentLocation}")
                        val currentLocInfo by viewModel.currentLocationInfo.collectAsState()
                        currentLocInfo?.let { loc ->
                            Text("Coords: (${loc.x}, ${loc.y})")
                        } ?: Text("Coords: Unknown")
                        
                        val allLocs by viewModel.locations.collectAsState()
                        Text("Total Islands: ${allLocs.size}")
                        if (allLocs.isNotEmpty()) {
                            Text("Last Island: ${allLocs.last().name}")
                        }
                    }
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAdminPanelClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Admin Panel")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (authViewModel.currentUser.value?.isAnonymous == true) {
                    showLogoutWarning = true
                } else {
                    authViewModel.signOut()
                }
            }) {
                Text("Logout")
            }
            Button(onClick = onBackClick) {
                Text("Back")
            }
        }
    }
}
