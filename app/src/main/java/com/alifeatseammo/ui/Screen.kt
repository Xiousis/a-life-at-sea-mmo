package com.alifeatseammo.ui

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable object Login : Screen
    @Serializable object CharacterCreation : Screen
    @Serializable object Dashboard : Screen
    @Serializable object Missions : Screen
    @Serializable object Travel : Screen
    @Serializable object Map : Screen
    @Serializable object Leaderboard : Screen
    @Serializable object PvP : Screen
    @Serializable object Chat : Screen
    @Serializable object Combat : Screen
    @Serializable object Victory : Screen
    @Serializable object Defeat : Screen
    @Serializable object Training : Screen
    @Serializable data class Professions(val skill: String = "all") : Screen
    @Serializable data class Character(val playerId: String) : Screen
    @Serializable object Crew : Screen
    @Serializable object More : Screen
    @Serializable object Inventory : Screen
    @Serializable object Stats : Screen
    @Serializable object Skills : Screen
    @Serializable object Settings : Screen
    @Serializable object Help : Screen
    @Serializable data class CrewProfile(val crewId: String? = null) : Screen
    @Serializable object Tavern : Screen
    @Serializable data class Market(val category: String? = null) : Screen
    @Serializable object Mail : Screen
    @Serializable object UpgradeAccount : Screen
    @Serializable object Infirmary : Screen
    @Serializable object Camp : Screen
    @Serializable object CookBook : Screen
    @Serializable object Traveling : Screen
    @Serializable object Shipyard : Screen
    @Serializable object Fishing : Screen
    @Serializable object MythicArt : Screen
    @Serializable object Auction : Screen
    @Serializable object Quests : Screen
    @Serializable object AdminPanel : Screen
}
