package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.ui.AuthViewModel
import com.alifeatseammo.ui.GameViewModel

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    authViewModel: AuthViewModel,
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
            val isAdmin by viewModel.isAdmin.collectAsState()
            if (isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.seedWorld() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Seed World Data (Admin)")
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
