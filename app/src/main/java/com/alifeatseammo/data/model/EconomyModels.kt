package com.alifeatseammo.data.model

data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: ItemType = ItemType.Miscellaneous,
    val rarity: Rarity = Rarity.Common,
    val price: Int = 0,
    val statBonus: Stats = Stats(),
    val statRequirements: Stats = Stats(),
    val storageBonus: Int = 0,
    val levelRequirement: Int = 1,
    val mythicTier: String? = null,
    val weaponCategory: String? = null,
    val quantity: Int = 1
)

enum class ItemType {
    Weapon, Armor, Accessory, Bag, Consumable, Tool, Miscellaneous, Fish, Food, Artifact, Lure
}

data class AuctionListing(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val item: Item = Item(),
    val price: Int = 0,
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
    val factionRequirement: Faction = Faction.Neutral,
    val isRankUp: Boolean = false,
    val targetRank: String? = null
)
