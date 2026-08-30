package com.alifeatseammo.data.model

import com.google.firebase.firestore.PropertyName
import androidx.annotation.Keep

@Keep
data class Enemy(
    val name: String = "",
    val level: Int = 1,
    val hp: Int = 50,
    val maxHp: Int = 50,
    val stats: Stats = Stats(),
    val goldReward: Long = 0,
    val xpReward: Int = 0,
    val dropTableId: String? = null,
    val elements: List<ElementType> = emptyList()
)

@Keep
data class CombatState(
    val enemy: Enemy = Enemy(),
    val opponentId: String? = null,
    @get:PropertyName("isPvP")
    val isPvP: Boolean = false,
    @get:PropertyName("isRankChallenge")
    val isRankChallenge: Boolean = false,
    @get:PropertyName("isRaid")
    val isRaid: Boolean = false,
    val raidId: String? = null,
    @get:PropertyName("playerTurn")
    val playerTurn: Boolean = true,
    val logs: List<String> = emptyList(),
    @get:PropertyName("isFinished")
    val isFinished: Boolean = false,
    @get:PropertyName("playerWon")
    val playerWon: Boolean = false,
    val defending: Boolean = false,
    val turnExpiresAt: Long? = null,
    val turnCount: Int = 0,
    val playerEffects: List<StatusEffect> = emptyList(),
    val enemyEffects: List<StatusEffect> = emptyList(),
    val cooldowns: Map<String, Int> = emptyMap(),
    val loot: List<Item> = emptyList(),
    val goldEarned: Long = 0,
    val xpEarned: Int = 0,
    val comboCount: Int = 0
)

@Keep
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
    Physical("⚔️"), Hybrid("🌀"),
    Fire("🔥"), Water("💧"), Earth("🌍"), Air("💨"), Lightning("⚡"), Ice("❄️"), Light("✨"), Dark("🌑"),
    // Special Elements (S+)
    Void("🌌"), Chaos("🌋"), Celestial("🌠"), Genesis("🌱"), Divine("🔱"), Annihilation("💀"), Creation("🎨")
}

enum class CombatAction {
    Attack, Technique, Defend, Item, Flee
}

@Keep
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
        // Navy Rokushiki
        "Soru" to StatType.Agility,
        "Tekkai" to StatType.Endurance,
        "Rankyaku" to StatType.MartialArts,
        "Geppo" to StatType.Agility,
        "Shigan" to StatType.MartialArts,
        "Kami-e" to StatType.Agility,
        "Rokuogan" to StatType.Willpower,
        // Pirate Dirty Fighting
        "Pocket Sand" to StatType.Luck,
        "Low Blow" to StatType.Brawling,
        "Dirty Distraction" to StatType.Luck,
        "Grog Splash" to StatType.Brawling,
        "Backstab" to StatType.Agility,
        "Scurvy Strike" to StatType.Strength,
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

object RaidRegistry {
    val worldBosses = listOf(
        Enemy(name = "Abyssal Kraken", level = 50, hp = 10000, maxHp = 10000, stats = Stats(strength = 50.0, endurance = 100.0)),
        Enemy(name = "Ancient Sea Dragon", level = 120, hp = 50000, maxHp = 50000, stats = Stats(strength = 150.0, endurance = 300.0)),
        Enemy(name = "Ghost Captain Silvereye", level = 180, hp = 150000, maxHp = 150000, stats = Stats(strength = 300.0, endurance = 600.0)),
        Enemy(name = "Leviathan of the Void", level = 250, hp = 500000, maxHp = 500000, stats = Stats(strength = 600.0, endurance = 1200.0)),
        Enemy(name = "The Sunken God", level = 300, hp = 1000000, maxHp = 1000000, stats = Stats(strength = 1000.0, endurance = 2000.0))
    )
}

@Keep
data class RaidBoss(
    val id: String = "",
    val enemy: Enemy = Enemy(),
    val locationId: String? = null, // Optional island name
    val x: Double = 0.0,
    val y: Double = 0.0,
    val totalDamageTaken: Long = 0,
    val participants: Map<String, RaidParticipant> = emptyMap(), // userId -> participant
    val status: RaidStatus = RaidStatus.Active,
    val spawnTime: Long = 0,
    val endTime: Long? = null
)

@Keep
data class RaidParticipant(
    val userId: String = "",
    val userName: String = "",
    val totalDamage: Long = 0,
    val lastHitAt: Long = 0
)

enum class RaidStatus {
    Active, Defeated, Expired
}

@Keep
data class RaidReward(
    val raidId: String = "",
    val rankRewards: List<RankReward> = emptyList(),
    val participationReward: RewardEntry = RewardEntry()
)

@Keep
data class RankReward(
    val minRank: Int,
    val maxRank: Int,
    val rewards: RewardEntry
)

@Keep
data class RewardEntry(
    val gold: Long = 0,
    val xp: Int = 0,
    val exclusiveDrops: List<LootEntry> = emptyList()
)

