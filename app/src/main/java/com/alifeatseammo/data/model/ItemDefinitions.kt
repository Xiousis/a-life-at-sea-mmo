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

    val CURSED_CUTLASS = Item(
        id = "cursed_cutlass",
        name = "Cursed Cutlass",
        description = "A wicked blade that hums with dark energy. High damage, but carries a heavy burden.",
        type = ItemType.Weapon,
        rarity = Rarity.Rare,
        price = 12000,
        statBonus = Stats(swordsmanship = 35.0, strength = 10.0, endurance = -5.0),
        statRequirements = Stats(swordsmanship = 80.0),
        levelRequirement = 25,
        factionRequirement = Faction.Pirate
    )

    val PLUNDERERS_PISTOL = Item(
        id = "plunderers_pistol",
        name = "Plunderer's Pistol",
        description = "A pirate's best friend. Guaranteed to find more loot.",
        type = ItemType.Weapon,
        rarity = Rarity.Uncommon,
        price = 4500,
        statBonus = Stats(gunslinging = 12.0, luck = 10.0),
        statRequirements = Stats(gunslinging = 30.0),
        levelRequirement = 15,
        factionRequirement = Faction.Pirate
    )

    val NAVY_SABER = Item(
        id = "navy_saber",
        name = "Navy Officer Saber",
        description = "A finely crafted saber issued to high-ranking Navy officers.",
        type = ItemType.Weapon,
        rarity = Rarity.Rare,
        price = 15000,
        statBonus = Stats(swordsmanship = 25.0, agility = 10.0),
        statRequirements = Stats(swordsmanship = 100.0),
        levelRequirement = 30,
        factionRequirement = Faction.Navy
    )

    val NAVY_CARBINE = Item(
        id = "navy_carbine",
        name = "Navy Carbine",
        description = "A powerful and accurate rifle for elite Marine units.",
        type = ItemType.Weapon,
        rarity = Rarity.Rare,
        price = 18000,
        statBonus = Stats(sniper = 35.0, perception = 15.0),
        statRequirements = Stats(sniper = 120.0),
        levelRequirement = 35,
        factionRequirement = Faction.Navy
    )

    val IRON_SPEAR = Item(
        id = "iron_spear",
        name = "Iron Spear",
        description = "A long-reaching thrusting weapon.",
        type = ItemType.Weapon,
        rarity = Rarity.Common,
        price = 120,
        statBonus = Stats(spear = 4.0, strength = 1.0),
        statRequirements = Stats(spear = 5.0),
        levelRequirement = 2
    )

    val TRIDENT_OF_THE_DEEP = Item(
        id = "trident_of_the_deep",
        name = "Trident of the Deep",
        description = "A mystical trident found in the depths.",
        type = ItemType.Weapon,
        rarity = Rarity.Rare,
        price = 15000,
        statBonus = Stats(spear = 30.0, strength = 10.0, willpower = 20.0),
        statRequirements = Stats(spear = 100.0),
        levelRequirement = 30
    )

    val PIRATE_MUSKET = Item(
        id = "pirate_musket",
        name = "Blackbeard's Musket",
        description = "A pirate's favorite ranged weapon.",
        type = ItemType.Weapon,
        rarity = Rarity.Uncommon,
        price = 2000,
        statBonus = Stats(gunslinging = 15.0, perception = 5.0),
        statRequirements = Stats(gunslinging = 40.0),
        levelRequirement = 12
    )

    val DOUBLE_PISTOL = Item(
        id = "double_pistol",
        name = "Double-Barreled Pistol",
        description = "Two shots are better than one.",
        type = ItemType.Weapon,
        rarity = Rarity.Rare,
        price = 8000,
        statBonus = Stats(gunslinging = 35.0, agility = 5.0),
        statRequirements = Stats(gunslinging = 90.0),
        levelRequirement = 20
    )

    // Armor
    val LEATHER_VEST = Item(
        id = "leather_vest",
        name = "Leather Vest",
        description = "Provides basic protection against cuts and scrapes.",
        type = ItemType.Armor,
        rarity = Rarity.Common,
        price = 150,
        statBonus = Stats(endurance = 5.0),
        levelRequirement = 3
    )

    val IRON_CHESTPLATE = Item(
        id = "iron_chestplate",
        name = "Iron Chestplate",
        description = "Solid iron protection.",
        type = ItemType.Armor,
        rarity = Rarity.Uncommon,
        price = 1200,
        statBonus = Stats(endurance = 15.0, willpower = 2.0),
        levelRequirement = 10
    )

    val NAVY_UNIFORM = Item(
        id = "navy_uniform",
        name = "Marine Uniform",
        description = "The standard issue blues.",
        type = ItemType.Armor,
        rarity = Rarity.Uncommon,
        price = 2500,
        statBonus = Stats(endurance = 20.0, agility = 5.0),
        levelRequirement = 15,
        factionRequirement = Faction.Navy
    )

    val NAVY_OFFICER_UNIFORM = Item(
        id = "navy_officer_uniform",
        name = "Navy Officer Uniform",
        description = "Commanding presence with reinforced protection.",
        type = ItemType.Armor,
        rarity = Rarity.Rare,
        price = 20000,
        statBonus = Stats(endurance = 45.0, willpower = 10.0),
        levelRequirement = 30,
        factionRequirement = Faction.Navy
    )

    val JUSTICE_CAPE = Item(
        id = "justice_cape",
        name = "Navy Justice Cape",
        description = "The iconic white cape with 'JUSTICE' emblazoned on the back.",
        type = ItemType.Armor,
        rarity = Rarity.Epic,
        price = 50000,
        statBonus = Stats(endurance = 60.0, willpower = 30.0, luck = 10.0),
        levelRequirement = 50,
        factionRequirement = Faction.Navy
    )

    val SEA_CAPTAIN_COAT = Item(
        id = "sea_captain_coat",
        name = "Sea Captain's Coat",
        description = "A heavy coat that commands respect.",
        type = ItemType.Armor,
        rarity = Rarity.Rare,
        price = 12000,
        statBonus = Stats(endurance = 40.0, willpower = 15.0, luck = 5.0),
        levelRequirement = 25
    )

    val REINFORCED_BOOTS = Item(
        id = "reinforced_boots",
        name = "Reinforced Boots",
        description = "Sturdy boots for rough terrain.",
        type = ItemType.Armor,
        rarity = Rarity.Uncommon,
        price = 800,
        statBonus = Stats(endurance = 8.0, agility = 3.0),
        slot = "Boots",
        levelRequirement = 8
    )

    val STEEL_GLOVES = Item(
        id = "steel_gloves",
        name = "Steel Plated Gloves",
        description = "Protect your hands during combat.",
        type = ItemType.Armor,
        rarity = Rarity.Uncommon,
        price = 1500,
        statBonus = Stats(endurance = 10.0, strength = 5.0),
        levelRequirement = 12
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
        slot = "Helmet",
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

    val SMUGGLERS_CLOAK = Item(
        id = "smugglers_cloak",
        name = "Smuggler's Cloak",
        description = "Enchanted to hide contraband and the wearer. High Agility and Luck.",
        type = ItemType.Armor,
        rarity = Rarity.Rare,
        price = 8000,
        statBonus = Stats(agility = 20.0, luck = 15.0, endurance = -2.0),
        levelRequirement = 20,
        factionRequirement = Faction.Pirate
    )

    val MARINE_MEDAL_OF_VALOR = Item(
        id = "marine_medal_valor",
        name = "Marine Medal of Valor",
        description = "A prestigious award for bravery in the line of duty.",
        type = ItemType.Accessory,
        rarity = Rarity.Rare,
        price = 5000, // In Justice Points (Backend logic needed)
        statBonus = Stats(willpower = 15.0, endurance = 10.0),
        levelRequirement = 30,
        factionRequirement = Faction.Navy
    )

    val ADMIRALS_COMMAND_WHISTLE = Item(
        id = "admirals_whistle",
        name = "Admiral's Command Whistle",
        description = "The sound of authority. Boosts the morale of nearby allies.",
        type = ItemType.Accessory,
        rarity = Rarity.Epic,
        price = 25000, // In Justice Points
        statBonus = Stats(willpower = 40.0, perception = 10.0),
        levelRequirement = 50,
        factionRequirement = Faction.Navy
    )

    // Ingredients
    val SALT = Item(
        id = "salt",
        name = "Salt",
        description = "Essential for preserving and seasoning food.",
        type = ItemType.Ingredient,
        rarity = Rarity.Common,
        price = 5
    )

    val SPICES = Item(
        id = "spices",
        name = "Spices",
        description = "Exotic spices from distant lands.",
        type = ItemType.Ingredient,
        rarity = Rarity.Uncommon,
        price = 50
    )

    val SEAWEED = Item(
        id = "seaweed",
        name = "Seaweed",
        description = "Nutritious but slimy. Good for soups.",
        type = ItemType.Ingredient,
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

    // Ships
    val SLOOP = Item(
        id = "sloop",
        name = "Sloop",
        description = "A fast and maneuverable one-masted ship.",
        type = ItemType.Ship,
        rarity = Rarity.Uncommon,
        price = 500,
        levelRequirement = 10
    )

    val CARAVEL = Item(
        id = "caravel",
        name = "Caravel",
        description = "A sturdy vessel capable of longer voyages.",
        type = ItemType.Ship,
        rarity = Rarity.Rare,
        price = 2500,
        levelRequirement = 20
    )

    val GALLEON = Item(
        id = "galleon",
        name = "Galleon",
        description = "A massive warship with heavy firepower.",
        type = ItemType.Ship,
        rarity = Rarity.Epic,
        price = 10000,
        levelRequirement = 40
    )

    val allItems = listOf(
        IRON_SWORD, FLINTLOCK_PISTOL, WOODEN_SPEAR, CURSED_BLADE_OF_SORROW, POSEIDON_TRIDENT, LONG_RANGE_RIFLE,
        NAVY_SABER, NAVY_CARBINE, CURSED_CUTLASS, PLUNDERERS_PISTOL,
        IRON_SPEAR, TRIDENT_OF_THE_DEEP, PIRATE_MUSKET, DOUBLE_PISTOL,
        LEATHER_VEST, IRON_CHESTPLATE, NAVY_UNIFORM, NAVY_OFFICER_UNIFORM, JUSTICE_CAPE, SEA_CAPTAIN_COAT, REINFORCED_BOOTS, STEEL_GLOVES,
        AEGIS_SHIELD, IRON_HELMET, PIRATE_CLOAK, SMUGGLERS_CLOAK, MARINE_MEDAL_OF_VALOR, ADMIRALS_COMMAND_WHISTLE,
        SALT, SPICES, SEAWEED, RAW_FISH, COOKED_FISH, SEA_STEW,
        SMALL_BAG, MEDIUM_BAG, LARGE_BAG, LEGENDARY_BAG,
        ARTIFACT_F, ARTIFACT_E, ARTIFACT_D, ARTIFACT_C, ARTIFACT_B,
        ARTIFACT_A, ARTIFACT_S, ARTIFACT_SS, ARTIFACT_SSS, ARTIFACT_Z,
        BASIC_LURE, HEAVY_LURE, GLOWING_LURE,
        SLOOP, CARAVEL, GALLEON
    )
}
