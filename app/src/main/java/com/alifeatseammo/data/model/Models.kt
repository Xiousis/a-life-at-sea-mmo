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
    val faction: Faction = Faction.Neutral,
    val lastOnline: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val friends: List<String> = emptyList(),
    val blocked: List<String> = emptyList()
) {
    fun getCurrentEnergy(): Int {
        val regenRateMs = 3 * 60 * 1000L // 3 minutes
        val elapsed = System.currentTimeMillis() - energyUpdatedAt
        val regenerated = (elapsed / regenRateMs).toInt()
        return (energy + regenerated).coerceAtMost(maxEnergy)
    }

}

enum class Gender {
    Male, Female
}

enum class Faction {
    Neutral, Navy, Pirate
}

enum class Rarity {
    Common, Uncommon, Rare, Epic, Legendary
}

enum class Race {
    Human, Aquaris, Halfling
}

data class TravelState(
    val destination: String = "",
    val arrivalTime: Long = 0,
    val startTime: Long = 0
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
    val strength: Int = 0,
    val endurance: Int = 0,
    val agility: Int = 0,
    val perception: Int = 0,
    val willpower: Int = 0,
    val luck: Int = 0,
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
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val energyCost: Int = 10,
    val minLevel: Int = 1,
    val goldReward: Int = 0,
    val xpReward: Int = 0,
    val difficulty: Int = 1,
    val locationId: String = "",
    val factionRequirement: Faction = Faction.Neutral
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
    val opponentId: String? = null,
    val isPvP: Boolean = false,
    val playerTurn: Boolean = true,
    val logs: List<String> = emptyList(),
    val isFinished: Boolean = false,
    val playerWon: Boolean = false,
    val defending: Boolean = false,
    val turnCount: Int = 0,
    val playerEffects: List<StatusEffect> = emptyList(),
    val enemyEffects: List<StatusEffect> = emptyList(),
    val cooldowns: Map<String, Int> = emptyMap()
)

data class StatusEffect(
    val type: EffectType = EffectType.Bleed,
    val duration: Int = 0, // In turns
    val magnitude: Int = 0,
    val source: String = ""
)

enum class EffectType {
    Bleed, Stun, Weaken, Fortify, Burn, Haste
}


enum class CombatAction {
    Attack, Technique, Defend, Item, Flee
}

data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: ItemType = ItemType.Miscellaneous,
    val rarity: Rarity = Rarity.Common,
    val price: Int = 0,
    val statBonus: Stats = Stats(),
    val levelRequirement: Int = 1
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
    val roles: Map<String, CrewRole> = emptyMap(),
    val totalBounty: Long = 0,
    val level: Int = 1,
    val experience: Long = 0
)

enum class CrewRole {
    Captain, Officer, Member
}

data class CrewInvite(
    val crewId: String = "",
    val crewName: String = "",
    val senderId: String = "",
    val targetId: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)

enum class ActionType {
    Docks, Tavern, Training, Market, Bounties, Crew, Arena, Smuggler, BlackMarket, Shipyard, Camp, Cave, Fishing
}

fun Character.checkLevelUp(): Character = this // Handled server-side now
