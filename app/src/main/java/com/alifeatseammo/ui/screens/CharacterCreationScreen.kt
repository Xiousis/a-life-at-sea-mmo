package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alifeatseammo.data.model.Gender
import com.alifeatseammo.data.model.Race
import com.alifeatseammo.data.repository.AuthResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    creationResult: AuthResult?,
    onCharacterCreated: (String, Gender, Race) -> Unit,
    onClearError: () -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.Male) }
    var selectedRace by remember { mutableStateOf(Race.Human) }
    var expanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val isLoading = creationResult is AuthResult.Loading
    val serverError = (creationResult as? AuthResult.Error)?.message

    fun validateName(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length < 3) return "Name too short (min 3)"
        if (trimmed.length > 16) return "Name too long (max 16)"
        if (!trimmed.all { it.isLetterOrDigit() || it == '_' }) return "Only letters, numbers, and underscores allowed"
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Begin Your Adventure", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        if (serverError != null) {
            Text(
                text = serverError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = onClearError, modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Dismiss Server Error")
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it
                localError = null
            },
            label = { Text("Character Name") },
            isError = localError != null || serverError != null,
            supportingText = { 
                localError?.let { Text(it) } ?: serverError?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Gender", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Gender.values().forEach { gender ->
                FilterChip(
                    selected = selectedGender == gender,
                    onClick = { selectedGender = gender },
                    label = { Text(gender.name) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Race", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(
            expanded = expanded && !isLoading,
            onExpandedChange = { if (!isLoading) expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedRace.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Race") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                enabled = !isLoading
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Race.entries.forEach { race ->
                    DropdownMenuItem(
                        text = { Text(race.name) },
                        onClick = {
                            selectedRace = race
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Race Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = selectedRace.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = selectedRace.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                val boosts = selectedRace.getStatBoosts()
                val boostList = mutableListOf<String>()
                if (boosts.strength > 0) boostList.add("Strength +${boosts.strength.toInt()}")
                if (boosts.endurance > 0) boostList.add("Endurance +${boosts.endurance.toInt()}")
                if (boosts.agility > 0) boostList.add("Agility +${boosts.agility.toInt()}")
                if (boosts.perception > 0) boostList.add("Perception +${boosts.perception.toInt()}")
                if (boosts.willpower > 0) boostList.add("Willpower +${boosts.willpower.toInt()}")
                if (boosts.luck > 0) boostList.add("Luck +${boosts.luck.toInt()}")

                if (boostList.isNotEmpty()) {
                    Text(
                        text = "Stat Boosts: ${boostList.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    val validationError = validateName(name)
                    if (validationError == null) {
                        onCharacterCreated(name.trim(), selectedGender, selectedRace)
                    } else {
                        localError = validationError
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Sail")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onLogout,
            enabled = !isLoading
        ) {
            Text("Sign Out / Back to Login")
        }
    }
}
