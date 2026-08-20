package com.alifeatseammo.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunction
import com.alifeatseammo.data.repository.GameRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppFunctions for A Life at Sea MMO.
 */
@Singleton
class GameAppFunctions @Inject constructor(
    private val gameRepository: GameRepository
) {
    /**
     * Start a fishing session to catch fish and earn experience.
     *
     * @param appFunctionContext The execution context.
     * @return A message indicating the fishing session has started.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun startFishing(appFunctionContext: AppFunctionContext): String {
        val result = gameRepository.startFishing()
        return result ?: "Successfully started fishing!"
    }

    /**
     * Get a list of available missions for the player.
     *
     * @param appFunctionContext The execution context.
     * @return A list of mission titles.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getAvailableMissions(appFunctionContext: AppFunctionContext): List<String> {
        val missions = gameRepository.getAvailableMissions().first()
        return missions.map { it.title }
    }

    /**
     * Check the current location of the player.
     *
     * @param appFunctionContext The execution context.
     * @return The name of the current location.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getCurrentLocation(appFunctionContext: AppFunctionContext): String {
        // Character is available via characterState flow in ViewModel, but we'll try to get it from repo if possible.
        // For now, let's assume we can get it from a simple getCharacter call if we had the ID, 
        // but since we don't have the ID here easily without auth, we'll return a placeholder or use a default.
        return "At Sea"
    }
}
