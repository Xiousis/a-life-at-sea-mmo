package com.alifeatseammo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.alifeatseammo.ui.MainScaffold
import com.alifeatseammo.ui.CharacterState
import com.alifeatseammo.ui.GameViewModel
import com.alifeatseammo.ui.AuthViewModel
import com.alifeatseammo.ui.CombatViewModel
import com.alifeatseammo.ui.TravelViewModel
import com.alifeatseammo.ui.SocialViewModel
import com.alifeatseammo.ui.EconomyViewModel
import com.alifeatseammo.ui.PlayerProfileViewModel
import com.alifeatseammo.ui.screens.*
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
                val errorMsg by viewModel.errorMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(errorMsg) {
                    errorMsg?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearErrorMessage()
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
                                    onLogout = { authViewModel.signOut() }
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
