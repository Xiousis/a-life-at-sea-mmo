package com.alifeatseammo

import android.app.Application
import androidx.appfunctions.AppFunctionConfiguration
import androidx.lifecycle.ProcessLifecycleOwner
import com.alifeatseammo.appfunctions.GameAppFunctions
import com.alifeatseammo.util.MusicManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AlifeAtSeaApplication : Application(), AppFunctionConfiguration.Provider {
    @Inject lateinit var musicManager: MusicManager
    @Inject lateinit var gameAppFunctions: GameAppFunctions

    override val appFunctionConfiguration: AppFunctionConfiguration =
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GameAppFunctions::class.java) { gameAppFunctions }
            .build()

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(musicManager)
        } catch (e: Exception) {
            // Fallback or log if process lifecycle is not available
        }
    }
}
