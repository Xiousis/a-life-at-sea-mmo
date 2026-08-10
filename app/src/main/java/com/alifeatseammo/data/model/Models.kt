package com.alifeatseammo.data.model

data class Character(
    val id: String = "",
    val name: String,
    val originIsland: String,
    val style: CombatStyle,
    val stats: Stats = Stats(),
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val gold: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val bounty: Long = 0,
    val currentLocation: String = "Logue Town",
    val travelState: TravelState? = null,
    val inventory: List<Item> = emptyList()
)

data class TravelState(
    val destination: String,
    val arrivalTime: Long
)

data class CombatLog(
    val id: String = "",
    val attackerName: String = "",
    val defenderName: String = "",
    val result: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Stats(
    val strength: Int = 5,
    val endurance: Int = 5,
    val agility: Int = 5,
    val perception: Int = 5,
    val willpower: Int = 5,
    val luck: Int = 5
)

enum class CombatStyle {
    Swordsmanship, Brawling, Gunslinger, Spear, MartialArts, DualBlades
}

enum class StatType {
    Strength, Endurance, Agility, Perception, Willpower, Luck
}

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val energyCost: Int,
    val minLevel: Int,
    val rewards: Reward,
    val difficulty: Int
)

data class Reward(
    val gold: Int,
    val xp: Int
)

data class Enemy(
    val name: String,
    val stats: Stats,
    val goldReward: Int,
    val xpReward: Int
)

data class Item(
    val id: String,
    val name: String,
    val price: Int,
    val statBonus: Stats = Stats()
)
