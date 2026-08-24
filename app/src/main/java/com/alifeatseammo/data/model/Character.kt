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
    val justicePoints: Int = 0,
    val pirateReputation: Int = 0,
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
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val friends: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
    val learnedTechniques: List<String> = emptyList(),
    val professionStats: ProfessionStats = ProfessionStats(),
    val hasMedicalLicense: Boolean = false,
    val healingState: HealingState? = null,
    val trainingState: TrainingState? = null,
    val ship: Ship = Ship(),
    val activeShipId: String = "row_boat",
    val ownedShips: List<Ship> = emptyList(),
    val mythicArt: MythicArt? = null,
    val freeMythicRolls: Int = 3,
    val mythicMana: Int = 100,
    val maxMythicMana: Int = 100,
    val mythicManaUpdatedAt: Long = System.currentTimeMillis(),
    val lastRankChallengeAt: Long = 0,
    val completedQuests: List<String> = emptyList(),
    val completedMissions: List<String> = emptyList(),
    val warContribution: Int = 0,
    val lastWarContributionAt: Long = 0,
    val lastRaidAttackAt: Long = 0,
) {
    fun isHardcodedAdmin(): Boolean {
        val admins = listOf("sedna", "von")
        return admins.contains(name.lowercase())
    }

    fun canEquip(item: Item): Boolean {
        if (item.factionRequirement != Faction.Neutral && item.factionRequirement != faction) {
            return false
        }
        val reqs = item.statRequirements
        return (level >= item.levelRequirement) &&
                (stats.strength >= reqs.strength) &&
                (stats.endurance >= reqs.endurance) &&
                (stats.agility >= reqs.agility) &&
                (stats.perception >= reqs.perception) &&
                (stats.willpower >= reqs.willpower) &&
                (stats.luck >= reqs.luck) &&
                (stats.swordsmanship >= reqs.swordsmanship) &&
                (stats.brawling >= reqs.brawling) &&
                (stats.gunslinging >= reqs.gunslinging) &&
                (stats.spear >= reqs.spear) &&
                (stats.martialArts >= reqs.martialArts) &&
                (stats.sniper >= reqs.sniper) &&
                (stats.mysticArts >= reqs.mysticArts)
    }

    fun getMissingRequirements(item: Item): List<String> {
        val missing = mutableListOf<String>()
        if (item.factionRequirement != Faction.Neutral && item.factionRequirement != faction) {
            missing.add("Faction: ${item.factionRequirement}")
        }
        if (level < item.levelRequirement) {
            missing.add("Level ${item.levelRequirement}")
        }
        
        val reqs = item.statRequirements
        if (stats.strength < reqs.strength) missing.add("Strength ${reqs.strength}")
        if (stats.endurance < reqs.endurance) missing.add("Endurance ${reqs.endurance}")
        if (stats.agility < reqs.agility) missing.add("Agility ${reqs.agility}")
        if (stats.perception < reqs.perception) missing.add("Perception ${reqs.perception}")
        if (stats.willpower < reqs.willpower) missing.add("Willpower ${reqs.willpower}")
        if (stats.luck < reqs.luck) missing.add("Luck ${reqs.luck}")
        
        if (stats.swordsmanship < reqs.swordsmanship) missing.add("Swordsmanship ${reqs.swordsmanship}")
        if (stats.brawling < reqs.brawling) missing.add("Brawling ${reqs.brawling}")
        if (stats.gunslinging < reqs.gunslinging) missing.add("Gunslinging ${reqs.gunslinging}")
        if (stats.spear < reqs.spear) missing.add("Spear ${reqs.spear}")
        if (stats.martialArts < reqs.martialArts) missing.add("Martial Arts ${reqs.martialArts}")
        if (stats.sniper < reqs.sniper) missing.add("Sniper ${reqs.sniper}")
        if (stats.mysticArts < reqs.mysticArts) missing.add("Mystic Arts ${reqs.mysticArts}")
        
        return missing
    }

    fun calculateMaxCapacity(): Int {
        var capacity = inventoryCapacity
        equipment["Bag"]?.let {
            capacity += it.storageBonus
        }
        capacity += ship.upgrades.storageLevel * 5
        return capacity
    }

    fun getCurrentEnergy(): Int {
        val baseRegenRateMs = 3 * 60 * 1000L // 3 minutes
        val regenMultiplier = (mythicArt?.energyRegainMultiplier ?: 1.0f).coerceAtLeast(0.01f)
        val regenRateMs = (baseRegenRateMs / regenMultiplier).toLong().coerceAtLeast(1000L)
        
        val now = System.currentTimeMillis()
        val elapsed = (now - energyUpdatedAt).coerceAtLeast(0L)
        val regenerated = (elapsed / regenRateMs).toInt()
        return (energy + regenerated).coerceAtMost(maxEnergy)
    }

    fun getCurrentMythicMana(): Int {
        if (mythicArt == null) return 0
        val baseRegenRateMs = 2 * 1000L // 2 seconds for mythic mana
        val regenMultiplier = (mythicArt.energyRegainMultiplier).coerceAtLeast(0.01f)
        val regenRateMs = (baseRegenRateMs / regenMultiplier).toLong().coerceAtLeast(1000L)

        val now = System.currentTimeMillis()
        val elapsed = (now - mythicManaUpdatedAt).coerceAtLeast(0L)
        val regenerated = (elapsed / regenRateMs).toInt()
        return (mythicMana + regenerated).coerceAtMost(maxMythicMana)
    }

    fun getDerivedStats(): DerivedStats {
        return DerivedStats(
            criticalChance = 5.0 + (stats.luck * 0.5) + (stats.perception * 0.2),
            dodgeChance = (stats.agility * 0.8) + (stats.luck * 0.2),
            blockEffectiveness = (stats.endurance * 0.2),
            manaRegenPerSecond = (stats.willpower * 0.01),
        )
    }

}

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
    val elements: List<ElementType> = emptyList(),
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
    val speedMultiplier: Float = 1.0f,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val defense: Int = 0,
    val attack: Int = 0,
    val level: Int = 1,
    val upgrades: ShipUpgrades = ShipUpgrades()
)

data class ShipUpgrades(
    val hullLevel: Int = 0,
    val sailLevel: Int = 0,
    val cannonLevel: Int = 0,
    val rudderLevel: Int = 0,
    val storageLevel: Int = 0,
    val cabinLevel: Int = 0,
    val figureheadLevel: Int = 0
)

enum class Gender {
    Male, Female
}

enum class Faction {
    Neutral, Navy, Pirate
}

enum class Rarity {
    Common, Uncommon, Rare, Epic, Legendary, Mythic
}

enum class Race(val description: String) {
    Human("Versatile and adaptable, humans thrive in any environment."),
    Abyssal("Resilient dwellers of the deep, known for their immense endurance."),
    Beastkin("Fierce and agile warriors with animalistic instincts."),
    Celestian("Ethereal beings with strong willpower and sharp perception."),
    Automaton("Constructed for power and durability, built to last.");

    fun getStatBoosts(): Stats {
        return when (this) {
            Human -> Stats(luck = 2.0, strength = 1.0, endurance = 1.0, agility = 1.0)
            Abyssal -> Stats(endurance = 3.0, willpower = 2.0)
            Beastkin -> Stats(agility = 3.0, perception = 2.0)
            Celestian -> Stats(willpower = 3.0, perception = 2.0)
            Automaton -> Stats(strength = 3.0, endurance = 2.0)
        }
    }
}

data class TravelEvent(
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class TravelState(
    val destination: String = "",
    val arrivalTime: Long = 0,
    val startTime: Long = 0,
    val eventMessage: String? = null,
    val events: List<TravelEvent> = emptyList()
)

data class Stats(
    val strength: Double = 0.0,
    val endurance: Double = 0.0,
    val agility: Double = 0.0,
    val perception: Double = 0.0,
    val willpower: Double = 0.0,
    val luck: Double = 0.0,
    // Combat Skills
    val swordsmanship: Double = 0.0,
    val brawling: Double = 0.0,
    val gunslinging: Double = 0.0,
    val spear: Double = 0.0,
    val martialArts: Double = 0.0,
    val sniper: Double = 0.0,
    val mysticArts: Double = 0.0,
    // Elemental Stats
    val elementalResistances: Map<ElementType, Double> = emptyMap(),
    val elementalMastery: Map<ElementType, Double> = emptyMap()
)

data class DerivedStats(
    val criticalChance: Double = 0.0,
    val dodgeChance: Double = 0.0,
    val blockEffectiveness: Double = 0.0,
    val manaRegenPerSecond: Double = 0.0
)

data class ProfessionStats(
    val cooking: Double = 0.0,
    val navigating: Double = 0.0,
    val treasureHunting: Double = 0.0,
    val blacksmith: Double = 0.0,
    val fishing: Double = 0.0,
    val medical: Double = 0.0
)

enum class StatType {
    Strength, Endurance, Agility, Perception, Willpower, Luck,
    Swordsmanship, Brawling, Gunslinging, Spear, MartialArts, Sniper, MysticArts,
    Cooking, Navigating, TreasureHunting, Blacksmith, Fishing, Medical
}

data class Crew(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val captainId: String = "",
    val members: List<String> = emptyList(),
    val roles: Map<String, CrewRole> = emptyMap(),
    val totalBounty: Long = 0,
    val gold: Long = 0,
    val totalDonated: Long = 0,
    val pvpWins: Int = 0,
    val pvpLosses: Int = 0,
    val isPvPEnabled: Boolean = false,
    val level: Int = 1,
    val experience: Long = 0,
    val faction: Faction = Faction.Neutral,
    val unlockedPerks: Map<String, Int> = emptyMap(),
    val isPublic: Boolean = true
)

enum class CrewPerk(val label: String, val description: String) {
    SwiftSails("Swift Sails", "Reduces travel time by 10%"),
    GoldenPlunder("Golden Plunder", "Increases gold rewards from missions by 15%"),
    BountifulSeas("Bountiful Seas", "Increases fishing success rate and rare fish chance"),
    CombatHardened("Combat Hardened", "Increases crew members' defense by 5%"),
    LuckyFind("Lucky Find", "Increases chance to find artifacts in missions"),
    CartographyExpertise("Cartography Expertise", "Reduces travel time by an additional 5% per level"),
    MerchantTies("Merchant Ties", "Increases sell prices by 2% per level"),
    VanguardTactics("Vanguard Tactics", "Increases crew members' attack by 2% per level"),
    IronHold("Iron Hold", "Increases crew members' defense by 2% per level"),
    SeaBreadRegimen("Sea Bread Regimen", "Increases crew members' max HP by 50 per level")
}

enum class CrewRole {
    Captain, CoCaptain, FirstMate, Quartermaster, Officer, Member
}

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
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val subject: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val rewards: Map<String, Int>? = null // e.g., gold, items
)

enum class ActionType {
    Docks, Tavern, Training, Market, Bounties, Crew, Arena, Smuggler, BlackMarket, Shipyard, Camp, Fishing, Infirmary, Work,
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
    while ((currentXp >= xpNeeded) && (currentLevel < maxLevel)) {
        currentXp -= xpNeeded
        currentLevel++
        currentMaxHp += 20
        if ((currentLevel % 5) == 0) {
            currentMaxEnergy += 5
        }
        
        // Match server-side stat growth (+1 to all base stats)
        currentStats = currentStats.copy(
            strength = currentStats.strength + 1.0,
            endurance = currentStats.endurance + 1.0,
            agility = currentStats.agility + 1.0,
            perception = currentStats.perception + 1.0,
            willpower = currentStats.willpower + 1.0,
            luck = currentStats.luck + 1.0
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
