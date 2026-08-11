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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    onCharacterCreated: (String, Gender, Race) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.Male) }
    var selectedRace by remember { mutableStateOf(Race.Human) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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

        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it
                error = null
            },
            label = { Text("Character Name") },
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Race", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedRace.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Race") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Race.values().forEach { race ->
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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val validationError = validateName(name)
                if (validationError == null) {
                    onCharacterCreated(name.trim(), selectedGender, selectedRace)
                } else {
                    error = validationError
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Sail")
        }
    }
}
