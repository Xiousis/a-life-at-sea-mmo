package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.repository.AuthResult

@Composable
fun UpgradeAccountScreen(
    authResult: AuthResult?,
    onUpgrade: (String, String) -> Unit,
    onBackClick: () -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Upgrade Guest Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Secure your progress by linking an email.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (authResult is AuthResult.Error) {
            Text(
                text = authResult.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (password == confirmPassword) {
                    onUpgrade(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading && email.isNotBlank() && password.length >= 6
        ) {
            if (authResult is AuthResult.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Upgrade Account")
            }
        }

        TextButton(
            onClick = {
                onClearError()
                onBackClick()
            },
            enabled = authResult !is AuthResult.Loading
        ) {
            Text("Go Back")
        }
    }

    if (authResult is AuthResult.Success) {
        LaunchedEffect(Unit) {
            onBackClick()
        }
    }
}
