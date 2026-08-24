package com.alifeatseammo.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunction
import com.alifeatseammo.data.repository.AuthRepository
import com.alifeatseammo.data.repository.CrewRepository
import com.alifeatseammo.data.repository.GameRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppFunctions for A Life at Sea MMO.
 */
@Singleton
class GameAppFunctions @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val crewRepository: CrewRepository,
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
        val userId = authRepository.currentUser.value?.uid ?: return "Not logged in"
        val character = gameRepository.getCharacter(userId).firstOrNull()
        return character?.currentLocation ?: "Unknown Location"
    }

    /**
     * Check the status of your character, including HP, Energy, and Gold.
     *
     * @param appFunctionContext The execution context.
     * @return A status summary message.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkCharacterStatus(appFunctionContext: AppFunctionContext): String {
        val userId = authRepository.currentUser.value?.uid ?: return "Not logged in"
        val character = gameRepository.getCharacter(userId).firstOrNull() ?: return "Character not found"
        return "Character: ${character.name} | Level: ${character.level} | HP: ${character.hp}/${character.maxHp} | Gold: ${character.gold}"
    }

    /**
     * Check the status and upgrades of your current ship.
     *
     * @param appFunctionContext The execution context.
     * @return A ship status summary message.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkShipStatus(appFunctionContext: AppFunctionContext): String {
        val userId = authRepository.currentUser.value?.uid ?: return "Not logged in"
        val character = gameRepository.getCharacter(userId).firstOrNull() ?: return "Character not found"
        val ship = character.ship
        return "Ship: ${ship.name} | Hull: Lv.${ship.upgrades.hullLevel} | Sails: Lv.${ship.upgrades.sailLevel} | Cannons: Lv.${ship.upgrades.cannonLevel}"
    }

    /**
     * Check your crew's status and active perks.
     *
     * @param appFunctionContext The execution context.
     * @return A crew status summary message.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkCrewStatus(appFunctionContext: AppFunctionContext): String {
        val userId = authRepository.currentUser.value?.uid ?: return "Not logged in"
        val character = gameRepository.getCharacter(userId).firstOrNull() ?: return "Character not found"
        val crewId = character.crewId ?: return "You are not in a crew"
        val crew = crewRepository.getCrew(crewId).firstOrNull() ?: return "Crew not found"
        val perkCount = crew.unlockedPerks.size
        return "Crew: ${crew.name} | Level: ${crew.level} | Members: ${crew.members.size} | Perks: $perkCount"
    }

    /**
     * Donate gold to your crew's treasury.
     *
     * @param appFunctionContext The execution context.
     * @param amount The amount of gold to donate.
     * @return A confirmation message.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun donateToCrew(appFunctionContext: AppFunctionContext, amount: Long): String {
        val success = crewRepository.donateToCrew(amount.toInt())
        return if (success) "Successfully donated $amount gold to your crew!" else "Failed to donate gold."
    }

    /**
     * Check if there is an active world raid and its location.
     *
     * @param appFunctionContext The execution context.
     * @return A message about the active raid or "No active raids".
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkActiveRaid(appFunctionContext: AppFunctionContext): String {
        val raids = gameRepository.getActiveRaids().firstOrNull()
        if (raids.isNullOrEmpty()) return "There are no active world raids currently."
        val raid = raids[0]
        return "⚠️ ACTIVE RAID: ${raid.enemy.name} at ${raid.locationId}! HP: ${raid.enemy.hp}/${raid.enemy.maxHp}"
    }
}
