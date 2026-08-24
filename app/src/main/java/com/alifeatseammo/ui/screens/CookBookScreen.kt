package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Recipe
import com.alifeatseammo.ui.UIActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookBookScreen(
    character: Character,
    recipes: List<Recipe>,
    actionState: UIActionState,
    onCook: (Recipe) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cook Book") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cooking Level: ${"%.1f".format(character.professionStats.cooking)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recipes) { recipe ->
                        RecipeItem(
                            recipe = recipe,
                            characterLevel = character.professionStats.cooking,
                            isLoading = actionState is UIActionState.Loading,
                            onCook = { onCook(recipe) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeItem(
    recipe: Recipe,
    characterLevel: Double,
    isLoading: Boolean,
    onCook: () -> Unit
) {
    val isLocked = characterLevel < recipe.levelRequirement
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary
                )
                if (isLocked) {
                    Text(
                        text = "LOCKED (Lv.${recipe.levelRequirement})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ingredients:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            recipe.ingredients.forEach { ingredient ->
                Text(
                    text = "• ${ingredient.quantity}x ${ingredient.itemId.replace("_", " ").capitalize()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onCook,
                modifier = Modifier.align(Alignment.End),
                enabled = !isLocked && !isLoading
            ) {
                Text(if (isLocked) "Low Level" else "Cook")
            }
        }
    }
}
