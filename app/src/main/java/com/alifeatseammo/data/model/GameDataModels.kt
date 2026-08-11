package com.alifeatseammo.data.model

data class LocationDef(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val description: String = "",
    val isSafe: Boolean = true,
    val recommendedLevel: Int = 1,
    val weather: String = "Clear",
    val actions: List<ActionDef> = emptyList(),
    val x: Int = 0,
    val y: Int = 0,
)

data class ActionDef(
    val type: ActionType = ActionType.Market,
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

data class LootTable(
    val id: String = "",
    val entries: List<LootEntry> = emptyList()
)

data class LootEntry(
    val itemId: String = "",
    val chance: Float = 0.1f, // 0.0 to 1.0
    val minAmount: Int = 1,
    val maxAmount: Int = 1
)


