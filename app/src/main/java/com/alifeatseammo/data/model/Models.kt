package com.alifeatseammo.data.model

import com.google.firebase.firestore.PropertyName

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
    val infamy: Int = 0,
    val rank: String = "Novice Sailor",
    val title: String = "",
    val unlockedTitles: List<String> = emptyList(),
    val pvpWins: Int = 0,
    val pvpLosses: Int = 0,
    val energyUpdatedAt: Long = System.currentTimeMillis(),
    val currentLocation: String = "Fogi Tail Island",
    val travelState: TravelState? = null,
    val combatState: CombatState? = null,
    val inventory: List<Item> = emptyList(),
    val inventoryCapacity: Int = 20,
    val equipment: Map<String, Item?> = emptyMap(),
    val crewId: String? = null,
    val faction: Faction = Faction.Neutral,
    val lastOnline: Long = System.currentTimeMillis(),
    @get:PropertyName("isOnline")
    val isOnline: Boolean = false,
    val friends: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
    val learnedTechniques: List<String> = emptyList(),
    val professionStats: ProfessionStats = ProfessionStats(),
    val hasMedicalLicense: Boolean = false,
    val healingState: HealingState? = null,
    val trainingState: TrainingState? = null,
    val ship: Ship = Ship(),
    val mythicArt: MythicArt? = null,
    val freeMythicRolls: Int = 3
) {
    fun getCurrentEnergy(): Int {
        val baseRegenRateMs = 3 * 60 * 1000L // 3 minutes
        val regenMultiplier = (mythicArt?.energyRegainMultiplier ?: 1.0f).coerceAtLeast(0.01f)
        val regenRateMs = (baseRegenRateMs / regenMultiplier).toLong().coerceAtLeast(1000L)
        
        val now = System.currentTimeMillis()
        val elapsed = (now - energyUpdatedAt).coerceAtLeast(0L)
        val regenerated = (elapsed / regenRateMs).toInt()
        return (energy + regenerated).coerceAtMost(maxEnergy)
    }

}

data class CharacterPrivate(
    val stats: Stats = Stats(),
    val hp: Int = 100,
    val maxHp: Int = 100,
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val gold: Int = 0,
    val xp: Int = 0,
    val infamy: Int = 0,
    val rank: String = "Novice Sailor",
    val title: String = "",
    val unlockedTitles: List<String> = emptyList(),
    val energyUpdatedAt: Long = System.currentTimeMillis(),
    val travelState: TravelState? = null,
    val combatState: CombatState? = null,
    val inventory: List<Item> = emptyList(),
    val inventoryCapacity: Int = 20,
    val equipment: Map<String, Item?> = emptyMap(),
    val friends: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
    val learnedTechniques: List<String> = emptyList(),
    val professionStats: ProfessionStats = ProfessionStats(),
    val hasMedicalLicense: Boolean = false,
    val healingState: HealingState? = null,
    val trainingState: TrainingState? = null,
    val ship: Ship = Ship(),
    val mythicArt: MythicArt? = null,
    val freeMythicRolls: Int = 3
)

data class MythicArt(
    val name: String = "",
    val tier: String = "F",
    val description: String = "",
    val bonusStats: Stats = Stats(),
    val skillMultiplier: Float = 1.0f,
    val multipliedSkill: StatType = StatType.Swordsmanship,
    val techniques: List<String> = emptyList(),
    val debuffPercentage: Float = 0f,
    val restrictedSkillTypes: List<StatType> = emptyList(),
    val energyRegainMultiplier: Float = 1.0f,
    val hugeBuffType: StatType? = null,
    val hugeBuffValue: Float = 0f,
    val weakAgainst: List<StatType> = emptyList(),
    val travelTimeMultiplier: Float = 1.0f,
    val canLearnNonCombatSkills: Boolean = true,
    val element: ElementType? = null,
    val elementalWeaknesses: List<ElementType> = emptyList()
)

data class HealingState(
    val endTime: Long = 0
)

data class TrainingState(
    val endTime: Long = 0,
    val statType: StatType = StatType.Strength
)

data class Ship(
    val id: String = "row_boat",
    val name: String = "Row Boat",
    val price: Int = 0,
    val speedMultiplier: Float = 1.0f
)

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
    Human, Abyssal, Beastkin, Celestian, Automaton
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
    val sniper: Int = 0,
    val mysticArts: Int = 0
)

data class ProfessionStats(
    val cooking: Int = 0,
    val navigating: Int = 0,
    val treasureHunting: Int = 0,
    val blacksmith: Int = 0,
    val fishing: Int = 0,
    val medical: Int = 0
)

enum class StatType {
    Strength, Endurance, Agility, Perception, Willpower, Luck,
    Swordsmanship, Brawling, Gunslinging, Spear, MartialArts, Sniper, MysticArts,
    Cooking, Navigating, TreasureHunting, Blacksmith, Fishing, Medical
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
    val xpReward: Int = 0,
    val dropTableId: String? = null
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
    val turnExpiresAt: Long? = null,
    val turnCount: Int = 0,
    val playerEffects: List<StatusEffect> = emptyList(),
    val enemyEffects: List<StatusEffect> = emptyList(),
    val cooldowns: Map<String, Int> = emptyMap(),
    val loot: List<Item> = emptyList(),
    val goldEarned: Int = 0,
    val xpEarned: Int = 0
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

enum class ElementType(val symbol: String) {
    Fire("🔥"), Water("💧"), Earth("🌍"), Air("💨"), Lightning("⚡"), Ice("❄️"), Light("✨"), Dark("🌑"),
    // Special Elements (S+)
    Void("🌌"), Chaos("🌋"), Celestial("🌠"), Genesis("🌱"), Divine("🔱"), Annihilation("💀"), Creation("🎨")
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
    val storageBonus: Int = 0,
    val levelRequirement: Int = 1,
    val mythicTier: String? = null
)

enum class ItemType {
    Weapon, Armor, Accessory, Bag, Consumable, Tool, Miscellaneous, Fish, Food, Artifact
}

data class Crew(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val captainId: String = "",
    val members: List<String> = emptyList(),
    val roles: Map<String, CrewRole> = emptyMap(),
    val totalBounty: Long = 0,
    val level: Int = 1,
    val experience: Long = 0,
    val faction: Faction = Faction.Neutral
)

enum class CrewRole {
    Captain, Officer, Member
}

data class AuctionListing(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val item: Item = Item(),
    val price: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class CrewInvite(
    val crewId: String = "",
    val crewName: String = "",
    val senderId: String = "",
    val targetId: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)

data class MailMessage(
    val id: String = "",
    val senderName: String = "",
    val subject: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val rewards: Map<String, Int>? = null // e.g., gold, items
)

enum class ActionType {
    Docks, Tavern, Training, Market, Bounties, Crew, Arena, Smuggler, BlackMarket, Shipyard, Camp, Cave, Fishing, Infirmary, Work,
    Kitchen, Forge, Observatory, Expedition, Grind, MythicRoll
}

fun Character.getXpNeeded(): Int {
    return (level * level * 100).coerceAtLeast(100)
}

fun Character.checkLevelUp(): Character {
    val maxLevel = 300
    if (level >= maxLevel) return this.copy(xp = 0)

    var currentLevel = level
    var currentXp = xp
    var currentMaxHp = maxHp
    var currentMaxEnergy = maxEnergy
    var currentStats = stats

    var xpNeeded = currentLevel * currentLevel * 100
    while (currentXp >= xpNeeded && currentLevel < maxLevel) {
        currentXp -= xpNeeded
        currentLevel++
        currentMaxHp += 20
        if (currentLevel % 5 == 0) {
            currentMaxEnergy += 5
        }
        
        // Match server-side stat growth (+1 to all base stats)
        currentStats = currentStats.copy(
            strength = currentStats.strength + 1,
            endurance = currentStats.endurance + 1,
            agility = currentStats.agility + 1,
            perception = currentStats.perception + 1,
            willpower = currentStats.willpower + 1,
            luck = currentStats.luck + 1
        )
        
        if (currentLevel < maxLevel) {
            xpNeeded = currentLevel * currentLevel * 100
        } else {
            currentXp = 0
        }
    }

    return this.copy(
        level = currentLevel,
        xp = currentXp,
        maxHp = currentMaxHp,
        hp = if (currentLevel > level) currentMaxHp else hp,
        maxEnergy = currentMaxEnergy,
        energy = if (currentLevel > level) currentMaxEnergy else energy,
        stats = currentStats
    )
}

data class TechniqueDefinition(
    val id: String,
    val type: StatType
)

object TechniqueRegistry {
    val allTechniques = mapOf(
        "Horizontal Slash" to StatType.Swordsmanship,
        "Sturdy Block" to StatType.Swordsmanship,
        "Dash" to StatType.Agility,
        "Heavy Chop" to StatType.Swordsmanship,
        "Point Strike" to StatType.Swordsmanship,
        "Calm State" to StatType.Willpower,
        "Wild Swing" to StatType.Swordsmanship,
        "Distraction" to StatType.Luck,
        "Pull" to StatType.Strength,
        "Brace" to StatType.Endurance,
        "Deep Cut" to StatType.Swordsmanship,
        "Iron Wall" to StatType.Endurance,
        "Evasion" to StatType.Agility,
        "Pre-empt" to StatType.Perception,
        "Unshakable" to StatType.Willpower,
        "Double or Nothing" to StatType.Luck,
        "Double Slash" to StatType.Swordsmanship,
        "Slam" to StatType.Strength,
        "Precision Hit" to StatType.Perception,
        "Stampede" to StatType.Strength,
        "Flowing Strike" to StatType.Swordsmanship,
        "Immovable" to StatType.Endurance,
        "Cyclone" to StatType.Swordsmanship,
        "Focused Fire" to StatType.Sniper,
        "Grip Smash" to StatType.MartialArts,
        "Shadow Strike" to StatType.Agility,
        "Air Piercer" to StatType.Spear,
        "Disarm" to StatType.Agility,
        "Shockwave" to StatType.Strength,
        "Flicker" to StatType.Agility,
        "Water Slicer" to StatType.Swordsmanship,
        "Bone Breaker" to StatType.Strength,
        "Breeze Step" to StatType.Agility,
        "True Vision" to StatType.Perception,
        "Purge" to StatType.MysticArts,
        "Bleed Out" to StatType.Swordsmanship,
        "Bolt Strike" to StatType.MysticArts,
        "Tremor" to StatType.Strength,
        "Sand Trap" to StatType.Agility,
        "Rebirth" to StatType.Willpower,
        "Fire Slash" to StatType.MysticArts,
        "Colossus Strike" to StatType.Strength,
        "Flash Step" to StatType.Agility,
        "Prevision" to StatType.Perception,
        "Nullify" to StatType.Willpower,
        "Destiny Strike" to StatType.Luck,
        "Ice Prison" to StatType.MysticArts,
        "Flood" to StatType.Swordsmanship,
        "Dark Bind" to StatType.MysticArts,
        "Starfall" to StatType.MysticArts,
        "Cosmic Tear" to StatType.MysticArts,
        "Heavenly Smash" to StatType.Strength,
        "Time Warp" to StatType.Agility,
        "Soul Rend" to StatType.MysticArts,
        "Mirror Shield" to StatType.Endurance,
        "Overawe" to StatType.Willpower,
        "Entangle" to StatType.MysticArts,
        "Miracle" to StatType.Luck,
        "Sunburst" to StatType.MysticArts,
        "Devour" to StatType.MysticArts,
        "Sonic Boom" to StatType.Agility,
        "Infinite Afterimage" to StatType.Agility,
        "Great Divide" to StatType.Swordsmanship,
        "Earth Quake" to StatType.Strength,
        "Soul Suck" to StatType.MysticArts,
        "Spirit Explosion" to StatType.MysticArts,
        "Frozen Domain" to StatType.MysticArts,
        "Shatter" to StatType.Strength,
        "Fate's Seal" to StatType.Luck,
        "Unstoppable Force" to StatType.Willpower,
        "Entropy" to StatType.MysticArts,
        "Butterfly Effect" to StatType.Luck,
        "Singularity" to StatType.MysticArts,
        "Ascension" to StatType.MysticArts,
        "Holy Rain" to StatType.MysticArts,
        "Judgment" to StatType.Willpower,
        "Erasure" to StatType.MysticArts,
        "Non-Existence" to StatType.MysticArts,
        "Dark Matter" to StatType.MysticArts,
        "Creation" to StatType.MysticArts,
        "Renewal" to StatType.Willpower,
        "Alpha Strike" to StatType.Swordsmanship,
        "One Strike" to StatType.Swordsmanship,
        "Universal Cut" to StatType.Swordsmanship,
        "End of All" to StatType.MysticArts,
        "Finality" to StatType.MysticArts,
        "Rewrite" to StatType.MysticArts,
        "Delete" to StatType.MysticArts,
        "Absolute Command" to StatType.Willpower,
        "bash" to StatType.Brawling,
        // New B/A Rank Techniques
        "Heat Haze" to StatType.MysticArts,
        "Earth Breaker" to StatType.Strength,
        "Afterimage" to StatType.Agility,
        "Mind Link" to StatType.Perception,
        "Gravity Field" to StatType.Willpower,
        "Jackpot" to StatType.Luck,
        "Glacial Wall" to StatType.MysticArts,
        "Tidal Wave" to StatType.Swordsmanship,
        "Nightmare" to StatType.MysticArts,
        "Sunbeam" to StatType.MysticArts,
        "Black Hole" to StatType.MysticArts,
        "Nova" to StatType.MysticArts,
        "Sky Cracker" to StatType.Strength,
        "Final Pillar" to StatType.Strength,
        "Stutter" to StatType.Agility,
        "Future Echo" to StatType.Agility,
        "Spirit Bind" to StatType.MysticArts,
        "Essence Theft" to StatType.MysticArts,
        "Fortress" to StatType.Endurance,
        "Aegis" to StatType.Endurance,
        "Command" to StatType.Willpower,
        "Domination" to StatType.Willpower,
        "Root Spike" to StatType.MysticArts,
        "Thorn Hail" to StatType.MysticArts,
        "Lucky Break" to StatType.Luck,
        "Twist of Fate" to StatType.Luck,
        "Blinding Light" to StatType.MysticArts,
        "Solar Storm" to StatType.MysticArts,
        "Void Pull" to StatType.MysticArts,
        "Darkness Falls" to StatType.MysticArts,
        // Z-Tier Annihilation Techniques
        "Annihilation: Void Burst" to StatType.MysticArts,
        "Annihilation: Reality Erasure" to StatType.MysticArts,
        "Annihilation: Soul Grasp" to StatType.MysticArts,
        "Annihilation: World's End" to StatType.MysticArts,
        "Annihilation: Abyssal Gaze" to StatType.MysticArts,
        "Annihilation: Dark Matter Crush" to StatType.MysticArts,
        "Annihilation: Entropy Pulse" to StatType.MysticArts,
        "Annihilation: Singularity Strike" to StatType.MysticArts,
        "Annihilation: Oblivion Wave" to StatType.MysticArts,
        "Annihilation: Shadow Reign" to StatType.MysticArts,
        "Annihilation: Ruin" to StatType.MysticArts,
        "Annihilation: Decay" to StatType.MysticArts,
        "Annihilation: Despair" to StatType.MysticArts,
        "Annihilation: Chaos Bolt" to StatType.MysticArts,
        "Annihilation: Ultimate Zero" to StatType.MysticArts,
        // Z-Tier Creation Techniques
        "Creation: Genesis Flash" to StatType.MysticArts,
        "Creation: Life Weaver" to StatType.MysticArts,
        "Creation: Stellar Birth" to StatType.MysticArts,
        "Creation: Infinite Bloom" to StatType.MysticArts,
        "Creation: Holy Radiance" to StatType.MysticArts,
        "Creation: Divine Structure" to StatType.MysticArts,
        "Creation: Harmony Strike" to StatType.MysticArts,
        "Creation: Eternal Dawn" to StatType.MysticArts,
        "Creation: Cosmic Pulse" to StatType.MysticArts,
        "Creation: Seraphim's Gaze" to StatType.MysticArts,
        "Creation: Restoration" to StatType.MysticArts,
        "Creation: Sanctity" to StatType.MysticArts,
        "Creation: Purity" to StatType.MysticArts,
        "Creation: Luminescence" to StatType.MysticArts,
        "Creation: Omega Spark" to StatType.MysticArts
    )

    fun getTypeFor(techniqueId: String): StatType? = allTechniques[techniqueId]
}
