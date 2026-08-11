package com.alifeatseammo.data.model

data class Character(
    val id: String = "",
    val name: String = "",
    val gender: Gender = Gender.Male,
    val race: Race = Race.Human,
    val stats: Stats = Stats(),
    val hp: Int = 100,
    val maxHp: Int = 100,
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val gold: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val bounty: Long = 0,
    val title: String = "Novice Sailor",
    val pvpWins: Int = 0,
    val pvpLosses: Int = 0,
    val energyUpdatedAt: Long = System.currentTimeMillis(),
    val currentLocation: String = "Fogi Tail Island",
    val travelState: TravelState? = null,
    val combatState: CombatState? = null,
    val inventory: List<Item> = emptyList(),
    val equipment: Map<String, Item?> = emptyMap(),
    val crewId: String? = null,
    val lastOnline: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false
) {
    fun getCurrentEnergy(): Int {
        val regenRateMs = 3 * 60 * 1000L // 3 minutes
        val elapsed = System.currentTimeMillis() - energyUpdatedAt
        val regenerated = (elapsed / regenRateMs).toInt()
        return (energy + regenerated).coerceAtMost(maxEnergy)
    }

    fun getEnergyRegenProgress(): Float {
        val regenRateMs = 3 * 60 * 1000L
        val elapsed = System.currentTimeMillis() - energyUpdatedAt
        if (energy >= maxEnergy) return 0f
        return (elapsed % regenRateMs).toFloat() / regenRateMs.toFloat()
    }
}

enum class Gender {
    Male, Female
}

enum class Race {
    Human, Aquaris, Halfling
}

data class TravelState(
    val destination: String = "",
    val arrivalTime: Long = 0
)

data class CombatLog(
    val id: String = "",
    val attackerName: String = "",
    val defenderName: String = "",
    val result: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class TransactionLog(
    val id: String = "",
    val userId: String = "",
    val action: String = "",
    val details: String = "",
    val goldChange: Int = 0,
    val xpChange: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class Stats(
    val strength: Int = 5,
    val endurance: Int = 5,
    val agility: Int = 5,
    val perception: Int = 5,
    val willpower: Int = 5,
    val luck: Int = 5,
    // Combat Skills
    val swordsmanship: Int = 0,
    val brawling: Int = 0,
    val gunslinging: Int = 0,
    val spear: Int = 0,
    val martialArts: Int = 0,
    val dualBlades: Int = 0
)

enum class StatType {
    Strength, Endurance, Agility, Perception, Willpower, Luck,
    Swordsmanship, Brawling, Gunslinging, Spear, MartialArts, DualBlades
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
    val name: String = "",
    val level: Int = 1,
    val hp: Int = 50,
    val maxHp: Int = 50,
    val stats: Stats = Stats(),
    val goldReward: Int = 0,
    val xpReward: Int = 0
)

data class CombatState(
    val enemy: Enemy = Enemy(),
    val playerTurn: Boolean = true,
    val logs: List<String> = emptyList(),
    val isFinished: Boolean = false,
    val playerWon: Boolean = false
)

enum class CombatAction {
    Attack, Technique, Defend, Item, Flee
}

data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: ItemType = ItemType.Miscellaneous,
    val price: Int = 0,
    val statBonus: Stats = Stats()
)

enum class ItemType {
    Weapon, Armor, Accessory, Consumable, Miscellaneous
}

data class Location(
    val name: String = "",
    val region: String = "",
    val isSafe: Boolean = true,
    val description: String = "",
    val weather: String = "Clear",
    val playersHere: Int = 0,
    val recommendedLevel: Int = 1,
    val actions: List<LocationAction> = emptyList()
)

data class LocationAction(
    val type: ActionType,
    val label: String,
    val icon: String // Emoji for now
)

data class Crew(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val captainId: String = "",
    val members: List<String> = emptyList(),
    val totalBounty: Long = 0,
    val level: Int = 1,
    val experience: Long = 0
)

enum class ActionType {
    Docks, Tavern, Training, Market, Bounties, Crew, Arena, Smuggler, BlackMarket, Shipyard, Camp, Cave, Fishing
}

fun Character.checkLevelUp(): Character {
    val xpNeeded = level * 100
    return if (xp >= xpNeeded) {
        val nextLevel = level + 1
        val newEndurance = stats.endurance + 1
        val newMaxHp = 50 + (newEndurance * 10)
        this.copy(
            level = nextLevel,
            xp = xp - xpNeeded,
            maxEnergy = maxEnergy + 5,
            energy = maxEnergy + 5,
            maxHp = newMaxHp,
            hp = newMaxHp,
            stats = stats.copy(
                strength = stats.strength + 1,
                endurance = newEndurance,
                agility = stats.agility + 1,
                perception = stats.perception + 1,
                willpower = stats.willpower + 1,
                luck = stats.luck + 1
            )
        ).checkLevelUp()
    } else {
        this
    }
}
