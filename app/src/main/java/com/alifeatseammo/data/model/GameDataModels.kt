package com.alifeatseammo.data.model

import com.google.firebase.firestore.PropertyName

data class LocationDef(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val description: String = "",
    @get:PropertyName("isSafe")
    val isSafe: Boolean = false,
    val recommendedLevel: Int = 1,
    val weather: String = "Clear",
    val actions: List<ActionDef> = emptyList(),
    val controlledBy: Faction = Faction.Neutral,
    val x: Int = 0,
    val y: Int = 0,
)

data class ActionDef(
    val type: ActionType = ActionType.Market,
    val label: String = "",
    val icon: String = "",
    val parameter: String? = null
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

data class Technique(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: StatType = StatType.Swordsmanship,
    val power: Float = 1.0f,
    val energyCost: Int = 10,
    val cooldown: Int = 0,
    val factionRequirement: Faction = Faction.Neutral,
    val element: ElementType? = null,
    val effects: List<StatusEffect> = emptyList()
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

data class FishDef(
    val id: String = "",
    val name: String = "",
    val rarity: Rarity = Rarity.Common,
    val baseDifficulty: Float = 1.0f,
    val movementPattern: FishingMovementPattern = FishingMovementPattern.Steady,
    val value: Int = 10,
    val xpReward: Int = 5
)

enum class FishingMovementPattern {
    Steady, Sinker, Floater, Darting
}

data class SeaEvent(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: SeaEventType = SeaEventType.Storm,
    val x: Int = 0,
    val y: Int = 0,
    val radius: Int = 100,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val effectDescription: String = "",
    val active: Boolean = true
)

enum class SeaEventType(val icon: String, val color: String) {
    Storm("⛈️", "#2C3E50"),
    MerchantConvoy("🚢", "#F1C40F"),
    Kraken("🐙", "#8E44AD"),
    Shipwreck("🏚️", "#7F8C8D"),
    Whirlpool("🌀", "#2980B9"),
    MysticMist("🌫️", "#BDC3C7")
}

data class WarState(
    val id: String = "current",
    val targetLocation: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val navyScore: Int = 0,
    val pirateScore: Int = 0,
    val isActive: Boolean = false
)


