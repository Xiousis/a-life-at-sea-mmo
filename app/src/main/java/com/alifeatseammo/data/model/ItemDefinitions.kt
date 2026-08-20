package com.alifeatseammo.data.model

@Suppress("unused")
object ItemDefinitions {
    // Weapons
    val IRON_SWORD = Item(
        id = "iron_sword",
        name = "Iron Sword",
        description = "A standard issue iron sword. Sturdy and reliable.",
        type = ItemType.Weapon,
        rarity = Rarity.Common,
        price = 100,
        statBonus = Stats(swordsmanship = 5.0, strength = 2.0),
        statRequirements = Stats(swordsmanship = 10.0),
        levelRequirement = 5
    )

    val FLINTLOCK_PISTOL = Item(
        id = "flintlock_pistol",
        name = "Flintlock Pistol",
        description = "A classic pirate sidearm. Loud but effective.",
        type = ItemType.Weapon,
        rarity = Rarity.Uncommon,
        price = 250,
        statBonus = Stats(gunslinging = 8.0, perception = 3.0),
        statRequirements = Stats(gunslinging = 25.0),
        levelRequirement = 10
    )

    val WOODEN_SPEAR = Item(
        id = "wooden_spear",
        name = "Wooden Spear",
        description = "A simple sharpened pole. Better than nothing.",
        type = ItemType.Weapon,
        rarity = Rarity.Common,
        price = 50,
        statBonus = Stats(spear = 4.0, agility = 1.0),
        levelRequirement = 1
    )

    val CURSED_BLADE_OF_SORROW = Item(
        id = "cursed_blade_sorrow",
        name = "Cursed Blade of Sorrow",
        description = "A dark blade that hungers for souls. Grants immense power but at a terrible price.",
        type = ItemType.Weapon,
        rarity = Rarity.Legendary,
        price = 500000,
        statBonus = Stats(swordsmanship = 50.0, strength = 30.0, endurance = -10.0),
        statRequirements = Stats(swordsmanship = 150.0, willpower = 50.0),
        levelRequirement = 40
    )

    val POSEIDON_TRIDENT = Item(
        id = "poseidon_trident",
        name = "Trident of Poseidon",
        description = "A mythical weapon that commands the very seas.",
        type = ItemType.Weapon,
        rarity = Rarity.Mythic,
        price = 5000000,
        statBonus = Stats(spear = 100.0, strength = 50.0, willpower = 50.0),
        statRequirements = Stats(spear = 500.0, strength = 200.0, willpower = 200.0),
        levelRequirement = 80
    )

    val LONG_RANGE_RIFLE = Item(
        id = "long_range_rifle",
        name = "Long Range Rifle",
        description = "A precision weapon for those who strike from afar.",
        type = ItemType.Weapon,
        rarity = Rarity.Rare,
        price = 1500,
        statBonus = Stats(sniper = 15.0, perception = 10.0),
        statRequirements = Stats(sniper = 50.0, perception = 30.0),
        levelRequirement = 20
    )

    // Armor
    val LEATHER_VEST = Item(
        id = "leather_vest",
        name = "Leather Vest",
        description = "Provides basic protection against cuts and scrapes.",
        type = ItemType.Armor,
        rarity = Rarity.Common,
        price = 80,
        statBonus = Stats(endurance = 5.0),
        levelRequirement = 3
    )

    val AEGIS_SHIELD = Item(
        id = "aegis_shield",
        name = "Aegis Shield",
        description = "A shield forged by the gods themselves. Unbreakable.",
        type = ItemType.Armor,
        rarity = Rarity.Mythic,
        price = 4500000,
        statBonus = Stats(endurance = 120.0, willpower = 60.0),
        levelRequirement = 75
    )

    val IRON_HELMET = Item(
        id = "iron_helmet",
        name = "Iron Helmet",
        description = "Protects your head from blunt trauma.",
        type = ItemType.Armor,
        rarity = Rarity.Uncommon,
        price = 200,
        statBonus = Stats(endurance = 10.0, willpower = 2.0),
        levelRequirement = 8
    )

    val PIRATE_CLOAK = Item(
        id = "pirate_cloak",
        name = "Pirate Cloak",
        description = "A stylish cloak that helps you blend into the shadows.",
        type = ItemType.Armor,
        rarity = Rarity.Rare,
        price = 500,
        statBonus = Stats(agility = 8.0, luck = 5.0),
        levelRequirement = 15
    )

    // Ingredients
    val SALT = Item(
        id = "salt",
        name = "Salt",
        description = "Essential for preserving and seasoning food.",
        type = ItemType.Miscellaneous, // Or Ingredient if we add it to ItemType
        rarity = Rarity.Common,
        price = 5
    )

    val SPICES = Item(
        id = "spices",
        name = "Spices",
        description = "Exotic spices from distant lands.",
        type = ItemType.Miscellaneous,
        rarity = Rarity.Uncommon,
        price = 50
    )

    val SEAWEED = Item(
        id = "seaweed",
        name = "Seaweed",
        description = "Nutritious but slimy. Good for soups.",
        type = ItemType.Miscellaneous,
        rarity = Rarity.Common,
        price = 2
    )
    
    val RAW_FISH = Item(
        id = "raw_fish",
        name = "Raw Fish",
        description = "Freshly caught. Needs cooking.",
        type = ItemType.Fish,
        rarity = Rarity.Common,
        price = 10
    )

    // Food
    val COOKED_FISH = Item(
        id = "cooked_fish",
        name = "Cooked Fish",
        description = "A simple meal that restores HP and MP.",
        type = ItemType.Consumable,
        rarity = Rarity.Common,
        price = 25
    )

    val SEA_STEW = Item(
        id = "sea_stew",
        name = "Sea Stew",
        description = "A hearty stew that restores a good amount of HP and MP.",
        type = ItemType.Consumable,
        rarity = Rarity.Uncommon,
        price = 150
    )

    // Bags
    val SMALL_BAG = Item(
        id = "bag_small",
        name = "Small Cotton Bag",
        description = "A simple bag that adds 5 slots to your inventory.",
        type = ItemType.Bag,
        rarity = Rarity.Common,
        price = 500,
        storageBonus = 5
    )

    val MEDIUM_BAG = Item(
        id = "bag_medium",
        name = "Sturdy Leather Satchel",
        description = "A well-made satchel that adds 10 slots to your inventory.",
        type = ItemType.Bag,
        rarity = Rarity.Uncommon,
        price = 5000,
        storageBonus = 10
    )

    val LARGE_BAG = Item(
        id = "bag_large",
        name = "Reinforced Sea-Chest Bag",
        description = "A massive bag for serious collectors. Adds 20 slots.",
        type = ItemType.Bag,
        rarity = Rarity.Rare,
        price = 50000,
        storageBonus = 20
    )

    val LEGENDARY_BAG = Item(
        id = "bag_legendary",
        name = "Infinite Void Pouch",
        description = "A pouch that seems to defy the laws of space. Adds 50 slots.",
        type = ItemType.Bag,
        rarity = Rarity.Legendary,
        price = 1000000,
        storageBonus = 50
    )

    // Artifacts
    val ARTIFACT_F = Item(
        id = "artifact_f",
        name = "Shattered Slate [F]",
        description = "A common artifact containing a faint whisper of power.",
        type = ItemType.Artifact,
        rarity = Rarity.Common,
        price = 1000,
        mythicTier = "F"
    )

    val ARTIFACT_E = Item(
        id = "artifact_e",
        name = "Bones of Old [E]",
        description = "A weathered relic that holds basic knowledge from a bygone age.",
        type = ItemType.Artifact,
        rarity = Rarity.Common,
        price = 5000,
        mythicTier = "E"
    )

    val ARTIFACT_D = Item(
        id = "artifact_d",
        name = "Ancient Shard [D]",
        description = "A shard from a long-lost civilization, pulsating with faint energy.",
        type = ItemType.Artifact,
        rarity = Rarity.Uncommon,
        price = 20000,
        mythicTier = "D"
    )

    val ARTIFACT_C = Item(
        id = "artifact_c",
        name = "Glowing Core [C]",
        description = "A core of pure energy that contains specialized techniques.",
        type = ItemType.Artifact,
        rarity = Rarity.Uncommon,
        price = 100000,
        mythicTier = "C"
    )

    val ARTIFACT_B = Item(
        id = "artifact_b",
        name = "Jade Idol [B]",
        description = "A beautifully crafted idol that resonates with your spirit.",
        type = ItemType.Artifact,
        rarity = Rarity.Rare,
        price = 500000,
        mythicTier = "B"
    )

    val ARTIFACT_A = Item(
        id = "artifact_a",
        name = "Dragon Scale [A]",
        description = "A scale from a legendary dragon, containing immense power.",
        type = ItemType.Artifact,
        rarity = Rarity.Rare,
        price = 2000000,
        mythicTier = "A"
    )

    val ARTIFACT_S = Item(
        id = "artifact_s",
        name = "Phoenix Feather [S]",
        description = "A feather that never stops burning with mythical energy.",
        type = ItemType.Artifact,
        rarity = Rarity.Epic,
        price = 10000000,
        mythicTier = "S"
    )

    val ARTIFACT_SS = Item(
        id = "artifact_ss",
        name = "Tear of a God [SS]",
        description = "A crystalline tear said to fall from the heavens.",
        type = ItemType.Artifact,
        rarity = Rarity.Epic,
        price = 50000000,
        mythicTier = "SS"
    )

    val ARTIFACT_SSS = Item(
        id = "artifact_sss",
        name = "Void Essence [SSS]",
        description = "The pure essence of the void. The absolute pinnacle of power.",
        type = ItemType.Artifact,
        rarity = Rarity.Legendary,
        price = 250000000,
        mythicTier = "SSS"
    )

    val ARTIFACT_Z = Item(
        id = "artifact_z",
        name = "Primordial Spark [Z]",
        description = "A fragment of the original creation, blindingly and terrifyingly powerful.",
        type = ItemType.Artifact,
        rarity = Rarity.Legendary,
        price = 1000000000,
        mythicTier = "Z"
    )

    // Lures
    val BASIC_LURE = Item(
        id = "lure_basic",
        name = "Basic Lure",
        description = "A simple lure that increases the catching bar size slightly.",
        type = ItemType.Lure,
        rarity = Rarity.Common,
        price = 150
    )

    val HEAVY_LURE = Item(
        id = "lure_heavy",
        name = "Heavy Lure",
        description = "A weighted lure that slows down the bar's gravity.",
        type = ItemType.Lure,
        rarity = Rarity.Uncommon,
        price = 500
    )

    val GLOWING_LURE = Item(
        id = "lure_glowing",
        name = "Glowing Lure",
        description = "Attracts rare and legendary fish more easily.",
        type = ItemType.Lure,
        rarity = Rarity.Rare,
        price = 2000
    )

    val allItems = listOf(
        IRON_SWORD, FLINTLOCK_PISTOL, WOODEN_SPEAR, CURSED_BLADE_OF_SORROW, POSEIDON_TRIDENT, LONG_RANGE_RIFLE,
        LEATHER_VEST, AEGIS_SHIELD, IRON_HELMET, PIRATE_CLOAK,
        SALT, SPICES, SEAWEED, RAW_FISH, COOKED_FISH, SEA_STEW,
        SMALL_BAG, MEDIUM_BAG, LARGE_BAG, LEGENDARY_BAG,
        ARTIFACT_F, ARTIFACT_E, ARTIFACT_D, ARTIFACT_C, ARTIFACT_B,
        ARTIFACT_A, ARTIFACT_S, ARTIFACT_SS, ARTIFACT_SSS, ARTIFACT_Z,
        BASIC_LURE, HEAVY_LURE, GLOWING_LURE
    )
}
