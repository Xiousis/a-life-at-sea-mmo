package com.alifeatseammo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.alifeatseammo.util.MusicManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AlifeAtSeaApplication : Application() {
    @Inject lateinit var musicManager: MusicManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase first
        try {
            FirebaseApp.initializeApp(this)
            
            // App Check initialization
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

            if (isDebug) {
                android.util.Log.d("AlifeAtSeaApplication", "Installing App Check DEBUG provider. Check Logcat for your debug token!")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AlifeAtSeaApplication", "Firebase/App Check initialization failed", e)
        }

        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(musicManager)
        } catch (e: Exception) {
            android.util.Log.e("AlifeAtSeaApplication", "Failed to add music observer", e)
        }
    }
}
