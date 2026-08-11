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
            onValueChange = { name = it },
            label = { Text("Character Name") },
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
                if (name.isNotBlank()) {
                    onCharacterCreated(name, selectedGender, selectedRace)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Sail")
        }
    }
}
