package com.alifeatseammo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.alifeatseammo.util.MusicManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AlifeAtSeaApplication : Application() {
    @Inject lateinit var musicManager: MusicManager

    override fun onCreate() {
        super.onCreate()
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(musicManager)
        } catch (e: Exception) {
            // Fallback or log if process lifecycle is not available
        }
    }
}
