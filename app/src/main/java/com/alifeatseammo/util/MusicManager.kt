package com.alifeatseammo.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    private var mediaPlayer: MediaPlayer? = null
    private var currentResId: Int = -1
    private var isPausedAutomatically: Boolean = false
    private var volume: Float = 0.5f

    fun play(resId: Int, loop: Boolean = true) {
        if (currentResId == resId && mediaPlayer?.isPlaying == true) return

        try {
            stop()
            
            // Check if resource exists before attempting to create
            val resName = try { context.resources.getResourceEntryName(resId) } catch (e: Exception) { null }
            if (resName == null) {
                Log.w("MusicManager", "Resource ID $resId not found. Skipping play.")
                return
            }

            mediaPlayer = MediaPlayer.create(context, resId).apply {
                isLooping = loop
                setVolume(volume, volume)
                start()
            }
            currentResId = resId
            isPausedAutomatically = false
            Log.d("MusicManager", "Playing: $resName")
        } catch (e: Exception) {
            Log.e("MusicManager", "Error playing music", e)
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isPausedAutomatically) {
            resume()
            isPausedAutomatically = false
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (mediaPlayer?.isPlaying == true) {
            pause()
            isPausedAutomatically = true
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            currentResId = -1
        } catch (e: Exception) {
            Log.e("MusicManager", "Error stopping music", e)
        }
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
    }

    fun release() {
        stop()
    }
}
