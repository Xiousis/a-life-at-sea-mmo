package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import com.alifeatseammo.data.repository.AuthResult

@Composable
fun LoginScreen(
    authResult: AuthResult?,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onGuestSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onClearError: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Join the Crew" else "Welcome Back",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                enabled = authResult !is AuthResult.Loading
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading
        )

        if (!isSignUp) {
            TextButton(
                onClick = { 
                    if (email.isNotBlank()) {
                        onForgotPassword(email)
                    } else {
                        // Perhaps a toast or simple error here, but for now just disable
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (authResult is AuthResult.Error) {
            Text(
                text = authResult.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (authResult is AuthResult.Success && !isSignUp) {
            Text(
                text = "Success! Please check your email if you requested a reset.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (isSignUp) {
                    onSignUp(email, password, username)
                } else {
                    onLogin(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading
        ) {
            if (authResult is AuthResult.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isSignUp) "Register" else "Login")
            }
        }

        TextButton(
            onClick = { 
                isSignUp = !isSignUp 
                onClearError()
            },
            enabled = authResult !is AuthResult.Loading
        ) {
            Text(if (isSignUp) "Already have an account? Login" else "New here? Create an account")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onGuestSignIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = authResult !is AuthResult.Loading
        ) {
            Text("Play as Guest (Skip Login)")
        }
    }
}
