package com.alifeatseammo.data.model

import androidx.annotation.Keep

@Keep
data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: ItemType = ItemType.Miscellaneous,
    val rarity: Rarity = Rarity.Common,
    val price: Long = 0,
    val statBonus: Stats = Stats(),
    val statRequirements: Stats = Stats(),
    val storageBonus: Int = 0,
    val levelRequirement: Int = 1,
    val mythicTier: String? = null,
    val weaponCategory: String? = null,
    val slot: String? = null,
    val factionRequirement: Faction = Faction.Neutral,
    val quantity: Int = 1
)

enum class ItemType {
    Weapon, Armor, Accessory, Bag, Consumable, Tool, Miscellaneous, Fish, Food, Artifact, Lure, Ship, Ingredient
}

@Keep
data class AuctionListing(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val item: Item = Item(),
    val price: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class TransactionLog(
    val id: String = "",
    val userId: String = "",
    val action: String = "",
    val details: String = "",
    val goldChange: Long = 0,
    val xpChange: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class Mission(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val energyCost: Int = 10,
    val minLevel: Int = 1,
    val goldReward: Long = 0,
    val xpReward: Int = 0,
    val difficulty: Int = 1,
    val locationId: String = "",
    val factionRequirement: Faction = Faction.Neutral,
    val isRankUp: Boolean = false,
    val targetRank: String? = null
)

@Keep
data class IslandQuest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val islandId: String = "",
    val minLevel: Int = 1,
    val maxLevel: Int = 300,
    val goldReward: Long = 0,
    val xpReward: Int = 0,
    val itemRewards: List<LootEntry> = emptyList(),
    val prerequisiteQuestId: String? = null,
    val isMainStory: Boolean = false,
    val enemyId: String? = null, // If it's a kill quest
    val killCountRequired: Int = 0
)

@Keep
data class Recipe(
    val id: String = "",
    val name: String = "",
    val levelRequirement: Int = 1,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val result: Item = Item()
)

@Keep
data class RecipeIngredient(
    val itemId: String = "",
    val quantity: Int = 1,
    val type: String? = null
)

