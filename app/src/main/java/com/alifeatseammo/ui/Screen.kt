package com.alifeatseammo.ui

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object CharacterCreation : Screen("character_creation")
    object Dashboard : Screen("dashboard")
    object Missions : Screen("missions")
    object Travel : Screen("travel")
    object Map : Screen("map")
    object Leaderboard : Screen("leaderboard")
    object PvP : Screen("pvp")
    object Chat : Screen("chat")
    object Combat : Screen("combat")
    object Training : Screen("training")
    object Professions : Screen("professions/{skill}") {
        fun createRoute(skill: String = "all") = "professions/$skill"
    }
    object Character : Screen("character/{playerId}") {
        fun createRoute(playerId: String) = "character/$playerId"
    }
    object Crew : Screen("crew")
    object More : Screen("more")
    object Inventory : Screen("inventory")
    object Stats : Screen("stats")
    object Skills : Screen("skills")
    object Settings : Screen("settings")
    object Help : Screen("help")
    object CrewProfile : Screen("crew_profile")
    object Tavern : Screen("tavern")
    object Market : Screen("market")
    object Mail : Screen("mail")
    object UpgradeAccount : Screen("upgrade_account")
    object Infirmary : Screen("infirmary")
    object Camp : Screen("camp")
    object Traveling : Screen("traveling")
    object Shipyard : Screen("shipyard")
    object Fishing : Screen("fishing")
    object MythicArt : Screen("mythic_art")
    object Auction : Screen("auction")
}
