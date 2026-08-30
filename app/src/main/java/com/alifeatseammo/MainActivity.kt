package com.alifeatseammo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import javax.inject.Inject
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.alifeatseammo.ui.AuthViewModel
import com.alifeatseammo.ui.CharacterState
import com.alifeatseammo.ui.CombatViewModel
import com.alifeatseammo.ui.GameViewModel
import com.alifeatseammo.ui.MainScaffold
import com.alifeatseammo.ui.MusicViewModel
import com.alifeatseammo.ui.screens.CharacterCreationScreen
import com.alifeatseammo.ui.screens.CombatScreen
import com.alifeatseammo.ui.screens.LoginScreen
import com.alifeatseammo.ui.screens.TravelingScreen
import com.alifeatseammo.ui.theme.ALifeAtSeaMMOTheme
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var musicManager: com.alifeatseammo.util.MusicManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Handle music lifecycle globally
        ProcessLifecycleOwner.get().lifecycle.addObserver(musicManager)
        
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                Log.d("AppCheck", "Using Debug Provider. Check Logcat for 'DebugAppCheckProvider' to find your token for the Firebase Console.")
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )

        enableEdgeToEdge()
        setContent {
            ALifeAtSeaMMOTheme {
                val navController = rememberNavController()
                
                // Initialize Music ViewModel (starts playing music automatically)
                hiltViewModel<MusicViewModel>()

                val viewModel: GameViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                val combatViewModel: CombatViewModel = hiltViewModel()

                val characterState by viewModel.characterState.collectAsState()
                val user by viewModel.currentUser.collectAsState()
                
                val errorMsg by viewModel.errorMessage.collectAsState()
                val combatErrorMsg by combatViewModel.errorMessage.collectAsState()
                
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(errorMsg, combatErrorMsg) {
                    val message = errorMsg ?: combatErrorMsg
                    message?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearErrorMessage()
                        combatViewModel.clearErrorMessage()
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
                        ) {
                            authViewModel.clearAuthResult()
                        }
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
                                ) {
                                    authViewModel.signOut()
                                }
                            }
                            is CharacterState.Loaded -> {
                                MainScaffold(
                                    navController = navController,
                                    currentChar = state.character,
                                    snackbarHostState = snackbarHostState,
                                )
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
}
