package com.alifeatseammo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.alifeatseammo.ui.AuthViewModel
import com.alifeatseammo.ui.CharacterState
import com.alifeatseammo.ui.CombatViewModel
import com.alifeatseammo.ui.EconomyViewModel
import com.alifeatseammo.ui.GameViewModel
import com.alifeatseammo.ui.MainScaffold
import com.alifeatseammo.ui.PlayerProfileViewModel
import com.alifeatseammo.ui.SocialViewModel
import com.alifeatseammo.ui.TravelViewModel
import com.alifeatseammo.ui.screens.CharacterCreationScreen
import com.alifeatseammo.ui.screens.CombatScreen
import com.alifeatseammo.ui.screens.LoginScreen
import com.alifeatseammo.ui.screens.TravelingScreen
import com.alifeatseammo.ui.theme.ALifeAtSeaMMOTheme
import com.alifeatseammo.util.MusicManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ALifeAtSeaMMOTheme {
                val navController = rememberNavController()
                val viewModel: GameViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                val combatViewModel: CombatViewModel = hiltViewModel()
                val travelViewModel: TravelViewModel = hiltViewModel()
                val socialViewModel: SocialViewModel = hiltViewModel()
                val economyViewModel: EconomyViewModel = hiltViewModel()
                val profileViewModel: PlayerProfileViewModel = hiltViewModel()

                val characterState by viewModel.characterState.collectAsState()
                val user by viewModel.currentUser.collectAsState()
                val currentLocation by viewModel.currentLocationInfo.collectAsState()
                val errorMsg by viewModel.errorMessage.collectAsState()
                val combatErrorMsg by combatViewModel.errorMessage.collectAsState()
                val travelErrorMsg by travelViewModel.errorMessage.collectAsState()
                val socialErrorMsg by socialViewModel.errorMessage.collectAsState()
                val economyErrorMsg by economyViewModel.errorMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                val context = androidx.compose.ui.platform.LocalContext.current
                val targetTrackResId = remember(user, characterState, currentLocation) {
                    if (user == null) {
                        R.raw.life_at_sea_menu_sound
                    } else {
                        when (val state = characterState) {
                            is CharacterState.Loaded -> {
                                val char = state.character
                                when {
                                    char.travelState != null -> {
                                        R.raw.life_at_sea_traveling_music
                                    }
                                    currentLocation?.name?.startsWith("Navy Outpost", ignoreCase = true) == true -> {
                                        R.raw.navy_outpost_music
                                    }
                                    currentLocation?.name?.equals("Pirate's Den", ignoreCase = true) == true -> {
                                        R.raw.pirate_den_music
                                    }
                                    else -> {
                                        R.raw.life_at_sea_menu_sound
                                    }
                                }
                            }
                            else -> {
                                R.raw.life_at_sea_menu_sound
                            }
                        }
                    }
                }

                LaunchedEffect(targetTrackResId) {
                    MusicManager.play(context, targetTrackResId)
                }

                LaunchedEffect(errorMsg, combatErrorMsg, travelErrorMsg, socialErrorMsg, economyErrorMsg) {
                    val message = errorMsg ?: combatErrorMsg ?: travelErrorMsg ?: socialErrorMsg ?: economyErrorMsg
                    message?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearErrorMessage()
                        combatViewModel.clearErrorMessage()
                        travelViewModel.clearErrorMessage()
                        socialViewModel.clearErrorMessage()
                        economyViewModel.clearErrorMessage()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (user == null) {
                        val authResult by authViewModel.authResult.collectAsState()
                        LoginScreen(
                            authResult = authResult,
                            onLogin = { email, password -> authViewModel.signIn(email, password) },
                            onSignUp = { email, password, username -> authViewModel.signUp(email, password, username) },
                            onGuestSignIn = { authViewModel.signIn() },
                            onForgotPassword = { authViewModel.resetPassword(it) },
                            onClearError = { authViewModel.clearAuthResult() }
                        )
                    } else {
                        when (val state = characterState) {
                            is CharacterState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is CharacterState.NoCharacter -> {
                                val creationResult by authViewModel.createCharacterResult.collectAsState()
                                CharacterCreationScreen(
                                    creationResult = creationResult,
                                    onCharacterCreated = { name, gender, race ->
                                        authViewModel.createCharacter(name, gender, race)
                                    },
                                    onClearError = { authViewModel.clearCreateCharacterResult() },
                                    onLogout = { authViewModel.signOut() },
                                )
                            }
                            is CharacterState.Loaded -> {
                                val currentChar = state.character
                                if (currentChar.combatState != null) {
                                    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { p ->
                                        Box(Modifier.padding(p)) {
                                            CombatScreen(
                                                character = currentChar,
                                                onActionClick = { action, techId, itemId -> combatViewModel.combatAction(action, techId, itemId) }
                                            )
                                        }
                                    }
                                } else if (currentChar.travelState != null) {
                                    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { p ->
                                        Box(Modifier.padding(p)) {
                                            TravelingScreen(
                                                character = currentChar,
                                                onCompleteClick = { viewModel.finishTravel() }
                                            )
                                        }
                                    }
                                } else {
                                    MainScaffold(
                                        navController = navController,
                                        currentChar = currentChar,
                                        viewModel = viewModel,
                                        authViewModel = authViewModel,
                                        combatViewModel = combatViewModel,
                                        travelViewModel = travelViewModel,
                                        socialViewModel = socialViewModel,
                                        economyViewModel = economyViewModel,
                                        profileViewModel = profileViewModel,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                            }
                            is CharacterState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { authViewModel.signOut() }) {
                                            Text("Back to Login")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pause()
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.release()
    }
}
