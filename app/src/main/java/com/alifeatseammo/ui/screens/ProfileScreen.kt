package com.alifeatseammo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alifeatseammo.data.model.Character
import com.alifeatseammo.data.model.Crew
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    character: Character,
    crew: Crew? = null,
    isOwnProfile: Boolean = false,
    onBackClick: () -> Unit,
    onAttackClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onViewCrewClick: () -> Unit = {},
    onAddFriendClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "☠ ${character.name.uppercase()}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Level ${character.level}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = character.race.name,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatRow("Bounty:", String.format(Locale.getDefault(), "%,d", character.bounty), color = MaterialTheme.colorScheme.error)
                ProfileStatRow("Faction:", character.faction.name)
                if (character.infamy > 0) {
                    ProfileStatRow("Infamy:", "${character.infamy}/100", color = MaterialTheme.colorScheme.error)
                }
                ProfileStatRow("Crew:", crew?.name ?: "None")
                ProfileStatRow("Title:", character.title)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ProfileStatRow("PvP:", "${character.pvpWins}W / ${character.pvpLosses}L")
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (!isOwnProfile) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAttackClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("ATTACK", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onMessageClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("MESSAGE", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onViewCrewClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall,
                        enabled = character.crewId != null
                    ) {
                        Text("VIEW CREW", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAddFriendClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("ADD FRIEND", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
