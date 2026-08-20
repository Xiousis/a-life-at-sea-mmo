package com.alifeatseammo.data.model

data class RankInfo(
    val rank: String,
    val levelRequired: Int,
    val missionId: String,
    val faction: Faction
)

object RankDefinitions {
    val NAVY_RANKS = listOf(
        RankInfo("Navy Cadet", 0, "", Faction.Navy),
        RankInfo("Navy Recruit", 1, "navy_rank_1", Faction.Navy),
        RankInfo("Petty Officer", 20, "navy_rank_2", Faction.Navy),
        RankInfo("Chief Petty Officer", 40, "navy_rank_3", Faction.Navy),
        RankInfo("Ensign", 60, "navy_rank_4", Faction.Navy),
        RankInfo("Lieutenant", 80, "navy_rank_5", Faction.Navy),
        RankInfo("Commander", 100, "navy_rank_6", Faction.Navy),
        RankInfo("Captain", 130, "navy_rank_7", Faction.Navy),
        RankInfo("Commodore", 160, "navy_rank_8", Faction.Navy),
        RankInfo("Vice Admiral", 200, "navy_rank_9", Faction.Navy),
        RankInfo("Admiral", 250, "navy_rank_10", Faction.Navy),
        RankInfo("Fleet Admiral", 300, "navy_rank_11", Faction.Navy)
    )

    val PIRATE_RANKS = listOf(
        RankInfo("Rogue", 0, "", Faction.Pirate),
        RankInfo("Rogue Sailor", 1, "pirate_rank_1", Faction.Pirate),
        RankInfo("Skirmisher", 20, "pirate_rank_2", Faction.Pirate),
        RankInfo("Deckhand", 40, "pirate_rank_3", Faction.Pirate),
        RankInfo("Swashbuckler", 60, "pirate_rank_4", Faction.Pirate),
        RankInfo("Marauder", 80, "pirate_rank_5", Faction.Pirate),
        RankInfo("Buccaneer", 100, "pirate_rank_6", Faction.Pirate),
        RankInfo("Corsair", 130, "pirate_rank_7", Faction.Pirate),
        RankInfo("Dread Pirate", 160, "pirate_rank_8", Faction.Pirate),
        RankInfo("Pirate Lord", 200, "pirate_rank_9", Faction.Pirate),
        RankInfo("Yonko", 250, "pirate_rank_10", Faction.Pirate),
        RankInfo("Pirate King", 300, "pirate_rank_11", Faction.Pirate)
    )

    fun getNextRank(character: Character): RankInfo? {
        val currentRanks = if (character.faction == Faction.Navy) NAVY_RANKS else if (character.faction == Faction.Pirate) PIRATE_RANKS else return null
        val currentIndex = currentRanks.indexOfFirst { it.rank.equals(character.rank, ignoreCase = true) }
        
        // If not found, they might be "Novice Sailor" or similar.
        if (currentIndex == -1) {
             return currentRanks.firstOrNull()
        }
        
        if (currentIndex < currentRanks.size - 1) {
            return currentRanks[currentIndex + 1]
        }
        
        return null
    }
}
