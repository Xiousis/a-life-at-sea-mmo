package com.alifeatseammo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alifeatseammo.ui.FishingState
import com.alifeatseammo.ui.FishingViewModel

@Composable
fun FishingScreen(
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: FishingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val fishPos by viewModel.fishPosition.collectAsState()
    val barPos by viewModel.barPosition.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val caughtFish by viewModel.caughtFish.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FISHING",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            viewModel.onBarPress(true)
                            tryAwaitRelease()
                            viewModel.onBarPress(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                FishingState.IDLE -> {
                    Button(onClick = { viewModel.startFishing() }) {
                        Text("Cast Line")
                    }
                }
                FishingState.CASTING -> {
                    CircularProgressIndicator()
                    Text("Casting...", modifier = Modifier.padding(top = 80.dp))
                }
                FishingState.WAITING -> {
                    Text("Waiting for a bite...", style = MaterialTheme.typography.bodyLarge)
                }
                FishingState.HOOKED -> {
                    FishingMiniGame(fishPos, barPos, progress)
                }
                FishingState.SUCCESS -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨ CAUGHT! ✨", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(caughtFish?.name ?: "Unknown Fish", style = MaterialTheme.typography.headlineSmall)
                        Text("${caughtFish?.rarity} • ${caughtFish?.value} Gold", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FishingState.FAILURE -> {
                    Text("The fish got away...", fontSize = 24.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onBackClick) {
            Text("Back")
        }
    }
}

@Composable
fun FishingMiniGame(fishPos: Float, barPos: Float, progress: Float) {
    Row(
        modifier = Modifier
            .height(400.dp)
            .width(100.dp)
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // Tension/Progress Bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress / 100f)
                    .align(Alignment.BottomCenter)
                    .background(if (progress > 80) Color.Red else Color.Green)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Fishing Line / Game Area
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.1f))
        ) {
            // The Catching Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.15f) // 15% of height
                    .offset(y = (barPos / 100f * 400).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Green.copy(alpha = 0.5f))
            )
            
            // The Fish
            Text(
                text = "🐟",
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (fishPos / 100f * 400).dp)
            )
        }
    }
}
