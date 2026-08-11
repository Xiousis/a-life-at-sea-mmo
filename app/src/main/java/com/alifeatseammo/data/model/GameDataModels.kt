package com.alifeatseammo.data.model

data class LocationDef(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val description: String = "",
    val isSafe: Boolean = true,
    val recommendedLevel: Int = 1,
    val weather: String = "Clear",
    val actions: List<ActionDef> = emptyList()
)

data class ActionDef(
    val type: String = "",
    val label: String = "",
    val icon: String = ""
)

data class EnemyDef(
    val id: String = "",
    val name: String = "",
    val level: Int = 1,
    val hp: Int = 100,
    val stats: Stats = Stats(),
    val goldRewardMin: Int = 0,
    val goldRewardMax: Int = 0,
    val xpReward: Int = 0,
    val dropTableId: String? = null
)

data class MissionDef(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val energyCost: Int = 10,
    val minLevel: Int = 1,
    val goldReward: Int = 0,
    val xpReward: Int = 0,
    val difficulty: Int = 1,
    val locationId: String = ""
)
