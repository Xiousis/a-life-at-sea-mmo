import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// --- Constants & Config ---
const ENERGY_REGEN_RATE_MS = 3 * 60 * 1000; // 1 energy per 3 minutes
const MYTHIC_MANA_REGEN_RATE_MS = 2 * 1000; // 1 mana per 2 seconds
const MAX_ENERGY = 100;
const BASE_INVENTORY_CAPACITY = 20;
const TURN_TIMEOUT_MS = 60 * 1000; // 1 minute per turn
const HEALING_DURATION_MS = 2 * 60 * 1000; // 2 minutes
const TRAINING_DURATION_MS = 5 * 1000; // 5 seconds
const TRAINING_BASE_GOLD_COST = 10;
const MYTHIC_ROLL_GOLD_COST = 1000000;
const ROLL_COOLDOWN_MS = 1000;
const MAX_AUCTION_PRICE = 999999999;

const RECIPES: Record<string, any> = {
    "sea_stew": {
        id: "sea_stew",
        name: "Sea Stew",
        levelRequirement: 1,
        ingredients: [
            { itemId: "sardine", quantity: 1, type: "Fish" },
            { itemId: "water_jug", quantity: 1 },
            { itemId: "vegetables", quantity: 1 }
        ],
        result: { id: "sea_stew", name: "Sea Stew", type: "Food", healAmount: 50, rarity: "Common", price: 100 }
    },
    "spiced_fish": {
        id: "spiced_fish",
        name: "Spiced Grilled Fish",
        levelRequirement: 5,
        ingredients: [
            { itemId: "salmon", quantity: 1, type: "Fish" },
            { itemId: "spices", quantity: 1 },
            { itemId: "salt", quantity: 1 }
        ],
        result: { id: "spiced_fish", name: "Spiced Grilled Fish", type: "Food", healAmount: 120, rarity: "Uncommon", price: 250 }
    },
    "pirate_feast": {
        id: "pirate_feast",
        name: "Grand Pirate Feast",
        levelRequirement: 15,
        ingredients: [
            { itemId: "meat_chunk", quantity: 2 },
            { itemId: "tuna", quantity: 1, type: "Fish" },
            { itemId: "spices", quantity: 2 },
            { itemId: "vegetables", quantity: 2 }
        ],
        result: { id: "pirate_feast", name: "Grand Pirate Feast", type: "Food", healAmount: 500, rarity: "Rare", price: 1000 }
    }
};

const FISH_TYPES: Record<string, any> = {
    "sardine": { name: "Sardine", price: 10, healAmount: 5, weight: 60 },
    "mackerel": { name: "Mackerel", price: 20, healAmount: 8, weight: 25 },
    "salmon": { name: "Salmon", price: 50, healAmount: 12, weight: 10 },
    "tuna": { name: "Tuna", price: 100, healAmount: 20, weight: 4 },
    "swordfish": { name: "Swordfish", price: 250, healAmount: 35, weight: 0.9 },
    "kraken_tentacle": { name: "Kraken Tentacle", price: 1000, healAmount: 60, weight: 0.1 }
};

const STATIC_TECHNIQUES: Record<string, any> = {
    "bash": { id: "bash", name: "Bash", description: "A simple but effective physical strike using whatever you have on hand.", type: "Brawling", power: 1.0, energyCost: 5, cooldown: 0, element: null },
    "Horizontal Slash": { id: "Horizontal Slash", name: "Horizontal Slash", description: "A wide, sweeping cut that targets the enemy's midsection.", type: "Swordsmanship", power: 1.2, energyCost: 10, cooldown: 0, element: "Earth" },
    "Dash": { id: "Dash", name: "Dash", description: "A sudden burst of speed used to close the gap or evade an attack.", type: "Agility", power: 0.8, energyCost: 8, cooldown: 1, element: "Air" },
    "Point Strike": { id: "Point Strike", name: "Point Strike", description: "A precise thrust aimed at vital points.", type: "Swordsmanship", power: 1.5, energyCost: 15, cooldown: 1, element: "Earth" },
    "Deep Cut": { id: "Deep Cut", name: "Deep Cut", description: "A powerful slash that leaves a lasting wound.", type: "Swordsmanship", power: 1.8, energyCost: 20, cooldown: 2, element: "Earth" },
    "Iron Wall": { id: "Iron Wall", name: "Iron Wall", description: "Hardening your body or defense to negate incoming force.", type: "Endurance", power: 0.5, energyCost: 15, cooldown: 3, element: "Earth" },
    "Bolt Strike": { id: "Bolt Strike", name: "Bolt Strike", description: "Infusing your strike with electric energy to shock the target.", type: "MysticArts", power: 2.0, energyCost: 25, cooldown: 2, element: "Lightning" },
    "One Strike": { id: "One Strike", name: "One Strike", description: "The absolute pinnacle of focus. A single hit that decides the battle.", type: "Swordsmanship", power: 10.0, energyCost: 80, cooldown: 10, element: "Divine" },
    "Cosmic Tear": { id: "Cosmic Tear", name: "Cosmic Tear", description: "Ripping through the fabric of space to erase the enemy.", type: "MysticArts", power: 15.0, energyCost: 100, cooldown: 15, element: "Celestial" },
    "Annihilation: Void Burst": { id: "Annihilation: Void Burst", name: "Void Burst", description: "A localized explosion of absolute nothingness.", type: "MysticArts", power: 50.0, energyCost: 200, cooldown: 20, element: "Annihilation" },
    "Sturdy Block": { id: "Sturdy Block", name: "Sturdy Block", description: "A defensive maneuver using the blade's flat side.", type: "Swordsmanship", power: 0.5, energyCost: 10, cooldown: 1, element: "Earth" },
    "Heavy Chop": { id: "Heavy Chop", name: "Heavy Chop", description: "A vertical strike with significant weight behind it.", type: "Swordsmanship", power: 1.1, energyCost: 10, cooldown: 1, element: "Earth" },
    "Calm State": { id: "Calm State", name: "Calm State", description: "A mental focus that prepares the user for combat.", type: "Willpower", power: 0.0, energyCost: 10, cooldown: 1, element: "Light" },
    "Wild Swing": { id: "Wild Swing", name: "Wild Swing", description: "An unpredictable, powerful attack.", type: "Swordsmanship", power: 1.3, energyCost: 10, cooldown: 1, element: "Light" },
    "Distraction": { id: "Distraction", name: "Distraction", description: "A showy move to confuse the opponent.", type: "Luck", power: 0.5, energyCost: 10, cooldown: 1, element: "Air" },
    "Pull": { id: "Pull", name: "Pull", description: "Using strength to drag the enemy closer.", type: "Strength", power: 0.8, energyCost: 10, cooldown: 1, element: "Water" },
    "Brace": { id: "Brace", name: "Brace", description: "Preparing for an incoming blow.", type: "Endurance", power: 0.5, energyCost: 10, cooldown: 1, element: "Water" },
    "Evasion": { id: "Evasion", name: "Evasion", description: "A swift dodge to avoid damage.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Air" },
    "Pre-empt": { id: "Pre-empt", name: "Pre-empt", description: "Anticipating the enemy's next move.", type: "Perception", power: 0.5, energyCost: 10, cooldown: 1, element: "Lightning" },
    "Unshakable": { id: "Unshakable", name: "Unshakable", description: "Maintaining resolve under pressure.", type: "Willpower", power: 0.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Double or Nothing": { id: "Double or Nothing", name: "Double or Nothing", description: "A risky attack that could deal massive damage.", type: "Luck", power: 2.0, energyCost: 10, cooldown: 1, element: "Chaos" },
    "Double Slash": { id: "Double Slash", name: "Double Slash", description: "Two quick strikes in rapid succession.", type: "Swordsmanship", power: 1.4, energyCost: 10, cooldown: 1, element: "Earth" },
    "Slam": { id: "Slam", name: "Slam", description: "A forceful impact using body weight.", type: "Strength", power: 1.2, energyCost: 10, cooldown: 1, element: "Earth" },
    "Precision Hit": { id: "Precision Hit", name: "Precision Hit", description: "Targeting a weak point with great accuracy.", type: "Perception", power: 1.3, energyCost: 10, cooldown: 1, element: "Lightning" },
    "Stampede": { id: "Stampede", name: "Stampede", description: "A reckless charge into the enemy.", type: "Strength", power: 1.4, energyCost: 10, cooldown: 1, element: "Fire" },
    "Flowing Strike": { id: "Flowing Strike", name: "Flowing Strike", description: "A fluid, continuous attack.", type: "Swordsmanship", power: 1.5, energyCost: 10, cooldown: 1, element: "Water" },
    "Immovable": { id: "Immovable", name: "Immovable", description: "Standing firm against any force.", type: "Endurance", power: 0.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Cyclone": { id: "Cyclone", name: "Cyclone", description: "A spinning attack that creates a vortex.", type: "Swordsmanship", power: 1.6, energyCost: 10, cooldown: 1, element: "Air" },
    "Focused Fire": { id: "Focused Fire", name: "Focused Fire", description: "Concentrated attacks on a single target.", type: "Sniper", power: 1.7, energyCost: 10, cooldown: 1, element: "Lightning" },
    "Grip Smash": { id: "Grip Smash", name: "Grip Smash", description: "A crushing blow using martial prowess.", type: "MartialArts", power: 1.5, energyCost: 10, cooldown: 1, element: "Earth" },
    "Shadow Strike": { id: "Shadow Strike", name: "Shadow Strike", description: "An attack from the shadows.", type: "Agility", power: 1.6, energyCost: 10, cooldown: 1, element: "Air" },
    "Air Piercer": { id: "Air Piercer", name: "Air Piercer", description: "A thrust that pierces the very air.", type: "Spear", power: 1.6, energyCost: 10, cooldown: 1, element: "Air" },
    "Disarm": { id: "Disarm", name: "Disarm", description: "A technique to strip the enemy of their weapon.", type: "Agility", power: 0.5, energyCost: 10, cooldown: 1, element: "Water" },
    "Shockwave": { id: "Shockwave", name: "Shockwave", description: "An impact that sends ripples through the ground.", type: "Strength", power: 1.5, energyCost: 10, cooldown: 1, element: "Lightning" },
    "Flicker": { id: "Flicker", name: "Flicker", description: "A move so fast it leaves an afterimage.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Air" },
    "Water Slicer": { id: "Water Slicer", name: "Water Slicer", description: "A cut as sharp and fluid as water.", type: "Swordsmanship", power: 1.8, energyCost: 10, cooldown: 1, element: "Water" },
    "Bone Breaker": { id: "Bone Breaker", name: "Bone Breaker", description: "A strike aimed at shattering bone.", type: "Strength", power: 2.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Breeze Step": { id: "Breeze Step", name: "Breeze Step", description: "Movement as light as a breeze.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Air" },
    "True Vision": { id: "True Vision", name: "True Vision", description: "Seeing the reality behind illusions.", type: "Perception", power: 0.0, energyCost: 10, cooldown: 1, element: "Lightning" },
    "Purge": { id: "Purge", name: "Purge", description: "Cleansing the area of negative energy.", type: "MysticArts", power: 1.5, energyCost: 10, cooldown: 1, element: "Light" },
    "Bleed Out": { id: "Bleed Out", name: "Bleed Out", description: "An attack that causes severe bleeding.", type: "Swordsmanship", power: 1.6, energyCost: 10, cooldown: 1, element: "Dark" },
    "Tremor": { id: "Tremor", name: "Tremor", description: "A strike that causes the ground to shake.", type: "Strength", power: 1.7, energyCost: 10, cooldown: 1, element: "Earth" },
    "Sand Trap": { id: "Sand Trap", name: "Sand Trap", description: "Trapping the enemy in shifting sands.", type: "Agility", power: 1.0, energyCost: 10, cooldown: 1, element: "Air" },
    "Rebirth": { id: "Rebirth", name: "Rebirth", description: "Rising from the ashes with new power.", type: "Willpower", power: 0.0, energyCost: 10, cooldown: 1, element: "Fire" },
    "Fire Slash": { id: "Fire Slash", name: "Fire Slash", description: "A blade wreathed in flames.", type: "MysticArts", power: 2.2, energyCost: 10, cooldown: 1, element: "Fire" },
    "Heat Haze": { id: "Heat Haze", name: "Heat Haze", description: "Distorting the air with intense heat.", type: "MysticArts", power: 1.5, energyCost: 10, cooldown: 1, element: "Fire" },
    "Colossus Strike": { id: "Colossus Strike", name: "Colossus Strike", description: "A strike with the force of a giant.", type: "Strength", power: 2.5, energyCost: 10, cooldown: 1, element: "Earth" },
    "Earth Breaker": { id: "Earth Breaker", name: "Earth Breaker", description: "A blow that shatters the very ground.", type: "Strength", power: 2.5, energyCost: 10, cooldown: 1, element: "Earth" },
    "Flash Step": { id: "Flash Step", name: "Flash Step", description: "A movement faster than sight.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Lightning" },
    "Afterimage": { id: "Afterimage", name: "Afterimage", description: "Leaving a lingering image to deceive.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Lightning" },
    "Prevision": { id: "Prevision", name: "Prevision", description: "Glimpsing the immediate future.", type: "Perception", power: 0.0, energyCost: 10, cooldown: 1, element: "Light" },
    "Mind Link": { id: "Mind Link", name: "Mind Link", description: "Connecting minds for strategic advantage.", type: "Perception", power: 0.0, energyCost: 10, cooldown: 1, element: "Light" },
    "Nullify": { id: "Nullify", name: "Nullify", description: "Erasing incoming magical or spiritual effects.", type: "Willpower", power: 0.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Gravity Field": { id: "Gravity Field", name: "Gravity Field", description: "Manipulating gravity to pin enemies.", type: "Willpower", power: 1.5, energyCost: 10, cooldown: 1, element: "Void" },
    "Destiny Strike": { id: "Destiny Strike", name: "Destiny Strike", description: "An attack guided by fate.", type: "Luck", power: 2.5, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Jackpot": { id: "Jackpot", name: "Jackpot", description: "A lucky hit with extreme results.", type: "Luck", power: 5.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Ice Prison": { id: "Ice Prison", name: "Ice Prison", description: "Entrapping the enemy in a block of ice.", type: "MysticArts", power: 2.0, energyCost: 10, cooldown: 1, element: "Ice" },
    "Glacial Wall": { id: "Glacial Wall", name: "Glacial Wall", description: "Creating a massive wall of ice.", type: "MysticArts", power: 0.5, energyCost: 10, cooldown: 1, element: "Ice" },
    "Flood": { id: "Flood", name: "Flood", description: "A wave of water that overwhelms the enemy.", type: "Swordsmanship", power: 2.2, energyCost: 10, cooldown: 1, element: "Water" },
    "Tidal Wave": { id: "Tidal Wave", name: "Tidal Wave", description: "A massive surge of aquatic energy.", type: "Swordsmanship", power: 2.5, energyCost: 10, cooldown: 1, element: "Water" },
    "Dark Bind": { id: "Dark Bind", name: "Dark Bind", description: "Using shadows to restrict movement.", type: "MysticArts", power: 1.5, energyCost: 10, cooldown: 1, element: "Dark" },
    "Nightmare": { id: "Nightmare", name: "Nightmare", description: "Invading the enemy's mind with terror.", type: "MysticArts", power: 2.0, energyCost: 10, cooldown: 1, element: "Dark" },
    "Starfall": { id: "Starfall", name: "Starfall", description: "Calling down celestial fire.", type: "MysticArts", power: 2.8, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Sunbeam": { id: "Sunbeam", name: "Sunbeam", description: "A ray of concentrated sunlight.", type: "MysticArts", power: 2.8, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Black Hole": { id: "Black Hole", name: "Black Hole", description: "A singularity that consumes everything.", type: "MysticArts", power: 4.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Nova": { id: "Nova", name: "Nova", description: "A star going supernova.", type: "MysticArts", power: 5.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Heavenly Smash": { id: "Heavenly Smash", name: "Heavenly Smash", description: "A strike from the heavens themselves.", type: "Strength", power: 3.5, energyCost: 10, cooldown: 1, element: "Earth" },
    "Sky Cracker": { id: "Sky Cracker", name: "Sky Cracker", description: "A blow that shatters the horizon.", type: "Strength", power: 4.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Final Pillar": { id: "Final Pillar", name: "Final Pillar", description: "The ultimate weight of the world.", type: "Strength", power: 5.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Time Warp": { id: "Time Warp", name: "Time Warp", description: "Bending the flow of time.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Stutter": { id: "Stutter", name: "Stutter", description: "Displacing oneself in time.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Future Echo": { id: "Future Echo", name: "Future Echo", description: "An attack that resonates from the future.", type: "Agility", power: 2.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Soul Rend": { id: "Soul Rend", name: "Soul Rend", description: "Tearing at the opponent's spiritual essence.", type: "MysticArts", power: 4.0, energyCost: 10, cooldown: 1, element: "Dark" },
    "Spirit Bind": { id: "Spirit Bind", name: "Spirit Bind", description: "Restricting the target's soul.", type: "MysticArts", power: 3.0, energyCost: 10, cooldown: 1, element: "Dark" },
    "Essence Theft": { id: "Essence Theft", name: "Essence Theft", description: "Stealing the life force of the enemy.", type: "MysticArts", power: 3.5, energyCost: 10, cooldown: 1, element: "Dark" },
    "Mirror Shield": { id: "Mirror Shield", name: "Mirror Shield", description: "A defense that reflects incoming attacks.", type: "Endurance", power: 0.5, energyCost: 10, cooldown: 1, element: "Earth" },
    "Fortress": { id: "Fortress", name: "Fortress", description: "Becoming an unassailable bastion.", type: "Endurance", power: 0.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Aegis": { id: "Aegis", name: "Aegis", description: "The divine shield that protects all.", type: "Endurance", power: 1.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Overawe": { id: "Overawe", name: "Overawe", description: "Overwhelming the enemy with sheer presence.", type: "Willpower", power: 2.0, energyCost: 10, cooldown: 1, element: "Divine" },
    "Command": { id: "Command", name: "Command", description: "Dictating the flow of battle.", type: "Willpower", power: 0.0, energyCost: 10, cooldown: 1, element: "Divine" },
    "Domination": { id: "Domination", name: "Domination", description: "Asserting absolute control over the target.", type: "Willpower", power: 3.0, energyCost: 10, cooldown: 1, element: "Divine" },
    "Entangle": { id: "Entangle", name: "Entangle", description: "Using nature to bind the opponent.", type: "MysticArts", power: 2.0, energyCost: 10, cooldown: 1, element: "Earth" },
    "Root Spike": { id: "Root Spike", name: "Root Spike", description: "Piercing the enemy with wooden spikes.", type: "MysticArts", power: 3.0, energyCost: 10, cooldown: 1, element: "Earth" },
    // Navy Rokushiki
    "Soru": { id: "Soru", name: "Soru", description: "A high-speed movement technique that makes the user disappear. Massive dodge bonus.", type: "Agility", power: 0.0, energyCost: 15, cooldown: 2, element: "Air", factionRequirement: "Navy" },
    "Tekkai": { id: "Tekkai", name: "Tekkai", description: "Hardening the body to the density of iron. Massive defense boost.", type: "Endurance", power: 0.0, energyCost: 20, cooldown: 3, element: "Earth", factionRequirement: "Navy" },
    "Rankyaku": { id: "Rankyaku", name: "Rankyaku", description: "A powerful projectile kick that creates a sharp compressed air blade.", type: "MartialArts", power: 2.5, energyCost: 25, cooldown: 2, element: "Air", factionRequirement: "Navy" },
    // Pirate Dirty Fighting
    "Pocket Sand": { id: "Pocket Sand", name: "Pocket Sand", description: "Throwing sand into the enemy's eyes to blind them.", type: "Luck", power: 0.5, energyCost: 10, cooldown: 2, element: "Earth", factionRequirement: "Pirate" },
    "Low Blow": { id: "Low Blow", name: "Low Blow", description: "A cheap shot that stuns and weakens the enemy.", type: "Brawling", power: 1.5, energyCost: 15, cooldown: 3, element: "Physical", factionRequirement: "Pirate" },
    "Dirty Distraction": { id: "Dirty Distraction", name: "Dirty Distraction", description: "Using underhanded tricks to create an opening.", type: "Luck", power: 0.0, energyCost: 10, cooldown: 1, element: "Chaos", factionRequirement: "Pirate" },
    "Thorn Hail": { id: "Thorn Hail", name: "Thorn Hail", description: "A barrage of sharp thorns.", type: "MysticArts", power: 3.5, energyCost: 10, cooldown: 1, element: "Earth" },
    "Miracle": { id: "Miracle", name: "Miracle", description: "A phenomenon that defies logic.", type: "Luck", power: 10.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Lucky Break": { id: "Lucky Break", name: "Lucky Break", description: "Finding a sudden opening in the enemy's defense.", type: "Luck", power: 3.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Twist of Fate": { id: "Twist of Fate", name: "Twist of Fate", description: "Changing the outcome of a situation.", type: "Luck", power: 4.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Sunburst": { id: "Sunburst", name: "Sunburst", description: "An explosion of solar energy.", type: "MysticArts", power: 4.5, energyCost: 10, cooldown: 1, element: "Fire" },
    "Blinding Light": { id: "Blinding Light", name: "Blinding Light", description: "A flash of light that dazzles all.", type: "MysticArts", power: 1.0, energyCost: 10, cooldown: 1, element: "Fire" },
    "Solar Storm": { id: "Solar Storm", name: "Solar Storm", description: "A continuous barrage of fire from the sun.", type: "MysticArts", power: 5.0, energyCost: 10, cooldown: 1, element: "Fire" },
    "Devour": { id: "Devour", name: "Devour", description: "Consuming the target whole.", type: "MysticArts", power: 6.0, energyCost: 10, cooldown: 1, element: "Dark" },
    "Void Pull": { id: "Void Pull", name: "Void Pull", description: "Dragging the enemy into the void.", type: "MysticArts", power: 4.0, energyCost: 10, cooldown: 1, element: "Dark" },
    "Darkness Falls": { id: "Darkness Falls", name: "Darkness Falls", description: "Enveloping the battlefield in absolute dark.", type: "MysticArts", power: 3.0, energyCost: 10, cooldown: 1, element: "Dark" },
    "Sonic Boom": { id: "Sonic Boom", name: "Sonic Boom", description: "A shockwave created by exceeding the speed of sound.", type: "Agility", power: 6.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Infinite Afterimage": { id: "Infinite Afterimage", name: "Infinite Afterimage", description: "Filling the area with countless clones.", type: "Agility", power: 0.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Great Divide": { id: "Great Divide", name: "Great Divide", description: "A strike that splits the very world.", type: "Swordsmanship", power: 12.0, energyCost: 10, cooldown: 1, element: "Chaos" },
    "Earth Quake": { id: "Earth Quake", name: "Earth Quake", description: "A massive seismic shift.", type: "Strength", power: 10.0, energyCost: 10, cooldown: 1, element: "Chaos" },
    "Soul Suck": { id: "Soul Suck", name: "Soul Suck", description: "Draining the spiritual essence of others.", type: "MysticArts", power: 8.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Spirit Explosion": { id: "Spirit Explosion", name: "Spirit Explosion", description: "A violent release of soul energy.", type: "MysticArts", power: 12.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Frozen Domain": { id: "Frozen Domain", name: "Frozen Domain", description: "Freezing an entire region.", type: "MysticArts", power: 8.0, energyCost: 10, cooldown: 1, element: "Ice" },
    "Shatter": { id: "Shatter", name: "Shatter", description: "Breaking the target into a million pieces.", type: "Strength", power: 12.0, energyCost: 10, cooldown: 1, element: "Ice" },
    "Fate's Seal": { id: "Fate's Seal", name: "Fate's Seal", description: "Locking the target's destiny.", type: "Luck", power: 0.0, energyCost: 10, cooldown: 1, element: "Divine" },
    "Unstoppable Force": { id: "Unstoppable Force", name: "Unstoppable Force", description: "A momentum that cannot be broken.", type: "Willpower", power: 15.0, energyCost: 10, cooldown: 1, element: "Divine" },
    "Entropy": { id: "Entropy", name: "Entropy", description: "Accelerating the decay of the universe.", type: "MysticArts", power: 20.0, energyCost: 10, cooldown: 1, element: "Chaos" },
    "Butterfly Effect": { id: "Butterfly Effect", name: "Butterfly Effect", description: "Small changes leading to catastrophic results.", type: "Luck", power: 15.0, energyCost: 10, cooldown: 1, element: "Chaos" },
    "Singularity": { id: "Singularity", name: "Singularity", description: "The point of infinite density.", type: "MysticArts", power: 25.0, energyCost: 10, cooldown: 1, element: "Chaos" },
    "Ascension": { id: "Ascension", name: "Ascension", description: "Rising to a higher state of existence.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Holy Rain": { id: "Holy Rain", name: "Holy Rain", description: "A deluge of divine energy.", type: "MysticArts", power: 20.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Judgment": { id: "Judgment", name: "Judgment", description: "The final verdict.", type: "Willpower", power: 30.0, energyCost: 10, cooldown: 1, element: "Celestial" },
    "Erasure": { id: "Erasure", name: "Erasure", description: "Wiping the target from existence.", type: "MysticArts", power: 100.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Non-Existence": { id: "Non-Existence", name: "Non-Existence", description: "A state of being that is not.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Dark Matter": { id: "Dark Matter", name: "Dark Matter", description: "The invisible substance that binds the galaxy.", type: "MysticArts", power: 25.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Creation": { id: "Creation", name: "Creation", description: "Manifesting matter from nothing.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Genesis" },
    "Renewal": { id: "Renewal", name: "Renewal", description: "Restoring what was lost.", type: "Willpower", power: 0.0, energyCost: 10, cooldown: 1, element: "Genesis" },
    "Alpha Strike": { id: "Alpha Strike", name: "Alpha Strike", description: "The first and final blow.", type: "Swordsmanship", power: 50.0, energyCost: 10, cooldown: 1, element: "Genesis" },
    "Universal Cut": { id: "Universal Cut", name: "Universal Cut", description: "A slash that spans the universe.", type: "Swordsmanship", power: 200.0, energyCost: 10, cooldown: 1, element: "Divine" },
    "End of All": { id: "End of All", name: "End of All", description: "The final conclusion to everything.", type: "MysticArts", power: 500.0, energyCost: 10, cooldown: 1, element: "Divine" },
    "Finality": { id: "Finality", name: "Finality", description: "The absolute end.", type: "MysticArts", power: 1000.0, energyCost: 10, cooldown: 1, element: "Void" },
    "Rewrite": { id: "Rewrite", name: "Rewrite", description: "Changing the story of the world.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Delete": { id: "Delete", name: "Delete", description: "Erasing the target from the world's record.", type: "MysticArts", power: 9999.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Absolute Command": { id: "Absolute Command", name: "Absolute Command", description: "A word that must be obeyed by reality.", type: "Willpower", power: 0.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Annihilation: Reality Erasure": { id: "Annihilation: Reality Erasure", name: "Reality Erasure", description: "Tearing down the walls of reality.", type: "MysticArts", power: 100.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Soul Grasp": { id: "Annihilation: Soul Grasp", name: "Soul Grasp", description: "Crushing the target's soul in your hand.", type: "MysticArts", power: 100.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: World's End": { id: "Annihilation: World's End", name: "World's End", description: "Bringing about the apocalypse.", type: "MysticArts", power: 200.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Abyssal Gaze": { id: "Annihilation: Abyssal Gaze", name: "Abyssal Gaze", description: "A stare that drains all hope.", type: "MysticArts", power: 50.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Dark Matter Crush": { id: "Annihilation: Dark Matter Crush", name: "Dark Matter Crush", description: "Using dark matter to compress the enemy.", type: "MysticArts", power: 150.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Entropy Pulse": { id: "Annihilation: Entropy Pulse", name: "Entropy Pulse", description: "A wave of pure decay.", type: "MysticArts", power: 120.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Singularity Strike": { id: "Annihilation: Singularity Strike", name: "Singularity Strike", description: "An impact with infinite mass.", type: "MysticArts", power: 180.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Oblivion Wave": { id: "Annihilation: Oblivion Wave", name: "Oblivion Wave", description: "A surge of nothingness.", type: "MysticArts", power: 140.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Shadow Reign": { id: "Annihilation: Shadow Reign", name: "Shadow Reign", description: "Ruling the battlefield with darkness.", type: "MysticArts", power: 100.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Ruin": { id: "Annihilation: Ruin", name: "Ruin", description: "Complete destruction.", type: "MysticArts", power: 300.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Decay": { id: "Annihilation: Decay", name: "Decay", description: "The slow rot of everything.", type: "MysticArts", power: 80.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Despair": { id: "Annihilation: Despair", name: "Despair", description: "The ultimate mental burden.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Chaos Bolt": { id: "Annihilation: Chaos Bolt", name: "Chaos Bolt", description: "A bolt of unpredictable energy.", type: "MysticArts", power: 110.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Annihilation: Ultimate Zero": { id: "Annihilation: Ultimate Zero", name: "Ultimate Zero", description: "The return to nothing.", type: "MysticArts", power: 1000.0, energyCost: 10, cooldown: 1, element: "Annihilation" },
    "Creation: Genesis Flash": { id: "Creation: Genesis Flash", name: "Genesis Flash", description: "The light of the first dawn.", type: "MysticArts", power: 100.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Life Weaver": { id: "Creation: Life Weaver", name: "Life Weaver", description: "Manipulating the threads of life.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Stellar Birth": { id: "Creation: Stellar Birth", name: "Stellar Birth", description: "The creation of a new star.", type: "MysticArts", power: 150.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Infinite Bloom": { id: "Creation: Infinite Bloom", name: "Infinite Bloom", description: "The sudden growth of life.", type: "MysticArts", power: 50.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Holy Radiance": { id: "Creation: Holy Radiance", name: "Holy Radiance", description: "A blindingly pure light.", type: "MysticArts", power: 100.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Divine Structure": { id: "Creation: Divine Structure", name: "Divine Structure", description: "The blueprint of existence.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Harmony Strike": { id: "Creation: Harmony Strike", name: "Harmony Strike", description: "An attack that resonates with the world.", type: "MysticArts", power: 120.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Eternal Dawn": { id: "Creation: Eternal Dawn", name: "Eternal Dawn", description: "The beginning that never ends.", type: "MysticArts", power: 140.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Cosmic Pulse": { id: "Creation: Cosmic Pulse", name: "Cosmic Pulse", description: "The heartbeat of the universe.", type: "MysticArts", power: 110.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Seraphim's Gaze": { id: "Creation: Seraphim's Gaze", name: "Seraphim's Gaze", description: "The watchful eye of high heavens.", type: "MysticArts", power: 80.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Restoration": { id: "Creation: Restoration", name: "Restoration", description: "Returning things to their original state.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Sanctity": { id: "Creation: Sanctity", name: "Sanctity", description: "A holy presence that wards off evil.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Purity": { id: "Creation: Purity", name: "Purity", description: "The essence of absolute cleanliness.", type: "MysticArts", power: 0.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Luminescence": { id: "Creation: Luminescence", name: "Luminescence", description: "A soft, guiding light.", type: "MysticArts", power: 60.0, energyCost: 10, cooldown: 1, element: "Creation" },
    "Creation: Omega Spark": { id: "Creation: Omega Spark", name: "Omega Spark", description: "The final spark of creation.", type: "MysticArts", power: 500.0, energyCost: 10, cooldown: 1, element: "Creation" }
};

const WORLD_BOSSES = [
    { name: "Abyssal Kraken", level: 50, hp: 10000, maxHp: 10000, stats: { strength: 50.0, endurance: 100.0 } },
    { name: "Ancient Sea Dragon", level: 120, hp: 50000, maxHp: 50000, stats: { strength: 150.0, endurance: 300.0 } },
    { name: "Ghost Captain Silvereye", level: 180, hp: 150000, maxHp: 150000, stats: { strength: 300.0, endurance: 600.0 } },
    { name: "Leviathan of the Void", level: 250, hp: 500000, maxHp: 500000, stats: { strength: 600.0, endurance: 1200.0 } },
    { name: "The Sunken God", level: 300, hp: 1000000, maxHp: 1000000, stats: { strength: 1000.0, endurance: 2000.0 } }
];

const RAID_LOCATIONS = [
    "Sunken Reef",
    "Shadow Fen",
    "Kraken's Rest",
    "Pirate's Den",
    "Crystal Cove",
    "Volcano Peak",
    "Serpent's Maw"
];

function determineCaughtFish(level: number): string {
    // Adjust weights based on level: higher level = better fish
    const weights: Record<string, number> = { ...FISH_TYPES };
    const sardineWeight = Math.max(5, FISH_TYPES.sardine.weight - level);
    const krakenWeight = FISH_TYPES.kraken_tentacle.weight + (level * 0.01);
    const swordfishWeight = FISH_TYPES.swordfish.weight + (level * 0.05);

    const adjustedWeights: any = {
        "sardine": sardineWeight,
        "mackerel": FISH_TYPES.mackerel.weight,
        "salmon": FISH_TYPES.salmon.weight,
        "tuna": FISH_TYPES.tuna.weight,
        "swordfish": swordfishWeight,
        "kraken_tentacle": krakenWeight
    };

    const totalWeight = Object.values(adjustedWeights).reduce((a: any, b: any) => a + b, 0) as number;
    const rand = Math.random() * totalWeight;
    let cumulative = 0;
    for (const id of Object.keys(adjustedWeights)) {
        cumulative += adjustedWeights[id];
        if (rand < cumulative) return id;
    }
    return "sardine";
}

const MYTHIC_ARTS: Record<string, Array<{
    name: string,
    description: string,
    stats: any,
    skillMultiplier: number,
    multipliedSkill: string,
    techniques: string[],
    hugeBuffType?: string,
    hugeBuffValue?: number,
    debuffPercentage?: number,
    energyRegainMultiplier?: number,
    weakAgainst?: string[],
    elements: string[],
    elementalWeaknesses?: string[],
    travelTimeMultiplier?: number,
    canLearnNonCombatSkills?: boolean,
    restrictedSkillTypes?: string[]
}>> = {
    "F": [
        { name: "Novice Strike", description: "A basic strike taught to every beginner.", stats: { strength: 1 }, skillMultiplier: 1.10, multipliedSkill: "Swordsmanship", techniques: ["Horizontal Slash"], hugeBuffType: "Strength", hugeBuffValue: 0.05, weakAgainst: ["MartialArts"], elements: ["Physical"], elementalWeaknesses: ["Air"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Rusty Guard", description: "Using a worn blade to deflect blows.", stats: { endurance: 1 }, skillMultiplier: 1.10, multipliedSkill: "Blacksmith", techniques: ["Sturdy Block"], hugeBuffType: "Endurance", hugeBuffValue: 0.05, weakAgainst: ["Brawling"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Quick Step", description: "A simple movement to reposition.", stats: { agility: 1 }, skillMultiplier: 1.10, multipliedSkill: "Navigating", techniques: ["Dash"], hugeBuffType: "Agility", hugeBuffValue: 0.05, weakAgainst: ["Sniper"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Dull Edge", description: "Attacking with a poorly maintained weapon.", stats: { strength: 1, agility: 1 }, skillMultiplier: 1.10, multipliedSkill: "Brawling", techniques: ["Heavy Chop"], hugeBuffType: "Strength", hugeBuffValue: 0.05, weakAgainst: ["MartialArts"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Simple Thrust", description: "A straightforward piercing attack.", stats: { perception: 1 }, skillMultiplier: 1.10, multipliedSkill: "Spear", techniques: ["Point Strike"], hugeBuffType: "Perception", hugeBuffValue: 0.05, weakAgainst: ["Swordsmanship"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Steady Breath", description: "Focusing on breathing to maintain stamina.", stats: { willpower: 1 }, skillMultiplier: 1.10, multipliedSkill: "Medical", techniques: ["Calm State"], hugeBuffType: "Willpower", hugeBuffValue: 0.05, weakAgainst: ["Gunslinging"], elements: ["Light"], elementalWeaknesses: ["Dark"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Lucky Swipe", description: "An unplanned attack that somehow lands.", stats: { luck: 1 }, skillMultiplier: 1.10, multipliedSkill: "TreasureHunting", techniques: ["Wild Swing"], hugeBuffType: "Luck", hugeBuffValue: 0.05, weakAgainst: ["Spear"], elements: ["Light"], elementalWeaknesses: ["Dark"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Basic Flourish", description: "A simple showy move with no real power.", stats: { agility: 1, luck: 1 }, skillMultiplier: 1.10, multipliedSkill: "Cooking", techniques: ["Distraction"], hugeBuffType: "Agility", hugeBuffValue: 0.05, weakAgainst: ["Sniper"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Fisherman's Hook", description: "A technique derived from daily chores.", stats: { strength: 1, perception: 1 }, skillMultiplier: 1.10, multipliedSkill: "Fishing", techniques: ["Pull"], hugeBuffType: "Strength", hugeBuffValue: 0.05, weakAgainst: ["MartialArts"], elements: ["Water"], elementalWeaknesses: ["Lightning"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 },
        { name: "Sailor's Balance", description: "Maintaining footing on uneven ground.", stats: { agility: 1, endurance: 1 }, skillMultiplier: 1.10, multipliedSkill: "Navigating", techniques: ["Brace"], hugeBuffType: "Endurance", hugeBuffValue: 0.05, weakAgainst: ["Brawling"], elements: ["Water"], elementalWeaknesses: ["Lightning"], debuffPercentage: 0.40, energyRegainMultiplier: 1.05 }
    ],
    "E": [
        { name: "Steel Bite", description: "A more focused strike that pierces deeper.", stats: { strength: 4 }, skillMultiplier: 1.30, multipliedSkill: "Gunslinging", techniques: ["Deep Cut"], hugeBuffType: "Strength", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Vanguard Defense", description: "A defensive stance used by front-line soldiers.", stats: { endurance: 4 }, skillMultiplier: 1.30, multipliedSkill: "MartialArts", techniques: ["Iron Wall"], hugeBuffType: "Endurance", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Fleet Foot", description: "Agile movements that baffle the inexperienced.", stats: { agility: 4 }, skillMultiplier: 1.30, multipliedSkill: "Sniper", techniques: ["Evasion"], hugeBuffType: "Agility", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Sharpened Senses", description: "Heightened awareness on the battlefield.", stats: { perception: 4 }, skillMultiplier: 1.30, multipliedSkill: "MysticArts", techniques: ["Pre-empt"], hugeBuffType: "Perception", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Lightning"], elementalWeaknesses: ["Earth"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Stone Heart", description: "Resisting fear and mental pressure.", stats: { willpower: 4 }, skillMultiplier: 1.30, multipliedSkill: "Brawling", techniques: ["Unshakable"], hugeBuffType: "Willpower", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Gambler's Strike", description: "A high-risk, high-reward attack.", stats: { luck: 5 }, skillMultiplier: 1.30, multipliedSkill: "TreasureHunting", techniques: ["Double or Nothing"], hugeBuffType: "Luck", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Chaos"], elementalWeaknesses: ["Void"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Twin Fang", description: "A rapid two-hit combination.", stats: { strength: 2, agility: 3 }, skillMultiplier: 1.30, multipliedSkill: "Swordsmanship", techniques: ["Double Slash"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Crushing Weight", description: "Leveraging body weight into a strike.", stats: { strength: 5 }, skillMultiplier: 1.30, multipliedSkill: "Brawling", techniques: ["Slam"], hugeBuffType: "Strength", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Eagle Eye", description: "Spotting weaknesses from a distance.", stats: { perception: 5 }, skillMultiplier: 1.30, multipliedSkill: "Sniper", techniques: ["Precision Hit"], hugeBuffType: "Perception", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Lightning"], elementalWeaknesses: ["Earth"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 },
        { name: "Brave Charge", description: "Rushing forward with reckless abandon.", stats: { willpower: 5, strength: 2 }, skillMultiplier: 1.30, multipliedSkill: "MartialArts", techniques: ["Stampede"], hugeBuffType: "Strength", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], elements: ["Fire"], elementalWeaknesses: ["Water"], debuffPercentage: 0.35, energyRegainMultiplier: 1.10 }
    ],
    "D": [
        { name: "Rippling Blade", description: "A fluid attack that bypasses simple parries.", stats: { swordsmanship: 7, agility: 2 }, skillMultiplier: 1.60, multipliedSkill: "Swordsmanship", techniques: ["Flowing Strike"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Hybrid"], elementalWeaknesses: ["Lightning"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Mountain's Resolve", description: "Standing firm against a tide of enemies.", stats: { endurance: 8, willpower: 2 }, skillMultiplier: 1.60, multipliedSkill: "Blacksmith", techniques: ["Immovable"], hugeBuffType: "Endurance", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Whirlwind Spin", description: "A spinning attack that hits multiple targets.", stats: { agility: 8, strength: 2 }, skillMultiplier: 1.60, multipliedSkill: "Spear", techniques: ["Cyclone"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Hunter's Mark", description: "Tracking a target with lethal intent.", stats: { perception: 8, luck: 2 }, skillMultiplier: 1.60, multipliedSkill: "Sniper", techniques: ["Focused Fire"], hugeBuffType: "Sniper", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Lightning"], elementalWeaknesses: ["Earth"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Iron Fist", description: "Combining martial arts with swordplay.", stats: { martialArts: 7, strength: 3 }, skillMultiplier: 1.60, multipliedSkill: "MartialArts", techniques: ["Grip Smash"], hugeBuffType: "MartialArts", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Silent Step", description: "Moving without a sound to ambush foes.", stats: { agility: 10 }, skillMultiplier: 1.60, multipliedSkill: "TreasureHunting", techniques: ["Shadow Strike"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Piercing Gale", description: "A thrust that carries the force of a gust.", stats: { strength: 10 }, skillMultiplier: 1.60, multipliedSkill: "Spear", techniques: ["Air Piercer"], hugeBuffType: "Spear", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Serpent's Coil", description: "A deceptive technique that traps weapons.", stats: { agility: 7, perception: 3 }, skillMultiplier: 1.60, multipliedSkill: "MysticArts", techniques: ["Disarm"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Water"], elementalWeaknesses: ["Lightning"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Thunderous Clap", description: "An explosive strike that dazes opponents.", stats: { strength: 9, willpower: 3 }, skillMultiplier: 1.60, multipliedSkill: "Brawling", techniques: ["Shockwave"], hugeBuffType: "Strength", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Lightning"], elementalWeaknesses: ["Earth"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 },
        { name: "Mirror Image", description: "A feint that leaves an afterimage.", stats: { agility: 9, luck: 4 }, skillMultiplier: 1.60, multipliedSkill: "MysticArts", techniques: ["Flicker"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.30, energyRegainMultiplier: 1.15 }
    ],
    "C": [
        { name: "Azure Flow", description: "Mastering the rhythm of combat.", stats: { swordsmanship: 12, agility: 5 }, skillMultiplier: 2.00, multipliedSkill: "Swordsmanship", techniques: ["Water Slicer"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Hybrid"], elementalWeaknesses: ["Lightning"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Grizzly Crush", description: "An overwhelming strike with brute force.", stats: { strength: 15, endurance: 5 }, skillMultiplier: 2.00, multipliedSkill: "Brawling", techniques: ["Bone Breaker"], hugeBuffType: "Strength", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Wind Runner", description: "Moving as fast as the breeze.", stats: { agility: 15, luck: 5 }, skillMultiplier: 2.00, multipliedSkill: "Navigating", techniques: ["Breeze Step"], hugeBuffType: "Agility", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Watcher's Gaze", description: "Seeing through illusions and feints.", stats: { perception: 15, willpower: 5 }, skillMultiplier: 2.00, multipliedSkill: "Sniper", techniques: ["True Vision"], hugeBuffType: "Perception", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Lightning"], elementalWeaknesses: ["Earth"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Soul Shield", description: "Protecting the mind from dark arts.", stats: { willpower: 15, mysticArts: 5 }, skillMultiplier: 2.00, multipliedSkill: "MysticArts", techniques: ["Purge"], hugeBuffType: "MysticArts", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Light"], elementalWeaknesses: ["Dark"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Crimson Edge", description: "A blood-soaked blade that thirsts for battle.", stats: { luck: 15, strength: 5 }, skillMultiplier: 2.00, multipliedSkill: "Swordsmanship", techniques: ["Bleed Out"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Hybrid"], elementalWeaknesses: ["Light"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Storm Caller", description: "Infusing attacks with static energy.", stats: { mysticArts: 12, agility: 5 }, skillMultiplier: 2.00, multipliedSkill: "MysticArts", techniques: ["Bolt Strike"], hugeBuffType: "MysticArts", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Lightning"], elementalWeaknesses: ["Earth"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Earth Shaker", description: "Striking the ground to disrupt balance.", stats: { strength: 14, endurance: 6 }, skillMultiplier: 2.00, multipliedSkill: "Blacksmith", techniques: ["Tremor"], hugeBuffType: "Strength", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Desert Mirage", description: "A shimmering technique that hides intent.", stats: { agility: 14, perception: 6 }, skillMultiplier: 2.00, multipliedSkill: "TreasureHunting", techniques: ["Sand Trap"], hugeBuffType: "Agility", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Air"], elementalWeaknesses: ["Ice"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 },
        { name: "Phoenix Rise", description: "Recovering from the brink with newfound vigor.", stats: { willpower: 14, luck: 6 }, skillMultiplier: 2.00, multipliedSkill: "Medical", techniques: ["Rebirth"], hugeBuffType: "Willpower", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], elements: ["Fire"], elementalWeaknesses: ["Water"], debuffPercentage: 0.25, energyRegainMultiplier: 1.20 }
    ],
    "B": [
        { name: "Dragon's Breath", description: "Exhaling power through the blade.", stats: { swordsmanship: 40, mysticArts: 20, strength: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Fire Slash", "Heat Haze"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Fire"], elementalWeaknesses: ["Water"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Titan's Grip", description: "Wielding massive weapons with ease.", stats: { strength: 50, endurance: 20, willpower: 10 }, skillMultiplier: 4.00, multipliedSkill: "Blacksmith", techniques: ["Colossus Strike", "Earth Breaker"], hugeBuffType: "Strength", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Lightning Reflex", description: "Reacting before the thought even forms.", stats: { agility: 50, perception: 20, luck: 10 }, skillMultiplier: 4.00, multipliedSkill: "Gunslinging", techniques: ["Flash Step", "Afterimage"], hugeBuffType: "Agility", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Lightning"], elementalWeaknesses: ["Earth"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Oracle's Whisper", description: "Hearing the future of the fight.", stats: { perception: 50, willpower: 20, agility: 10 }, skillMultiplier: 4.00, multipliedSkill: "Navigating", techniques: ["Prevision", "Mind Link"], hugeBuffType: "Perception", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Light"], elementalWeaknesses: ["Dark"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Void Anchor", description: "Grounding oneself in the fabric of reality.", stats: { willpower: 50, endurance: 20, mysticArts: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Nullify", "Gravity Field"], hugeBuffType: "Willpower", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Void"], elementalWeaknesses: ["Chaos"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Fortune's Favor", description: "Destiny smiles upon your every move.", stats: { luck: 60, agility: 10, perception: 10 }, skillMultiplier: 4.00, multipliedSkill: "TreasureHunting", techniques: ["Destiny Strike", "Jackpot"], hugeBuffType: "Luck", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Celestial"], elementalWeaknesses: ["Void"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Frost Bite", description: "Freezing the enemy's movements.", stats: { mysticArts: 40, agility: 20, endurance: 10 }, skillMultiplier: 4.00, multipliedSkill: "Cooking", techniques: ["Ice Prison", "Glacial Wall"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Ice"], elementalWeaknesses: ["Air"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Raging Torrent", description: "A relentless barrage of attacks.", stats: { swordsmanship: 45, strength: 15, agility: 10 }, skillMultiplier: 4.00, multipliedSkill: "Fishing", techniques: ["Flood", "Tidal Wave"], hugeBuffType: "Swordsmanship", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Water"], elementalWeaknesses: ["Lightning"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Shadow Weaver", description: "Manipulating shadows to bind foes.", stats: { mysticArts: 45, perception: 15, luck: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Dark Bind", "Nightmare"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Dark"], elementalWeaknesses: ["Light"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 },
        { name: "Celestial Alignment", description: "Drawing power from the stars.", stats: { willpower: 45, luck: 15, mysticArts: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Starfall", "Sunbeam"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], elements: ["Celestial"], elementalWeaknesses: ["Void"], debuffPercentage: 0.25, energyRegainMultiplier: 1.30 }
    ],
    "A": [
        { name: "Nebula Strike", description: "A cosmic strike that transcends dimensions.", stats: { swordsmanship: 80, mysticArts: 50, strength: 30 }, skillMultiplier: 10.00, multipliedSkill: "MysticArts", techniques: ["Cosmic Tear", "Black Hole", "Nova"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Celestial"], elementalWeaknesses: ["Void"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Atlas Burden", description: "Holding the weight of the heavens.", stats: { strength: 90, endurance: 50, willpower: 30 }, skillMultiplier: 10.00, multipliedSkill: "Blacksmith", techniques: ["Heavenly Smash", "Sky Cracker", "Final Pillar"], hugeBuffType: "Strength", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Chronos Step", description: "Moving through time for a brief moment.", stats: { agility: 90, perception: 50, luck: 30 }, skillMultiplier: 10.00, multipliedSkill: "Navigating", techniques: ["Time Warp", "Stutter", "Future Echo"], hugeBuffType: "Agility", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Void"], elementalWeaknesses: ["Chaos"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Spirit Reaper", description: "Striking at the very soul of the opponent.", stats: { mysticArts: 90, willpower: 50, perception: 30 }, skillMultiplier: 10.00, multipliedSkill: "Medical", techniques: ["Soul Rend", "Spirit Bind", "Essence Theft"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Dark"], elementalWeaknesses: ["Light"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Eternal Bastion", description: "An unbreakable defense that reflects damage.", stats: { endurance: 90, luck: 50, strength: 30 }, skillMultiplier: 10.00, multipliedSkill: "Blacksmith", techniques: ["Mirror Shield", "Fortress", "Aegis"], hugeBuffType: "Endurance", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "King's Authority", description: "Commanding the battlefield with presence.", stats: { willpower: 90, perception: 50, agility: 30 }, skillMultiplier: 10.00, multipliedSkill: "Navigating", techniques: ["Overawe", "Command", "Domination"], hugeBuffType: "Willpower", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Divine"], elementalWeaknesses: ["Chaos"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Nature's Wrath", description: "Harnessing the power of the natural world.", stats: { mysticArts: 80, strength: 40, endurance: 30 }, skillMultiplier: 10.00, multipliedSkill: "Fishing", techniques: ["Entangle", "Root Spike", "Thorn Hail"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Earth"], elementalWeaknesses: ["Air"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Silver Lining", description: "Finding victory in the direst situations.", stats: { luck: 100, agility: 40, perception: 30 }, skillMultiplier: 10.00, multipliedSkill: "TreasureHunting", techniques: ["Miracle", "Lucky Break", "Twist of Fate"], hugeBuffType: "Luck", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Celestial"], elementalWeaknesses: ["Void"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Solar Flare", description: "Blinding enemies with the brilliance of the sun.", stats: { mysticArts: 85, perception: 40, willpower: 30 }, skillMultiplier: 10.00, multipliedSkill: "Cooking", techniques: ["Sunburst", "Blinding Light", "Solar Storm"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Fire"], elementalWeaknesses: ["Water"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 },
        { name: "Abyssal Maw", description: "Consuming the light and hope of foes.", stats: { mysticArts: 85, willpower: 40, endurance: 30 }, skillMultiplier: 10.00, multipliedSkill: "MysticArts", techniques: ["Devour", "Void Pull", "Darkness Falls"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], elements: ["Dark"], elementalWeaknesses: ["Light"], debuffPercentage: 0.30, energyRegainMultiplier: 1.40 }
    ],
    "S": [
        { name: "Godspeed", description: "Surpassing the limits of human speed.", stats: { agility: 60, perception: 30 }, skillMultiplier: 25.00, multipliedSkill: "Sniper", techniques: ["Sonic Boom", "Infinite Afterimage"], hugeBuffType: "Agility", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, restrictedSkillTypes: ["Spear", "Sniper"], weakAgainst: ["Brawling"], elements: ["Void"], elementalWeaknesses: ["Chaos"] },
        { name: "World Sunderer", description: "A strike capable of splitting islands.", stats: { strength: 70, swordsmanship: 40 }, skillMultiplier: 25.00, multipliedSkill: "Swordsmanship", techniques: ["Great Divide", "Earth Quake"], hugeBuffType: "Strength", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, restrictedSkillTypes: ["Gunslinging", "MartialArts"], weakAgainst: ["Brawling"], elements: ["Physical"], elementalWeaknesses: ["Void"] },
        { name: "Maelstrom of Souls", description: "A vortex of spiritual energy.", stats: { mysticArts: 65, willpower: 45 }, skillMultiplier: 25.00, multipliedSkill: "MysticArts", techniques: ["Soul Suck", "Spirit Explosion"], hugeBuffType: "MysticArts", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, restrictedSkillTypes: ["Brawling", "Spear"], weakAgainst: ["Brawling"], elements: ["Void"], elementalWeaknesses: ["Chaos"] },
        { name: "Absolute Zero", description: "Freezing time and space itself.", stats: { mysticArts: 68, endurance: 52 }, skillMultiplier: 25.00, multipliedSkill: "Medical", techniques: ["Frozen Domain", "Shatter"], hugeBuffType: "MysticArts", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, restrictedSkillTypes: ["Swordsmanship", "Sniper"], weakAgainst: ["Brawling"], elements: ["Ice"], elementalWeaknesses: ["Air"] },
        { name: "Divine Providence", description: "Guided by the hand of fate.", stats: { luck: 100, willpower: 50 }, skillMultiplier: 25.00, multipliedSkill: "TreasureHunting", techniques: ["Fate's Seal", "Unstoppable Force"], hugeBuffType: "Luck", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, restrictedSkillTypes: ["Gunslinging", "Brawling"], weakAgainst: ["Brawling"], elements: ["Divine"], elementalWeaknesses: ["Chaos"] }
    ],
    "SS": [
        { name: "Chaos Theory", description: "Mastering the unpredictability of existence.", stats: { luck: 150, mysticArts: 100, perception: 50 }, skillMultiplier: 100.00, multipliedSkill: "MysticArts", techniques: ["Entropy", "Butterfly Effect", "Singularity"], hugeBuffType: "Luck", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, restrictedSkillTypes: ["Swordsmanship", "Spear", "Sniper", "MartialArts"], weakAgainst: ["MartialArts"], elements: ["Chaos"], elementalWeaknesses: ["Void"] },
        { name: "Elysium's Gate", description: "Opening the doors to a higher plane.", stats: { willpower: 150, endurance: 100, mysticArts: 50 }, skillMultiplier: 100.00, multipliedSkill: "Medical", techniques: ["Ascension", "Holy Rain", "Judgment"], hugeBuffType: "Willpower", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, restrictedSkillTypes: ["Brawling", "Gunslinging", "Spear", "Sniper"], weakAgainst: ["MartialArts"], elements: ["Celestial"], elementalWeaknesses: ["Void"] },
        { name: "Void Reaver", description: "Erasing anything the blade touches.", stats: { swordsmanship: 120, strength: 100, agility: 80 }, skillMultiplier: 100.00, multipliedSkill: "Swordsmanship", techniques: ["Erasure", "Non-Existence", "Dark Matter"], hugeBuffType: "Swordsmanship", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, restrictedSkillTypes: ["Brawling", "Gunslinging", "MysticArts", "MartialArts"], weakAgainst: ["MartialArts"], elements: ["Hybrid"], elementalWeaknesses: ["Celestial"] },
        { name: "Genesis", description: "The power of creation at your fingertips.", stats: { strength: 80, endurance: 80, agility: 80, perception: 80, willpower: 80, luck: 80, swordsmanship: 80, brawling: 80, gunslinging: 80, spear: 80, martialArts: 80, sniper: 80, mysticArts: 80 }, skillMultiplier: 100.00, multipliedSkill: "Cooking", techniques: ["Creation", "Renewal", "Alpha Strike"], hugeBuffType: "MysticArts", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, restrictedSkillTypes: ["Swordsmanship", "Gunslinging", "Spear", "Sniper"], weakAgainst: ["MartialArts"], elements: ["Genesis"], elementalWeaknesses: ["Void"] }
    ],
    "SSS": [
        { name: "Zenith", description: "The absolute pinnacle of martial prowess.", stats: { strength: 300, agility: 300, swordsmanship: 500, perception: 200 }, skillMultiplier: 500.00, multipliedSkill: "Swordsmanship", techniques: ["One Strike", "Universal Cut", "End of All"], hugeBuffType: "Swordsmanship", hugeBuffValue: 250.00, debuffPercentage: 0.0, energyRegainMultiplier: 2.0, restrictedSkillTypes: ["Brawling", "Gunslinging", "Spear", "MartialArts", "Sniper", "MysticArts"], elements: ["Hybrid", "Divine"], elementalWeaknesses: ["Chaos"] },
        { name: "Omegalyth", description: "The beginning and the end of all things.", stats: { mysticArts: 500, willpower: 400, luck: 300, endurance: 300 }, skillMultiplier: 500.00, multipliedSkill: "MysticArts", techniques: ["Erasure", "Rebirth", "Finality"], hugeBuffType: "MysticArts", hugeBuffValue: 250.00, debuffPercentage: 0.0, energyRegainMultiplier: 2.0, restrictedSkillTypes: ["Swordsmanship", "Brawling", "Gunslinging", "Spear", "MartialArts", "Sniper"], elements: ["Void", "Celestial"], elementalWeaknesses: ["Creation"] },
        { name: "The Author's Pen", description: "Rewriting the very laws of reality.", stats: { luck: 999, willpower: 999, perception: 999 }, skillMultiplier: 500.00, multipliedSkill: "TreasureHunting", techniques: ["Rewrite", "Delete", "Absolute Command"], hugeBuffType: "Willpower", hugeBuffValue: 250.00, debuffPercentage: 0.0, energyRegainMultiplier: 2.0, restrictedSkillTypes: ["Swordsmanship", "Brawling", "Gunslinging", "Spear", "MartialArts", "Sniper", "MysticArts"], elements: ["Creation"], elementalWeaknesses: ["Annihilation"] }
    ],
    "Z": [
        {
            name: "God's Eye of Annihilation",
            description: "The left eye of the void. A world-shattering power that seeks only to return everything to nothingness. Users can erase matter and souls alike, but are forbidden from mundane acts like cooking, healing, or professions. Travel is 2x slower due to the weight of your existence.",
            stats: { strength: 1000, endurance: 1000, agility: 1000, perception: 1000, willpower: 1000, luck: 1000, swordsmanship: 1000, brawling: 1000, gunslinging: 1000, spear: 1000, martialArts: 1000, sniper: 1000, mysticArts: 5000 },
            skillMultiplier: 5000.0, multipliedSkill: "MysticArts",
            techniques: ["Annihilation: Void Burst", "Annihilation: Reality Erasure", "Annihilation: Soul Grasp", "Annihilation: World's End", "Annihilation: Abyssal Gaze", "Annihilation: Dark Matter Crush", "Annihilation: Entropy Pulse", "Annihilation: Singularity Strike", "Annihilation: Oblivion Wave", "Annihilation: Shadow Reign", "Annihilation: Ruin", "Annihilation: Decay", "Annihilation: Despair", "Annihilation: Chaos Bolt", "Annihilation: Ultimate Zero"],
            hugeBuffType: "MysticArts", hugeBuffValue: 2500.0, debuffPercentage: 0.0, energyRegainMultiplier: 10.0,
            travelTimeMultiplier: 2.0, canLearnNonCombatSkills: false,
            restrictedSkillTypes: ["Cooking", "Navigating", "TreasureHunting", "Blacksmith", "Fishing", "Medical"],
            elements: ["Annihilation"], elementalWeaknesses: ["Creation"]
        },
        {
            name: "Celestial Eye of Creation",
            description: "The right eye of the origin. A world-building power that can manifest anything from thin air and manipulate the threads of destiny itself, but mundane acts like cooking, healing, or professions are beneath you. Travel is 2x slower due to the divine presence you carry.",
            stats: { strength: 1000, endurance: 1000, agility: 1000, perception: 1000, willpower: 1000, luck: 1000, swordsmanship: 1000, brawling: 1000, gunslinging: 1000, spear: 1000, martialArts: 1000, sniper: 1000, mysticArts: 5000 },
            skillMultiplier: 5000.0, multipliedSkill: "MysticArts",
            techniques: ["Creation: Genesis Flash", "Creation: Life Weaver", "Creation: Stellar Birth", "Creation: Infinite Bloom", "Creation: Holy Radiance", "Creation: Divine Structure", "Creation: Harmony Strike", "Creation: Eternal Dawn", "Creation: Cosmic Pulse", "Creation: Seraphim's Gaze", "Creation: Restoration", "Creation: Sanctity", "Creation: Purity", "Creation: Luminescence", "Creation: Omega Spark"],
            hugeBuffType: "MysticArts", hugeBuffValue: 2500.0, debuffPercentage: 0.0, energyRegainMultiplier: 10.0,
            travelTimeMultiplier: 2.0, canLearnNonCombatSkills: false,
            restrictedSkillTypes: ["Cooking", "Navigating", "TreasureHunting", "Blacksmith", "Fishing", "Medical"],
            elements: ["Creation"], elementalWeaknesses: ["Annihilation"]
        }
    ]
};

const STAT_MAPPING: Record<string, string> = {
    "Strength": "strength",
    "Endurance": "endurance",
    "Agility": "agility",
    "Perception": "perception",
    "Willpower": "willpower",
    "Luck": "luck",
    "Swordsmanship": "swordsmanship",
    "Brawling": "brawling",
    "Gunslinging": "gunslinging",
    "Spear": "spear",
    "MartialArts": "martialArts",
    "Sniper": "sniper",
    "MysticArts": "mysticArts",
    "Cooking": "cooking",
    "Navigating": "navigating",
    "TreasureHunting": "treasureHunting",
    "Blacksmith": "blacksmith",
    "Fishing": "fishing",
    "Medical": "medical"
};

const LOCATION_DATA: Record<string, { x: number, y: number, region: string }> = {
    "Fogi Tail Island": { x: 0, y: 0, region: "East Blue" },
    "Ironcrest Isle": { x: 640, y: 160, region: "East Blue" },
    "Amber Reach": { x: -320, y: 600, region: "East Blue" },
    "Sunken Reef": { x: 280, y: 360, region: "East Blue" },
    "Tortuga Bay": { x: 120, y: -840, region: "South Blue" },
    "Pirate\u0027s Den": { x: 1400, y: -1400, region: "South Blue" },
    "Navy Outpost Aqua": { x: -640, y: -440, region: "South Blue" },
    "Navy Outpost Terra": { x: -1200, y: 800, region: "Grand Line" },
    "Navy Outpost Ignis": { x: 1600, y: 1200, region: "Grand Line" },
    "Crystal Cove": { x: 1120, y: 480, region: "Grand Line" },
    "Volcano Peak": { x: 1680, y: 960, region: "Grand Line" },
    "Whispering Woods": { x: -600, y: 720, region: "Grand Line" },
    "Serpent\u0027s Maw": { x: 2000, y: 2000, region: "Grand Line" },
    "Kraken\u0027s Rest": { x: -1600, y: -1600, region: "South Blue" },
    "Shadow Fen": { x: -1200, y: -400, region: "East Blue" },
    "Island of World Secrets": { x: 4000, y: 4000, region: "Unknown" },
};

function calculateTravelTime(from: string, to: string, speedMultiplier: number = 1.0): number {
    const start = LOCATION_DATA[from] || { x: 0, y: 0, region: "Unknown" };
    const end = LOCATION_DATA[to] || { x: 0, y: 0, region: "Unknown" };

    const dist = Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
    let baseTime = dist * 300; // 1 unit = 0.3 seconds

    if (start.region !== end.region) {
        baseTime += 30000; // Extra 30 seconds for inter-region travel
    }

    const calculatedTime = Math.floor(baseTime / speedMultiplier);
    return Math.min(300000, Math.max(10000, calculatedTime)); // Cap at 5 mins, min 10s
}

// --- Helper Functions ---

function recordLog(transaction: admin.firestore.Transaction, userId: string, action: string, details: string, goldChange: number = 0, xpChange: number = 0) {
    const logRef = db.collection("logs").doc();
    transaction.set(logRef, {
        id: logRef.id,
        userId,
        action,
        details,
        goldChange,
        xpChange,
        timestamp: Date.now()
    });
}

function calculateMaxCapacity(character: any): number {
    let capacity = BASE_INVENTORY_CAPACITY;
    if (character.equipment && character.equipment.Bag) {
        capacity += (character.equipment.Bag.storageBonus || 0);
    }
    if (character.ship && character.ship.upgrades && character.ship.upgrades.storageLevel) {
        capacity += character.ship.upgrades.storageLevel * 5;
    }
    return capacity;
}

export function calculateCurrentEnergy(character: any): { energy: number, energyUpdatedAt: number } {
    const now = Date.now();
    const regenMultiplier = (character.mythicArt?.energyRegainMultiplier || 1.0);
    const regenRateMs = Math.max(1000, ENERGY_REGEN_RATE_MS / regenMultiplier);

    const elapsed = now - character.energyUpdatedAt;
    const regenerated = Math.floor(elapsed / regenRateMs);

    const currentMaxEnergy = character.maxEnergy ?? MAX_ENERGY;
    if (regenerated <= 0) return { energy: character.energy, energyUpdatedAt: character.energyUpdatedAt };

    const newEnergy = Math.min(currentMaxEnergy, character.energy + regenerated);
    const newTimestamp = (character.energy + regenerated >= currentMaxEnergy) ? now : character.energyUpdatedAt + Math.floor(regenerated * regenRateMs);

    return { energy: newEnergy, energyUpdatedAt: newTimestamp };
}

export function calculateCurrentMythicMana(character: any): { mythicMana: number, mythicManaUpdatedAt: number } {
    if (!character.mythicArt) return { mythicMana: 0, mythicManaUpdatedAt: character.mythicManaUpdatedAt || Date.now() };

    const now = Date.now();
    const regenMultiplier = (character.mythicArt?.energyRegainMultiplier || 1.0);
    const regenRateMs = Math.max(1000, MYTHIC_MANA_REGEN_RATE_MS / regenMultiplier);

    const elapsed = now - (character.mythicManaUpdatedAt || now);
    const regenerated = Math.floor(elapsed / regenRateMs);

    const currentMaxMana = character.maxMythicMana ?? 100;
    if (regenerated <= 0) return { mythicMana: character.mythicMana || 0, mythicManaUpdatedAt: character.mythicManaUpdatedAt || now };

    const newMana = Math.min(currentMaxMana, (character.mythicMana || 0) + regenerated);
    const newTimestamp = ((character.mythicMana || 0) + regenerated >= currentMaxMana) ? now : (character.mythicManaUpdatedAt || now) + Math.floor(regenerated * regenRateMs);

    return { mythicMana: newMana, mythicManaUpdatedAt: newTimestamp };
}

function assertCanPerformAction(character: any, actionName: string, options: { requireHp?: boolean, blockBusy?: boolean, blockHealing?: boolean } = {}) {
    if (character.isBanned) {
        throw new functions.https.HttpsError("permission-denied", "User is banned.");
    }

    if (options.requireHp !== false && character.hp <= 0) {
        throw new functions.https.HttpsError("failed-precondition", `You are too injured to ${actionName}. Visit an infirmary.`);
    }

    if (options.blockBusy) {
        if (character.travelState || character.trainingState || character.combatState || character.fishingState) {
             throw new functions.https.HttpsError("failed-precondition", `You are too busy to ${actionName}.`);
        }
    }

    if (options.blockHealing && character.healingState) {
        throw new functions.https.HttpsError("failed-precondition", `You cannot ${actionName} while resting in the infirmary.`);
    }
}

function executeCrewJoin(transaction: admin.firestore.Transaction, userId: string, playerRef: admin.firestore.DocumentReference, playerSnap: admin.firestore.DocumentSnapshot, crewRef: admin.firestore.DocumentReference, crewSnap: admin.firestore.DocumentSnapshot, inviteRef: admin.firestore.DocumentReference, crewId: string) {
    if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
    if (!crewSnap.exists) throw new functions.https.HttpsError("not-found", "Crew not found.");

    let character = playerSnap.data() as any;
    character = processCharacterUpdates(character);
    assertCanPerformAction(character, "join a crew", { blockBusy: true });

    if (character.crewId) throw new functions.https.HttpsError("already-exists", "Player is already in a crew.");

    const crew = crewSnap.data() as any;
    const members = crew.members || [];
    if (members.length >= 20) throw new functions.https.HttpsError("resource-exhausted", "Crew is full.");

    transaction.update(crewRef, {
        members: admin.firestore.FieldValue.arrayUnion(userId),
        totalBounty: admin.firestore.FieldValue.increment(character.bounty || 0)
    });
    transaction.update(playerRef, {
        hp: character.hp,
        energy: character.energy,
        energyUpdatedAt: character.energyUpdatedAt,
        healingState: character.healingState,
        crewId: crewId
    });
    transaction.update(inviteRef, { status: "accepted" });
}

function mergeStatusEffects(existingEffects: any[], newEffects: any[]): any[] {
    const effectsMap = new Map<string, any>();
    for (const effect of (existingEffects || [])) {
        effectsMap.set(effect.type, { ...effect });
    }
    for (const effect of (newEffects || [])) {
        // Issue 27-29: Validate and Clamp Effect Data
        if (!effect.type || typeof effect.type !== "string") continue;
        const duration = (Number.isInteger(effect.duration) && effect.duration > 0) ? Math.min(effect.duration, 10) : 0;
        if (duration <= 0) continue;

        // Issue 28: Magnitude Bounds
        const magnitude = (Number.isFinite(effect.magnitude)) ? Math.max(-100, Math.min(effect.magnitude, 100)) : 5;

        if (effectsMap.has(effect.type)) {
            const existing = effectsMap.get(effect.type);
            existing.duration = Math.max(existing.duration, duration);
            existing.magnitude = Math.max(existing.magnitude || 0, magnitude);
            if (existing.magnitude > 100) existing.magnitude = 100;
            if (existing.magnitude < -100) existing.magnitude = -100;
        } else {
            effectsMap.set(effect.type, { ...effect, duration, magnitude });
        }
    }

    // Issue 30: Array Size Protection
    const result = Array.from(effectsMap.values());
    return result.slice(0, 10);
}

function checkLevelUp(character: any) {
    let { level, xp, stats, maxEnergy, energy, maxHp, hp } = character;
    const MAX_LEVEL = 300;

    if (level >= MAX_LEVEL) {
        // Keep XP but don't level up
        return { ...character, level: MAX_LEVEL, leveledUp: false };
    }

    let xpNeeded = level * level * 100;

    let leveledUp = false;
    while (xp >= xpNeeded && level < MAX_LEVEL) {
        level++;
        xp -= xpNeeded;

        maxHp += 20;
        hp = maxHp;

        if (level % 5 === 0) {
            maxEnergy += 5;
            energy = maxEnergy;
        }

        stats.endurance += 1;
        stats.strength += 1;
        stats.agility += 1;
        stats.perception += 1;
        stats.willpower += 1;
        stats.luck += 1;

        if (level < MAX_LEVEL) {
            xpNeeded = level * level * 100;
        }
        leveledUp = true;
    }

    return { ...character, level, xp, stats, maxEnergy, energy, maxHp, hp, leveledUp };
}

function getArtifactDetails(tier: string): { name: string, description: string } {
    const details: Record<string, { name: string, description: string }> = {
        "F": { name: "Shattered Slate [F]", description: "A common artifact containing a faint whisper of power." },
        "E": { name: "Bones of Old [E]", description: "A weathered relic that holds basic knowledge from a bygone age." },
        "D": { name: "Ancient Shard [D]", description: "A shard from a long-lost civilization, pulsating with faint energy." },
        "C": { name: "Glowing Core [C]", description: "A core of pure energy that contains specialized techniques." },
        "B": { name: "Jade Idol [B]", description: "A beautifully crafted idol that resonates with your spirit." },
        "A": { name: "Dragon Scale [A]", description: "A scale from a legendary dragon, containing immense power." },
        "S": { name: "Phoenix Feather [S]", description: "A feather that never stops burning with mythical energy." },
        "SS": { name: "Tear of a God [SS]", description: "A crystalline tear said to fall from the heavens." },
        "SSS": { name: "Void Essence [SSS]", description: "The pure essence of the void. The absolute pinnacle of power." },
        "Z": { name: "Primordial Spark [Z]", description: "A fragment of the original creation, blindingly and terrifyingly powerful." }
    };
    return details[tier] || { name: `${tier} Tier Artifact`, description: `A mysterious ${tier} tier artifact.` };
}

export const rollMythicArt = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        const now = Date.now();
        if (character.lastRollAt && (now - character.lastRollAt < ROLL_COOLDOWN_MS)) {
            throw new functions.https.HttpsError("failed-precondition", "Slow down! You are rolling too fast.");
        }

        assertCanPerformAction(character, "roll for Mythic Arts", { blockBusy: true, blockHealing: true });

        // Location check
        if (character.currentLocation !== "Island of World Secrets") {
            throw new functions.https.HttpsError("failed-precondition", "You must be at the Island of World Secrets to roll for Mythic Arts.");
        }

        // Cost check
        let freeRolls = character.freeMythicRolls;
        if (freeRolls === undefined || freeRolls === null) {
            freeRolls = 3;
        }

        let goldCost = 0;
        if (freeRolls <= 0) {
            goldCost = MYTHIC_ROLL_GOLD_COST;
            if ((character.gold || 0) < goldCost) {
                throw new functions.https.HttpsError("failed-precondition", `Not enough gold (1,000,000 required). Current: ${character.gold || 0}`);
            }
        }

        // Inventory check
        const inventory = character.inventory || [];
        const maxCapacity = calculateMaxCapacity(character);

        if (inventory.length >= maxCapacity) {
            throw new functions.https.HttpsError(
                "failed-precondition",
                "Inventory is full."
            );
        }

        const rand = Math.random() * 100;

        let tier = "F";

        if (rand < 0.000001) tier = "Z";
        else if (rand < 0.000101) tier = "SSS";
        else if (rand < 0.001101) tier = "SS";
        else if (rand < 0.011101) tier = "S";
        else if (rand < 0.111101) tier = "A";
        else if (rand < 1.111101) tier = "B";
        else if (rand < 6.111101) tier = "C";
        else if (rand < 26.111101) tier = "D";

        const artifactDetails = getArtifactDetails(tier);

        const artifactItem = {
            id: `mythic_artifact_${tier}_${Date.now()}`,
            name: artifactDetails.name,
            description: artifactDetails.description,
            type: "Artifact",
            rarity: getRarityForTier(tier),
            price: getPriceForTier(tier),
            mythicTier: tier,
            levelRequirement: 1
        };

        const updates: any = {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            inventory: [...inventory, artifactItem],
            lastRollAt: now
        };

        if (freeRolls > 0) {
            updates.freeMythicRolls = freeRolls - 1;
        } else {
            updates.gold = admin.firestore.FieldValue.increment(-goldCost);
        }

        transaction.update(playerRef, updates);

        recordLog(
            transaction,
            userId,
            "RollMythicArt",
            `Rolled ${tier} tier artifact`,
            -goldCost,
            0
        );

        return {
            success: true,
            tier
        };
    });
});

function getRarityForTier(tier: string): string {
    switch (tier) {
        case "Z": return "Legendary";
        case "SSS": return "Legendary";
        case "SS": return "Legendary";
        case "S": return "Epic";
        case "A": return "Epic";
        case "B": return "Rare";
        case "C": return "Uncommon";
        default: return "Common";
    }
}

function getPriceForTier(tier: string): number {
    switch (tier) {
        case "Z": return 50000000;
        case "SSS": return 10000000;
        case "SS": return 5000000;
        case "S": return 2000000;
        case "A": return 1000000;
        case "B": return 500000;
        case "C": return 100000;
        case "D": return 50000;
        default: return 10000;
    }
}

function processCharacterUpdates(character: any): any {
    let updated = { ...character };

    // 1. Process Healing
    if (updated.healingState && updated.healingState.endTime <= Date.now()) {
        updated.hp = updated.maxHp;
        updated.healingState = null;
    }

    // 2. Process Energy Regeneration
    const energyResult = calculateCurrentEnergy(updated);
    updated.energy = energyResult.energy;
    updated.energyUpdatedAt = energyResult.energyUpdatedAt;

    // 3. Process Mythic Mana Regeneration
    if (updated.mythicArt) {
        // Migration: element -> elements
        if (updated.mythicArt.element && (!updated.mythicArt.elements || updated.mythicArt.elements.length === 0)) {
            updated.mythicArt.elements = [updated.mythicArt.element];
            delete updated.mythicArt.element;
        }

        // Repair: If elements is still missing/empty, sync from Registry by name
        if (!updated.mythicArt.elements || updated.mythicArt.elements.length === 0) {
            const artName = updated.mythicArt.name;
            const tier = updated.mythicArt.tier;
            const artsInTier = MYTHIC_ARTS[tier] || [];
            const registryArt = artsInTier.find((a: any) => a.name === artName);
            if (registryArt && registryArt.elements) {
                updated.mythicArt.elements = registryArt.elements;
                if (registryArt.elementalWeaknesses) {
                    updated.mythicArt.elementalWeaknesses = registryArt.elementalWeaknesses;
                }
            }
        }

        const manaResult = calculateCurrentMythicMana(updated);
        updated.mythicMana = manaResult.mythicMana;
        updated.mythicManaUpdatedAt = manaResult.mythicManaUpdatedAt;
    }

    return updated;
}

// --- Player Management ---

export const createCharacter = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { name, gender, race } = data;
    const userId = context.auth.uid;

    // Name Validation
    const trimmedName = (name || "").trim();
    if (trimmedName.length < 3 || trimmedName.length > 16) {
        throw new functions.https.HttpsError("invalid-argument", "Name must be between 3 and 16 characters.");
    }
    if (!/^[a-zA-Z0-9_]+$/.test(trimmedName)) {
        throw new functions.https.HttpsError("invalid-argument", "Name contains invalid characters.");
    }

    const reservedNames = ["admin", "system", "moderator", "game-master", "gm", "sedna", "von"];
    if (reservedNames.includes(trimmedName.toLowerCase())) {
        throw new functions.https.HttpsError("invalid-argument", "This name is reserved.");
    }

    // Race/Gender Validation
    const allowedRaces = ["Human", "Abyssal", "Beastkin", "Celestian", "Automaton"];
    const allowedGenders = ["Male", "Female", "Other"];
    if (!allowedRaces.includes(race)) throw new functions.https.HttpsError("invalid-argument", "Invalid race.");
    if (!allowedGenders.includes(gender)) throw new functions.https.HttpsError("invalid-argument", "Invalid gender.");

    const nameLower = trimmedName.toLowerCase();
    const playerRef = db.collection("players").doc(userId);
    const nameRef = db.collection("characterNames").doc(nameLower);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, nameSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(nameRef)
        ]);

        if (playerSnap.exists) {
            throw new functions.https.HttpsError("already-exists", "You already have a character.");
        }

        if (nameSnap.exists) {
            throw new functions.https.HttpsError("already-exists", "Character name is already taken.");
        }

        const baseStats = {
            strength: 5,
            endurance: 5,
            agility: 5,
            perception: 5,
            willpower: 5,
            luck: 5,
            swordsmanship: 0,
            brawling: 0,
            gunslinging: 0,
            spear: 0,
            martialArts: 0,
            sniper: 0,
            mysticArts: 0
        };

        // Apply Race Boosts
        if (race === "Human") {
            baseStats.luck += 2;
            baseStats.strength += 1;
            baseStats.endurance += 1;
            baseStats.agility += 1;
        } else if (race === "Abyssal") {
            baseStats.endurance += 3;
            baseStats.willpower += 2;
        } else if (race === "Beastkin") {
            baseStats.agility += 3;
            baseStats.perception += 2;
        } else if (race === "Celestian") {
            baseStats.willpower += 3;
            baseStats.perception += 2;
        } else if (race === "Automaton") {
            baseStats.strength += 3;
            baseStats.endurance += 2;
        }

        const character = {
            id: userId,
            name: trimmedName,
            nameLower: nameLower,
            gender: gender,
            race: race,
            level: 1,
            xp: 0,
            gold: 100,
            bounty: 0,
            infamy: 0,
            hp: 100,
            maxHp: 100,
            energy: 100,
            maxEnergy: 100,
            energyUpdatedAt: Date.now(),
            lastOnline: Date.now(),
            isOnline: true,
            isAdmin: false,
            isBanned: false,
            currentLocation: "Fogi Tail Island",
            freeMythicRolls: 3,
            mythicArt: null,
            rank: "Novice Sailor",
            title: "",
            unlockedTitles: [],
            pvpWins: 0,
            pvpLosses: 0,
            faction: "Neutral",
            stats: baseStats,
            professionStats: {
                cooking: 0,
                navigating: 0,
                treasureHunting: 0,
                blacksmith: 0,
                fishing: 0,
                medical: 0
            },
            inventory: [],
            inventoryCapacity: 20,
            equipment: {},
            travelState: null,
            combatState: null,
            learnedTechniques: ["bash"],
            healingState: null,
            ship: { id: "row_boat", name: "Row Boat", price: 0, speedMultiplier: 1.0 }
        };

        transaction.set(nameRef, { userId });
        transaction.set(playerRef, character);
        return { success: true };
    });
});

export const joinFaction = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { faction } = data; // Navy, Pirate
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        assertCanPerformAction(character, "join a faction", { blockBusy: true, blockHealing: true });

        if (character.faction !== "Neutral") {
            throw new functions.https.HttpsError("failed-precondition", "You are already in a faction.");
        }

        if (faction === "Pirate") {
            if (character.currentLocation !== "Pirate\u0027s Den") {
                throw new functions.https.HttpsError("failed-precondition", "You must be at the Pirate\u0027s Den to join the Pirates.");
            }
            transaction.update(playerRef, { faction: "Pirate", rank: "Rogue" });
            recordLog(transaction, userId, "JoinFaction", "Became a Pirate", 0, 0);
        } else if (faction === "Navy") {
            if (character.currentLocation !== "Navy Outpost Aqua") {
                throw new functions.https.HttpsError("failed-precondition", "You must be at the Navy Outpost Aqua to enlist in the Navy.");
            }
            transaction.update(playerRef, { faction: "Navy", rank: "Navy Cadet" });
            recordLog(transaction, userId, "JoinFaction", "Enlisted in the Navy", 0, 0);
        } else {
            throw new functions.https.HttpsError("invalid-argument", "Invalid faction choice.");
        }

        return { success: true, faction };
    });
});

export const train = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { statType } = data;
    // Stronger mapping validation to prevent incorrect fields like 'martialarts'
    const mappedStat = STAT_MAPPING[statType];
    if (!mappedStat) {
        throw new functions.https.HttpsError("invalid-argument", `Invalid stat type: ${statType}. Allowed: ${Object.keys(STAT_MAPPING).join(", ")}`);
    }

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "train", { blockBusy: true, blockHealing: true });

        // Issue 14: Location Validation for Training
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        if (!locationSnap.exists) throw new functions.https.HttpsError("not-found", "Current location not found.");
        const location = locationSnap.data();
        if (!location?.actions?.some((a: any) => a.type === "Training")) {
            throw new functions.https.HttpsError("failed-precondition", "You cannot train at your current location.");
        }

        if (statType === "Medical") {
            throw new functions.https.HttpsError("failed-precondition", "Medical skill can only be trained by healing patients in an infirmary.");
        }

        if (character.mythicArt && character.mythicArt.restrictedSkillTypes) {
            if (character.mythicArt.restrictedSkillTypes.includes(statType)) {
                throw new functions.https.HttpsError("failed-precondition", `Your Mythic Art restricts training ${statType}.`);
            }
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);

        if (energy < 10) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        // Calculate Cost based on current BASE stat (excluding mythic art)
        const stats = character.stats || {};
        const pStats = character.professionStats || {};
        const mythicStats = character.mythicArt?.bonusStats || {};

        const currentTotalValue = stats[mappedStat] ?? pStats[mappedStat] ?? 0;
        const mythicBonus = mythicStats[mappedStat] ?? 0;
        const baseStatForCost = Math.max(0, currentTotalValue - mythicBonus);

        const trainingGoldCost = TRAINING_BASE_GOLD_COST + Math.floor(baseStatForCost * 10);

        if (character.gold < trainingGoldCost) {
            throw new functions.https.HttpsError("failed-precondition", `Not enough gold (${trainingGoldCost} required).`);
        }

        const endTime = Date.now() + TRAINING_DURATION_MS;

        transaction.update(playerRef, {
            hp: character.hp,
            healingState: character.healingState,
            energy: energy - 10,
            energyUpdatedAt,
            gold: admin.firestore.FieldValue.increment(-trainingGoldCost),
            trainingState: { endTime, statType }
        });

        recordLog(transaction, userId, "TrainStart", `Started training ${statType}`, -trainingGoldCost, 0);

        return { success: true, endTime };
    });
});

export const finishTraining = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        const training = character.trainingState;
        if (!training) throw new functions.https.HttpsError("failed-precondition", "No active training session.");
        if (training.endTime > Date.now()) throw new functions.https.HttpsError("failed-precondition", "Training not yet complete.");

        const statType = training.statType;
        const mappedStat = STAT_MAPPING[statType];

        if (!mappedStat) {
            throw new functions.https.HttpsError("internal", `Corrupted training state: Invalid statType ${statType}`);
        }

        // Ensure stats object exists
        const stats = { ...character.stats };
        const pStats = { ...(character.professionStats || {}) };

        if (stats[mappedStat] !== undefined) {
            stats[mappedStat] = (stats[mappedStat] || 0) + 0.1;
        } else if (pStats[mappedStat] !== undefined) {
            pStats[mappedStat] = (pStats[mappedStat] || 0) + 0.1;
        } else {
            // Fallback for new stats
            const combatStats = ["swordsmanship", "brawling", "gunslinging", "spear", "martialArts", "sniper", "mysticArts"];
            if (combatStats.includes(mappedStat)) {
                stats[mappedStat] = (stats[mappedStat] || 0) + 0.1;
            } else {
                pStats[mappedStat] = (pStats[mappedStat] || 0) + 0.1;
            }
        }

        const updatedChar = { ...character, xp: character.xp + 5, stats, professionStats: pStats, trainingState: null };
        const finalChar = checkLevelUp(updatedChar);

        transaction.update(playerRef, finalChar);
        recordLog(transaction, userId, "TrainFinish", `Finished training ${statType}`, 0, 5);

        return { success: true };
    });
});

// --- Gameplay Mechanics ---

export const startTravel = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { destination } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "travel", { blockBusy: true, blockHealing: true });

        // Issue 14: Current location validation
        if (!LOCATION_DATA[character.currentLocation]) {
             throw new functions.https.HttpsError("failed-precondition", "Your current location is invalid.");
        }

        if (character.currentLocation === destination) {
            throw new functions.https.HttpsError("invalid-argument", "Already at destination.");
        }

        if (!LOCATION_DATA[destination]) {
            throw new functions.https.HttpsError("invalid-argument", "Invalid destination.");
        }

        const speedMultiplier = character.ship?.speedMultiplier || 1.0;
        let travelMultiplier = 1.0;
        if (character.mythicArt && character.mythicArt.travelTimeMultiplier) {
            travelMultiplier = character.mythicArt.travelTimeMultiplier;
        }

        // --- Faction Passives (Travel) ---
        // Navy: Faster travel between Outposts
        const isCurrentNavy = character.currentLocation.startsWith("Navy Outpost");
        const isDestNavy = destination.startsWith("Navy Outpost");
        if (character.faction === "Navy" && isCurrentNavy && isDestNavy) {
            travelMultiplier *= 1.5; // 50% speed boost
        }

        let travelDuration = calculateTravelTime(character.currentLocation, destination, speedMultiplier * (1.0 / travelMultiplier));
        let arrivalTime = Date.now() + travelDuration;
        let eventMessage: string | null = null;

        // Random Sea Events (35% chance)
        if (travelDuration > 10000 && Math.random() < 0.35) {
            const eventRoll = Math.random();

            if (eventRoll < 0.50) {
                // 1. Ambush (Combat)
                const enemy = generateEnemy(character.level);
                transaction.update(playerRef, {
                    hp: character.hp,
                    healingState: character.healingState,
                    combatState: {
                        enemy: enemy,
                        playerTurn: true,
                        logs: [`While sailing to ${destination}, you encountered a ${enemy.name}!`],
                        isFinished: false,
                        playerWon: false,
                        turnCount: 0,
                        intendedDestination: destination,
                        intendedArrivalTime: arrivalTime,
                        intendedStartTime: Date.now()
                    }
                });
                return { ambush: true, enemy };
            } else if (eventRoll < 0.65) {
                // 2. Tailwinds
                travelDuration = Math.floor(travelDuration * 0.75);
                arrivalTime = Date.now() + travelDuration;
                eventMessage = "You caught a strong tailwind! Voyage time reduced.";
            } else if (eventRoll < 0.75) {
                // 3. Sudden Storm
                const damage = Math.floor(character.maxHp * 0.15);
                character.hp = Math.max(1, character.hp - damage); // Don't die to storm, just low HP
                travelDuration += 30000; // 30s delay
                arrivalTime = Date.now() + travelDuration;
                eventMessage = "A sudden storm battered your ship! You took damage and were slowed down.";
            } else if (eventRoll < 0.90) {
                // 4. Floating Supplies
                let goldGain = 500;
                if (character.faction === "Pirate") goldGain = Math.floor(goldGain * 1.15);
                character.gold = (character.gold || 0) + goldGain;
                character.energy = Math.min(character.maxEnergy, character.energy + 15);
                eventMessage = `You found a crate of floating supplies! +${goldGain} Gold and +15 Energy.`;
            } else if (eventRoll < 0.95) {
                // 5. Ancient Driftwood
                let goldGain = 1000;
                if (character.faction === "Pirate") goldGain = Math.floor(goldGain * 1.15);
                character.gold = (character.gold || 0) + goldGain;
                eventMessage = `You salvaged something valuable from ancient driftwood! (Found hidden treasures worth ${goldGain} Gold)`;
            } else {
                // 6. Mermaid's Song
                character.energy = character.maxEnergy;
                eventMessage = "The hauntingly beautiful song of a mermaid has restored your energy.";
            }
        }

        transaction.update(playerRef, {
            hp: character.hp,
            gold: character.gold,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            travelState: { destination, arrivalTime, startTime: Date.now(), eventMessage }
        });
        return { success: true, arrivalTime, travelDuration, eventMessage };
    });
});

export const finishTravel = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        const travel = character.travelState;

        if (!travel || travel.arrivalTime > Date.now()) {
            throw new functions.https.HttpsError("failed-precondition", "Travel not yet complete.");
        }

        transaction.update(playerRef, {
            currentLocation: travel.destination,
            travelState: null
        });
        return { success: true, newLocation: travel.destination };
    });
});

export const finishHealing = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        if (!character.healingState) throw new functions.https.HttpsError("failed-precondition", "No active healing state.");
        if (character.healingState.endTime > Date.now()) {
            throw new functions.https.HttpsError("failed-precondition", "Healing not yet complete.");
        }

        transaction.update(playerRef, {
            hp: character.maxHp,
            healingState: null
        });
        return { success: true };
    });
});

export const purchaseShip = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { shipId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    const SHIPS: Record<string, { name: string, price: number, speedMultiplier: number }> = {
        "row_boat": { name: "Row Boat", price: 0, speedMultiplier: 1.0 },
        "sloop": { name: "Sloop", price: 500, speedMultiplier: 1.5 },
        "caravel": { name: "Caravel", price: 2500, speedMultiplier: 2.0 },
        "galleon": { name: "Galleon", price: 10000, speedMultiplier: 3.0 }
    };

    const ship = SHIPS[shipId];
    if (!ship) throw new functions.https.HttpsError("invalid-argument", "Invalid ship ID.");

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "purchase a ship", { blockBusy: true, blockHealing: true });

        // Validate location has a shipyard
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const hasShipyard = location?.actions?.some((a: any) => a.type === "Shipyard");

        if (!hasShipyard) {
            throw new functions.https.HttpsError("failed-precondition", "There is no shipyard at your current location.");
        }

        if (character.gold < ship.price) throw new functions.https.HttpsError("failed-precondition", "Not enough gold.");
        if (character.ship?.id === shipId) throw new functions.https.HttpsError("failed-precondition", "You already own this ship.");

        transaction.update(playerRef, {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            gold: character.gold - ship.price,
            ship: { id: shipId, ...ship }
        });

        recordLog(transaction, userId, "PurchaseShip", `Purchased ${ship.name}`, -ship.price, 0);
        return { success: true };
    });
});

// --- Combat Engine ---

interface CombatStats {
    hp: number;
    maxHp: number;
    strength: number;
    endurance: number;
    agility: number;
    perception: number;
    willpower: number;
    luck: number;
    swordsmanship?: number;
    brawling?: number;
    gunslinging?: number;
    spear?: number;
    martialArts?: number;
    sniper?: number;
    mysticArts?: number;
    defense: number;
    accuracy: number;
    dodge: number;
    critChance: number;
}

function calculateCombatStats(charOrEnemy: any, currentEffects: any[] = []): CombatStats {
    const stats = { ...(charOrEnemy.stats || {}) };
    const level = charOrEnemy.level || 1;

    // Apply equipment bonuses
    if (charOrEnemy.equipment) {
        for (const slot in charOrEnemy.equipment) {
            const item = charOrEnemy.equipment[slot];
            if (item && item.statBonus) {
                for (const stat in item.statBonus) {
                    if (stats[stat] !== undefined) {
                        stats[stat] += item.statBonus[stat];
                    }
                }
            }
        }
    }

    // Base stats
    let strength = stats.strength || 5;
    let endurance = stats.endurance || 5;
    let agility = stats.agility || 5;
    let perception = stats.perception || 5;
    let luck = stats.luck || 5;
    let willpower = stats.willpower || 5;

    // Apply Mythic Art bonuses
    const mythicArt = charOrEnemy.mythicArt;
    if (mythicArt) {
        const skillName = mythicArt.multipliedSkill;
        const skillMultiplier = mythicArt.skillMultiplier || 1.0;
        const mappedSkill = STAT_MAPPING[skillName];
        if (mappedSkill && stats[mappedSkill] !== undefined) {
            stats[mappedSkill] = Math.floor(stats[mappedSkill] * skillMultiplier);
        }

        const debuff = mythicArt.debuffPercentage || 0;
        if (debuff > 0) {
            const globalMultiplier = 1.0 - debuff;
            strength = Math.floor(strength * globalMultiplier);
            endurance = Math.floor(endurance * globalMultiplier);
            agility = Math.floor(agility * globalMultiplier);
            perception = Math.floor(perception * globalMultiplier);
            willpower = Math.floor(willpower * globalMultiplier);
            luck = Math.floor(luck * globalMultiplier);
        }

        const hugeBuffType = mythicArt.hugeBuffType;
        const hugeBuffValue = mythicArt.hugeBuffValue || 0;
        if (hugeBuffType && hugeBuffValue > 0) {
            const hugeMappedSkill = STAT_MAPPING[hugeBuffType];
            if (hugeMappedSkill && stats[hugeMappedSkill] !== undefined) {
                stats[hugeMappedSkill] = Math.floor(stats[hugeMappedSkill] * (1.0 + hugeBuffValue));
            } else {
                const baseStatMulti = 1.0 + hugeBuffValue;
                if (hugeBuffType === "Strength") strength = Math.floor(strength * baseStatMulti);
                if (hugeBuffType === "Endurance") endurance = Math.floor(endurance * baseStatMulti);
                if (hugeBuffType === "Agility") agility = Math.floor(agility * baseStatMulti);
                if (hugeBuffType === "Perception") perception = Math.floor(perception * baseStatMulti);
                if (hugeBuffType === "Willpower") willpower = Math.floor(willpower * baseStatMulti);
                if (hugeBuffType === "Luck") luck = Math.floor(luck * baseStatMulti);
            }
        }
    }

    // Derived stats
    let defense = Math.floor(endurance * 1.5 + level);
    let accuracy = 80 + agility * 0.5 + perception * 0.5;
    let dodge = agility * 0.8 + luck * 0.2;
    let critChance = 5 + luck * 0.5 + perception * 0.2;

    // Apply effects
    if (currentEffects.some(e => e.type === "Haste")) {
        dodge += 10;
        accuracy += 10;
    }

    return {
        hp: charOrEnemy.hp,
        maxHp: charOrEnemy.maxHp,
        strength,
        endurance,
        agility,
        perception,
        willpower,
        luck,
        swordsmanship: stats.swordsmanship || 0,
        brawling: stats.brawling || 0,
        gunslinging: stats.gunslinging || 0,
        spear: stats.spear || 0,
        martialArts: stats.martialArts || 0,
        sniper: stats.sniper || 0,
        mysticArts: stats.mysticArts || 0,
        defense,
        accuracy,
        dodge,
        critChance
    };
}

function generateEnemy(playerLevel: number) {
    const seaMobs = [
        { name: "Sea Serpent", minLevel: 1, maxLevel: 15, dropTableId: "basic_sea_loot" },
        { name: "Pirate Scout", minLevel: 3, maxLevel: 20, dropTableId: "basic_sea_loot" },
        { name: "Giant Squid", minLevel: 10, maxLevel: 30, dropTableId: "rare_sea_loot" },
        { name: "Ghost Pirate", minLevel: 15, maxLevel: 45, dropTableId: "rare_sea_loot" },
        { name: "Feral Crab", minLevel: 1, maxLevel: 8, dropTableId: "basic_sea_loot" },
        { name: "Rogue Sloop", minLevel: 5, maxLevel: 25, dropTableId: "basic_sea_loot" },
        { name: "Navy Enforcer", minLevel: 8, maxLevel: 35, dropTableId: "rare_sea_loot" },
        { name: "Sunken Golem", minLevel: 20, maxLevel: 50, dropTableId: "rare_sea_loot" },
        { name: "Abyssal Horror", minLevel: 50, maxLevel: 150, dropTableId: "mythic_sea_loot" },
        { name: "Shadow Leviathan", minLevel: 60, maxLevel: 200, dropTableId: "mythic_sea_loot" },
        { name: "Elite Navy Hunter", minLevel: 40, maxLevel: 100, dropTableId: "rare_sea_loot" },
        { name: "Pirate Warlord", minLevel: 70, maxLevel: 250, dropTableId: "mythic_sea_loot" },
        { name: "Siren", minLevel: 25, maxLevel: 60, dropTableId: "rare_sea_loot" },
        { name: "Storm Roc", minLevel: 30, maxLevel: 80, dropTableId: "rare_sea_loot" },
        { name: "Deep Sea Angler", minLevel: 35, maxLevel: 75, dropTableId: "rare_sea_loot" },
        { name: "Mutated Shark", minLevel: 12, maxLevel: 40, dropTableId: "basic_sea_loot" },
        { name: "Cursed Skeleton", minLevel: 18, maxLevel: 55, dropTableId: "rare_sea_loot" },
        { name: "Navy Dreadnought", minLevel: 80, maxLevel: 300, dropTableId: "mythic_sea_loot" }
    ];

    // Filter mobs by player level
    const possibleMobs = seaMobs.filter(m => playerLevel >= m.minLevel - 2); // Allow slightly higher level mobs
    const mobDef = possibleMobs.length > 0
        ? possibleMobs[Math.floor(Math.random() * possibleMobs.length)]
        : seaMobs[0];

    const level = Math.max(1, Math.min(mobDef.maxLevel, playerLevel + Math.floor(Math.random() * 3) - 1));

    const stats = {
        strength: 5 + level * 2,
        endurance: 5 + level * 2,
        agility: 5 + level,
        perception: 5 + level,
        willpower: 5 + level,
        luck: 5 + level / 2
    };

    const maxHp = 40 + (level * 15);

    return {
        name: mobDef.name,
        level,
        hp: maxHp,
        maxHp: maxHp,
        stats,
        goldReward: 20 * level,
        xpReward: 15 * level,
        dropTableId: mobDef.dropTableId
    };
}

async function processLoot(dropTableId: string): Promise<any[]> {
    if (!dropTableId) return [];

    const lootTableSnap = await db.collection("gameData").doc("world").collection("lootTables").doc(dropTableId).get();
    if (!lootTableSnap.exists) return [];

    const lootTable = lootTableSnap.data();
    const droppedItems: any[] = [];

    for (const entry of (lootTable?.entries || [])) {
        if (Math.random() <= entry.chance) {
            const itemSnap = await db.collection("gameData").doc("items").collection("all").doc(entry.itemId).get();
            if (itemSnap.exists) {
                const item = itemSnap.data();
                if (item) {
                    const amount = Math.floor(Math.random() * ((entry.maxAmount || 1) - (entry.minAmount || 1) + 1)) + (entry.minAmount || 1);
                    for (let i = 0; i < amount; i++) {
                        droppedItems.push({ ...item, id: `${item.id}_${Date.now()}_${Math.floor(Math.random() * 10000)}` });
                    }
                }
            }
        }
    }

    return droppedItems;
}

function processStatusEffects(character: any, effects: any[], logs: string[]): { character: any, activeEffects: any[] } {
    let updatedChar = { ...character };
    const activeEffects: any[] = [];

    for (const effect of effects) {
        if (effect.duration <= 0) continue;

        switch (effect.type) {
            case "Bleed":
            case "Burn":
                const damage = effect.magnitude || 5;
                updatedChar.hp = Math.max(0, updatedChar.hp - damage);
                logs.push(`${updatedChar.name || "You"} took ${damage} damage from ${effect.type}.`);
                break;
        }

        if (effect.duration > 1) {
            activeEffects.push({ ...effect, duration: effect.duration - 1 });
        }
    }

    return { character: updatedChar, activeEffects };
}

function getHighestCombatSkill(charOrEnemy: any): string {
    const combatSkills = ["Swordsmanship", "Brawling", "Gunslinging", "Spear", "MartialArts", "Sniper", "MysticArts"];
    let highestSkill = "Brawling";
    let highestValue = -1;

    const stats = charOrEnemy.stats || {};
    for (const skill of combatSkills) {
        const mapped = STAT_MAPPING[skill];
        const val = stats[mapped] || 0;
        if (val > highestValue) {
            highestValue = val;
            highestSkill = skill;
        }
    }
    return highestSkill;
}

function calculateDamage(attackerStats: CombatStats, defenderStats: CombatStats, attackerEffects: any[], defenderEffects: any[], isCrit: boolean, attackerCombatType?: string, defenderMythicArt?: any, attackerMythicArt?: any, attacker?: any, defender?: any): number {
    const mappedCombatSkill = attackerCombatType ? STAT_MAPPING[attackerCombatType] : "swordsmanship";
    const skillVal = (attackerStats as any)[mappedCombatSkill] || 0;
    let damage = attackerStats.strength * 2 + skillVal * 1.5;

    // Apply Weaken effect
    if (attackerEffects.some(e => e.type === "Weaken")) {
        damage *= 0.7;
    }

    // Weakness Logic (Combat Type)
    if (defenderMythicArt && defenderMythicArt.weakAgainst && attackerCombatType) {
        if (defenderMythicArt.weakAgainst.includes(attackerCombatType)) {
            damage *= 1.5; // 50% more damage if weak against the type
        }
    }

    // Elemental Logic
    if (attackerMythicArt && attackerMythicArt.elements && defenderMythicArt && defenderMythicArt.elementalWeaknesses) {
        if (attackerMythicArt.elements.some((e: string) => defenderMythicArt.elementalWeaknesses.includes(e))) {
            damage *= 1.5; // 50% more damage if elemental advantage
        }
    }

    let defense = defenderStats.defense;

    // --- Faction Passives ---
    // Navy Passive: +10% Defense when fighting high-infamy targets
    if (defender?.faction === "Navy" && (attacker?.infamy || 0) >= 50) {
        defense *= 1.1;
    }

    // Apply Fortify effect
    if (defenderEffects.some(e => e.type === "Fortify")) {
        defense *= 1.5;
    }

    damage = Math.max(1, Math.floor(damage - defense * 0.5));
    damage = Math.floor(damage * (0.9 + Math.random() * 0.2));

    if (isCrit) {
        damage = Math.floor(damage * 2);
    }

    return damage;
}

export const combatAction = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { action, techniqueId, itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processCharacterUpdates(character);

        let combat = character.combatState;
        if (!combat) throw new functions.https.HttpsError("failed-precondition", "No active combat.");

        // If battle is already finished, only allow Flee to clean up
        if (combat.isFinished) {
            if (action === "Flee") {
                transaction.update(playerRef, { combatState: null });
                return { success: true, cleanedUp: true };
            }
            throw new functions.https.HttpsError("failed-precondition", "Battle is already finished. Please close the results.");
        }

        // Turn Timeout Check
        const now = Date.now();
        if (combat.turnExpiresAt && now > combat.turnExpiresAt) {
            const logs = combat.logs || [];
            const forfeitedPlayer = combat.playerTurn ? "You" : "Opponent";
            logs.push(`${forfeitedPlayer} took too long! Forfeiting turn.`);

            // --- Issue 2: Explicit turn assignment instead of toggling ---
            const nextTurnIsPlayer = !combat.playerTurn;
            const nextTurnExpiresAt = now + TURN_TIMEOUT_MS;

            combat.playerTurn = nextTurnIsPlayer;
            combat.turnExpiresAt = nextTurnExpiresAt;
            combat.logs = logs;

            if (combat.isPvP) {
                const opponentRef = db.collection("players").doc(combat.opponentId);
                const opponentSnap = await transaction.get(opponentRef);
                if (opponentSnap.exists) {
                    const opponentData = opponentSnap.data() as any;
                    const opponentCombat = {
                        ...(opponentData.combatState || {}),
                        playerTurn: !nextTurnIsPlayer, // Explicit opposite
                        turnExpiresAt: nextTurnExpiresAt,
                        logs: logs // Synchronized logs (Issue 5)
                    };
                    // Issue 3: Both updated in same transaction
                    transaction.update(opponentRef, { combatState: opponentCombat });
                }
            }

            transaction.update(playerRef, { combatState: combat });
            return { success: true, timeout: true };
        }

        if (!combat.playerTurn) throw new functions.https.HttpsError("failed-precondition", "Not your turn.");

        const logs = [...(combat.logs || [])];
        let enemy = combat.enemy;
        let opponent: any = null;
        let opponentRef: admin.firestore.DocumentReference | null = null;

        if (combat.isPvP) {
            opponentRef = db.collection("players").doc(combat.opponentId);
            const opponentSnap = await transaction.get(opponentRef);
            if (!opponentSnap.exists) throw new functions.https.HttpsError("not-found", "Opponent not found.");
            opponent = opponentSnap.data();
        }

        let playerHp = character.hp;
        let playerEnergy = character.energy;
        let isFinished = false;
        let playerWon = false;

        // Process Player Effects at start of turn
        const pEffectResult = processStatusEffects({ ...character, hp: playerHp, name: "You" }, combat.playerEffects || [], logs);
        playerHp = pEffectResult.character.hp;
        let pActiveEffects = pEffectResult.activeEffects;

        if (playerHp <= 0) {
            isFinished = true;
            playerWon = false;
        }

        if (!isFinished) {
            const pStats = calculateCombatStats(character, pActiveEffects);
            const targetStats = combat.isPvP ? calculateCombatStats(opponent, opponent.combatState?.playerEffects || []) : calculateCombatStats(enemy, combat.enemyEffects || []);
            const targetEffects = combat.isPvP ? (opponent.combatState?.playerEffects || []) : (combat.enemyEffects || []);

            const isStunned = (pActiveEffects || []).some((e: any) => e.type === "Stun");

            if (!isStunned) {
                // --- Player Action ---
                if (action === "Attack") {
                    const hitRoll = Math.random() * 100;
                    const hitChance = Math.min(100, Math.max(0, pStats.accuracy - targetStats.dodge));

                    if (hitRoll < hitChance) {
                        const isCrit = Math.random() * 100 < pStats.critChance;
                        const attackerCombatType = getHighestCombatSkill(character);
                        const defenderMythicArt = combat.isPvP ? opponent.mythicArt : enemy.mythicArt;
                        const attackerMythicArt = character.mythicArt;
                        const damage = calculateDamage(pStats, targetStats, pActiveEffects, targetEffects, isCrit, attackerCombatType, defenderMythicArt, attackerMythicArt, character, combat.isPvP ? opponent : enemy);

                        if (isCrit) logs.push(`CRITICAL! You strike for ${damage} damage!`);
                        else logs.push(`You hit for ${damage} damage.`);

                        if (combat.isPvP) opponent.hp = Math.max(0, opponent.hp - damage);
                        else enemy.hp = Math.max(0, enemy.hp - damage);
                    } else {
                        logs.push("You missed your attack.");
                    }
                } else if (action === "Technique") {
                    if (!techniqueId) throw new functions.https.HttpsError("invalid-argument", "Missing technique ID.");

                    console.log(`Combat Action: Player ${userId} using technique: '${techniqueId}'`);
                    console.log(`Character learnedTechniques: ${JSON.stringify(character.learnedTechniques)}`);

                    // Technique validation
                    if (!character.learnedTechniques || !character.learnedTechniques.includes(techniqueId)) {
                        console.error(`Player ${userId} has not learned technique: '${techniqueId}'. Learned:`, character.learnedTechniques);
                        throw new functions.https.HttpsError("failed-precondition", "You have not learned this technique.");
                    }

                    // Issue 22: Transactional read for technique data with Robust Fallback
                    const techPath = db.collection("gameData").doc("skills").collection("techniques").doc(techniqueId);
                    const techSnap = await transaction.get(techPath);
                    let tech: any;

                    if (!techSnap.exists) {
                        console.warn(`Technique '${techniqueId}' not found in Firestore. Using STATIC_TECHNIQUES fallback.`);
                        tech = STATIC_TECHNIQUES[techniqueId];
                        if (!tech) {
                             console.error(`Technique NOT FOUND even in Static Fallback: id='${techniqueId}'`);
                             throw new functions.https.HttpsError("not-found", "Technique not found.");
                        }
                        // Robust: Seed Firestore with the missing technique so it's there next time
                        transaction.set(techPath, tech);
                    } else {
                        tech = techSnap.data();
                    }

                    // --- Validation (Security Audit Issues 23-27) ---
                    const cost = (Number.isFinite(tech.energyCost) && tech.energyCost >= 0) ? tech.energyCost : 0;
                    const techPower = (Number.isFinite(tech.power) && tech.power >= 0) ? tech.power : 0;
                    const techCooldown = (Number.isInteger(tech.cooldown) && tech.cooldown >= 0) ? Math.min(tech.cooldown, 100) : 0;
                    const accuracyBonus = (Number.isFinite(tech.accuracyBonus)) ? Math.max(-100, Math.min(tech.accuracyBonus, 100)) : 0;
                    const techEffects = Array.isArray(tech.effects) ? tech.effects.slice(0, 5) : []; // Issue 30: Limit effects

                    if (character.mythicArt) {
                        if ((character.mythicMana || 0) < cost) throw new functions.https.HttpsError("failed-precondition", "Not enough Mythic Mana.");
                        character.mythicMana = (character.mythicMana || 0) - cost;
                        character.mythicManaUpdatedAt = Date.now();
                    } else {
                        if (playerEnergy < cost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");
                        playerEnergy -= cost;
                        character.energy = playerEnergy;
                        character.energyUpdatedAt = Date.now();
                    }

                    if ((combat.cooldowns || {})[techniqueId] > 0) throw new functions.https.HttpsError("failed-precondition", "Technique on cooldown.");

                    // --- Issue 1: Cooldown starts even if we miss ---
                    const cooldowns = { ...(combat.cooldowns || {}) };
                    cooldowns[techniqueId] = techCooldown;
                    combat.cooldowns = cooldowns;

                    const hitRoll = Math.random() * 100;
                    const hitChance = Math.min(100, Math.max(0, pStats.accuracy - targetStats.dodge + accuracyBonus));

                    if (hitRoll < hitChance) {
                        const mappedTechSkill = STAT_MAPPING[tech.type] || "strength";
                        const techSkillVal = (pStats as any)[mappedTechSkill] || pStats.strength;
                        let techDamage = Math.floor(techSkillVal * techPower * 2);

                        // --- Mythic Art Scaling ---
                        const attackerMythicArt = character.mythicArt;
                        if (attackerMythicArt) {
                            // If the technique matches the Mythic Art's focus, it already gets the stat multiplier.
                            // We can add an extra "Tier" based bonus here for extra scaling.
                            const tierMultipliers: Record<string, number> = {
                                "F": 1.05, "E": 1.1, "D": 1.2, "C": 1.4, "B": 2.0, "A": 5.0, "S": 10.0, "SS": 25.0, "SSS": 50.0, "Z": 250.0
                            };
                            const tierBonus = tierMultipliers[attackerMythicArt.tier] || 1.0;
                            techDamage = Math.floor(techDamage * tierBonus);
                        }

                        // --- Elemental & Weakness Logic ---
                        const defenderMythicArt = combat.isPvP ? opponent.mythicArt : enemy.mythicArt;

                        // 1. Skill Type Weakness (e.g., Swordsmanship vs Spear)
                        if (defenderMythicArt && defenderMythicArt.weakAgainst && tech.type) {
                            if (defenderMythicArt.weakAgainst.includes(tech.type)) {
                                techDamage = Math.floor(techDamage * 1.5);
                            }
                        }

                        // 2. Elemental Weakness (Inherit elements from Mythic Art)
                        const techElements = attackerMythicArt?.elements || (tech.element ? [tech.element] : []);
                        if (techElements.length > 0 && defenderMythicArt && defenderMythicArt.elementalWeaknesses) {
                            if (techElements.some((e: string) => defenderMythicArt.elementalWeaknesses.includes(e))) {
                                techDamage = Math.floor(techDamage * 1.5);
                            }
                        }

                        // Apply Weaken/Fortify to Technique too
                        if (pActiveEffects.some((e: any) => e.type === "Weaken")) {
                            techDamage = Math.floor(techDamage * 0.7);
                        }

                        let defense = targetStats.defense;

                        // Navy Passive: +10% Defense vs High Infamy
                        const attackerInfamy = character.infamy || 0; // In this context character is attacker
                        const defenderFaction = combat.isPvP ? opponent.faction : "Neutral";
                        if (defenderFaction === "Navy" && attackerInfamy >= 50) {
                            defense *= 1.1;
                        }

                        if (targetEffects.some((e: any) => e.type === "Fortify")) {
                            defense *= 1.5;
                        }

                        techDamage = Math.max(1, Math.floor(techDamage - defense * 0.3));
                        techDamage = Math.floor(techDamage * (0.9 + Math.random() * 0.2));

                        if (combat.isPvP) {
                            opponent.hp = Math.max(0, opponent.hp - techDamage);
                            if (techEffects.length > 0) {
                                // Issue 4: Status Effect Sync - we update opponent's doc later
                                opponent.combatState.playerEffects = mergeStatusEffects(opponent.combatState.playerEffects, techEffects);
                            }
                        } else {
                            enemy.hp = Math.max(0, enemy.hp - techDamage);
                            if (techEffects.length > 0) {
                                combat.enemyEffects = mergeStatusEffects(combat.enemyEffects, techEffects);
                            }
                        }
                        logs.push(`You use ${tech.name}! Target takes ${techDamage} damage.`);
                    } else {
                        logs.push(`You used ${tech.name} but it missed!`);
                    }
                } else if (action === "Defend") {
                    logs.push("You take a defensive stance.");
                    combat.defending = true;
                } else if (action === "Item") {
                    if (!itemId) throw new functions.https.HttpsError("invalid-argument", "Missing item ID.");
                    const inventory = character.inventory || [];
                    const itemIndex = inventory.findIndex((i: any) => i.id === itemId);
                    if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");

                    const item = inventory[itemIndex];
                    if (item.type !== "Consumable") throw new functions.https.HttpsError("invalid-argument", "Item is not consumable.");

                    // Healing logic
                    const healAmount = item.healAmount ?? 30;
                    playerHp = Math.min(character.maxHp, playerHp + healAmount);

                    if (character.mythicArt) {
                        const manaHeal = item.healAmount ?? 30;
                        character.mythicMana = Math.min(character.maxMythicMana || 100, (character.mythicMana || 0) + manaHeal);
                        character.mythicManaUpdatedAt = Date.now();
                        logs.push(`You used ${item.name} and recovered ${healAmount} HP and ${manaHeal} MP.`);
                    } else {
                        logs.push(`You used ${item.name} and recovered ${healAmount} HP.`);
                    }

                    // Remove item from inventory
                    character.inventory.splice(itemIndex, 1);
                } else if (action === "Flee") {
                    if (combat.isPvP) throw new functions.https.HttpsError("failed-precondition", "Cannot flee from a duel.");
                    const fleeChance = Math.min(100, Math.max(0, 40 + (pStats.agility - targetStats.agility) * 2));
                    if (Math.random() * 100 < fleeChance) {
                        transaction.update(playerRef, { combatState: null });
                        return { fled: true };
                    } else {
                        logs.push("You failed to flee!");
                    }
                }
            } else {
                logs.push("You are stunned and cannot move!");
            }

            // Check if win/loss
            const targetHp = combat.isPvP ? opponent.hp : enemy.hp;
            if (targetHp <= 0) {
                logs.push(combat.isPvP ? `You defeated ${opponent.name}!` : `You defeated ${enemy.name}!`);
                playerWon = true;
                isFinished = true;
            } else if (!combat.isPvP) {
                // --- PvE Enemy Turn ---
                const eStats = calculateCombatStats(enemy, combat.enemyEffects || []);
                const eEffectResult = processStatusEffects({ ...enemy }, combat.enemyEffects || [], logs);
                enemy = eEffectResult.character;
                let eActiveEffects = eEffectResult.activeEffects;
                combat.enemyEffects = eActiveEffects;

                if (enemy.hp <= 0) {
                    logs.push(`The ${enemy.name} succumbed to its wounds!`);
                    playerWon = true;
                    isFinished = true;
                } else {
                    const eIsStunned = (eActiveEffects || []).some((e: any) => e.type === "Stun");
                    if (!eIsStunned) {
                        // Enemy Ability Logic
                        const useAbility = Math.random() < 0.3;
                        if (useAbility) {
                            // Issue 7: Special attack accuracy check
                            const eHitRoll = Math.random() * 100;
                            const eHitChance = Math.min(100, Math.max(0, eStats.accuracy - pStats.dodge));

                            if (eHitRoll < eHitChance) {
                                // Issue 6 & 8: Use full damage pipeline for special attacks
                                const attackerCombatType = getHighestCombatSkill(enemy);
                                const defenderMythicArt = character.mythicArt;
                                const attackerMythicArt = enemy.mythicArt;
                                let abilityDamage = calculateDamage(eStats, pStats, eActiveEffects, pActiveEffects, false, attackerCombatType, defenderMythicArt, attackerMythicArt, enemy, character);
                                abilityDamage = Math.floor(abilityDamage * 2.5);

                                if (combat.defending) {
                                    abilityDamage = Math.floor(abilityDamage * 0.5);
                                    logs.push(`${enemy.name} uses a special ability, but you were defending!`);
                                }
                                playerHp = Math.max(0, playerHp - abilityDamage);
                                logs.push(`${enemy.name} strikes you with a special attack for ${abilityDamage} damage!`);
                            } else {
                                logs.push(`${enemy.name} tried a special ability but missed!`);
                            }
                        } else {
                            const eHitRoll = Math.random() * 100;
                            const eHitChance = Math.min(100, Math.max(0, eStats.accuracy - pStats.dodge));
                            if (eHitRoll < eHitChance) {
                                const attackerCombatType = getHighestCombatSkill(enemy);
                                const defenderMythicArt = character.mythicArt;
                                const attackerMythicArt = enemy.mythicArt;
                                let eDamage = calculateDamage(eStats, pStats, eActiveEffects, pActiveEffects, false, attackerCombatType, defenderMythicArt, attackerMythicArt, enemy, character);
                                if (combat.defending) {
                                    eDamage = Math.floor(eDamage * 0.5);
                                }
                                playerHp = Math.max(0, playerHp - eDamage);
                                logs.push(`${enemy.name} hits you for ${eDamage} damage.`);
                            } else {
                                logs.push(`${enemy.name} missed its attack.`);
                            }
                        }
                    } else {
                        logs.push(`${enemy.name} is stunned!`);
                    }
                    combat.defending = false;

                    if (playerHp <= 0) {
                        logs.push("You were defeated...");
                        isFinished = true;
                        playerWon = false;
                    }
                }
            }
        }

        // Update Cooldowns
        const updatedCooldowns: any = {};
        for (const [key, val] of Object.entries(combat.cooldowns || {})) {
            if ((val as number) > 1) updatedCooldowns[key] = (val as number) - 1;
        }

        if (isFinished) {
            if (combat.isPvP) {
                // PvP Finish Logic
                const winner = playerWon ? character : opponent;
                const loser = playerWon ? opponent : character;
                const winnerRef = playerWon ? playerRef : opponentRef!;
                const loserRef = playerWon ? opponentRef! : playerRef;

                // Issue 18: Anti-Farming check
                const recentMatches = winner.recentPvP || {};
                const lastMatchWithOpponent = recentMatches[loser.id] || 0;
                const isFarming = (Date.now() - lastMatchWithOpponent) < 10 * 60 * 1000; // 10 min window

                const stealAmount = isFarming ? 0 : Math.floor(loser.gold * 0.15);
                const collectedBounty = loser.bounty || 0;
                const totalGoldGained = isFarming ? 0 : (stealAmount + Math.floor(collectedBounty * 0.1));

                // Faction Rewards (Justice Points / Pirate Reputation)
                let justiceGained = 0;
                let reputationGained = 0;
                if (!isFarming) {
                    if (winner.faction === "Navy" && loser.faction === "Pirate") {
                        justiceGained = 25 + Math.floor((loser.bounty || 0) / 1000);
                        justiceGained = Math.min(justiceGained, 250); // Cap per win
                    } else if (winner.faction === "Pirate" && loser.faction === "Navy") {
                        reputationGained = 50; // Flat reputation for defeating Marines
                    }
                }

                // Update Winner
                const winnerUpdate: any = {
                    gold: winner.gold + totalGoldGained,
                    justicePoints: (winner.justicePoints || 0) + justiceGained,
                    pirateReputation: (winner.pirateReputation || 0) + reputationGained,
                    pvpWins: (winner.pvpWins || 0) + (isFarming ? 0 : 1),
                    combatState: {
                        ...combat,
                        isFinished: true,
                        playerWon: playerWon,
                        goldEarned: playerWon ? totalGoldGained : 0,
                        logs: [...logs, playerWon ? "Victory!" : "Defeat..."]
                    },
                    hp: playerWon ? playerHp : winner.hp,
                    energy: playerWon ? playerEnergy : winner.energy,
                    [`recentPvP.${loser.id}`]: Date.now()
                };

                // Rank Swap Logic for Highest Rank Challenges
                if (combat.isRankChallenge) {
                    if (playerWon) {
                        // Challenger (winner) takes the rank
                        const tempRank = winner.rank;
                        winnerUpdate.rank = loser.rank;
                        winnerUpdate.lastRankChallengeAt = Date.now();
                        logs.push(`RANK CHALLENGE SUCCESS! ${winner.name} has claimed the title of ${winnerUpdate.rank}!`);
                    } else {
                        // Target (winner) defended the rank
                        winnerUpdate.lastRankChallengeAt = Date.now();
                        logs.push(`${winner.name} has successfully defended their rank!`);
                    }
                }

                // Issue 20: Pirate Bounty Farming Protection
                if (winner.faction === "Pirate" && !isFarming) {
                    winnerUpdate.bounty = (winner.bounty || 0) + 100;
                }

                if (playerWon) winnerUpdate.inventory = character.inventory;
                transaction.update(winnerRef, winnerUpdate);

                // --- Faction War Scoring ---
                const warRef = db.collection("gameData").doc("world").collection("war").document("current");
                const warSnap = await transaction.get(warRef);
                if (warSnap.exists) {
                    const war = warSnap.data() as any;
                    if (war.isActive && war.targetLocation === winner.currentLocation) {
                        if (winner.faction === "Navy" && loser.faction === "Pirate") {
                            transaction.update(warRef, { navyScore: admin.firestore.FieldValue.increment(10) });
                            transaction.update(winnerRef, { warContribution: admin.firestore.FieldValue.increment(10) });
                        } else if (winner.faction === "Pirate" && loser.faction === "Navy") {
                            transaction.update(warRef, { pirateScore: admin.firestore.FieldValue.increment(10) });
                            transaction.update(winnerRef, { warContribution: admin.firestore.FieldValue.increment(10) });
                        }
                    }
                }

                // Update Winner's Crew Bounty
                if (winner.crewId && winner.faction === "Pirate" && !isFarming) {
                    transaction.update(db.collection("crews").doc(winner.crewId), {
                        totalBounty: admin.firestore.FieldValue.increment(100)
                    });
                }

                // Update Loser
                // Issue 17: Consistent bounty reduction
                const bountyReduction = isFarming ? 0 : Math.floor(collectedBounty * 0.1);

                const loserUpdate: any = {
                    gold: loser.gold - stealAmount,
                    bounty: Math.max(0, (loser.bounty || 0) - bountyReduction),
                    pvpLosses: (loser.pvpLosses || 0) + (isFarming ? 0 : 1),
                    combatState: {
                        ...(loser.combatState || {}),
                        isFinished: true,
                        playerWon: !playerWon,
                        goldEarned: !playerWon ? -stealAmount : 0,
                        logs: [...logs, !playerWon ? "Victory!" : "Defeat..."]
                    },
                    hp: playerWon ? 0 : playerHp,
                    energy: playerWon ? loser.energy : playerEnergy,
                    currentLocation: "Fogi Tail Island" // Issue 21: Default Respawn
                };

                if (combat.isRankChallenge) {
                    if (playerWon) {
                        // Target (loser) is demoted to rank below
                        const factionRanks = loser.faction === "Navy" ? ["Admiral", "Fleet Admiral"] : ["Yonko", "Pirate King"];
                        loserUpdate.rank = factionRanks[0];
                        loserUpdate.lastRankChallengeAt = Date.now();
                        logs.push(`RANK CHALLENGE FAILURE! ${loser.name} has been demoted to ${loserUpdate.rank}.`);
                    } else {
                        // Challenger (loser) fails
                        loserUpdate.lastRankChallengeAt = Date.now();
                        logs.push(`RANK CHALLENGE FAILURE! ${loser.name} was unable to claim the rank.`);
                    }
                }

                if (!playerWon) loserUpdate.inventory = character.inventory;
                transaction.update(loserRef, loserUpdate);

                if (loser.crewId && loser.faction === "Pirate" && !isFarming) {
                    transaction.update(db.collection("crews").doc(loser.crewId), {
                        totalBounty: admin.firestore.FieldValue.increment(-bountyReduction)
                    });
                }

                recordLog(transaction, winner.id, "PvPWin", `Defeated ${loser.name}${isFarming ? " (Farming detected - no rewards)" : ` and collected ${totalGoldGained} Gold`}`, totalGoldGained, 0);
                recordLog(transaction, loser.id, "PvPLoss", `Lost to ${winner.name}`, -stealAmount, 0);

                return { success: true, isFinished: true, playerWon, bountyCollected: isFarming ? 0 : collectedBounty };
            } else {
                // PvE Finish Logic
                let updatedChar = { ...character, hp: playerHp, energy: playerEnergy };
                const loot = playerWon && enemy.dropTableId ? await processLoot(enemy.dropTableId) : [];
                let finalLoot: any[] = [];

                if (playerWon) {
                    const goldMultiplier = updatedChar.faction === "Pirate" ? 1.15 : 1.0;
                    updatedChar.gold += Math.floor(enemy.goldReward * goldMultiplier);
                    updatedChar.xp += enemy.xpReward;
                    if (loot.length > 0) {
                        const currentInv = updatedChar.inventory || [];
                        const maxCapacity = calculateMaxCapacity(updatedChar);
                        const freeSlots = maxCapacity - currentInv.length;
                        const lootToAdd = loot.slice(0, Math.max(0, freeSlots));
                        finalLoot = lootToAdd;

                        updatedChar.inventory = [...currentInv, ...lootToAdd];
                        if (lootToAdd.length > 0) {
                            logs.push(`Loot found: ${lootToAdd.map((i: any) => i.name).join(", ")}`);
                        }
                        if (lootToAdd.length < loot.length) {
                            logs.push("Some loot was lost because your inventory is full!");
                        }
                    }
                    updatedChar = checkLevelUp(updatedChar);

                    // Ambush Restoration
                    if (combat.intendedDestination) {
                        updatedChar.travelState = {
                            destination: combat.intendedDestination,
                            arrivalTime: combat.intendedArrivalTime,
                            startTime: combat.intendedStartTime
                        };
                        logs.push(`You defeated the ambush and continue your journey to ${combat.intendedDestination}.`);
                    }

                    recordLog(transaction, userId, "CombatWin", `Defeated ${enemy.name}`, enemy.goldReward, enemy.xpReward);
                } else {
                    const goldLost = Math.floor(updatedChar.gold * 0.1);
                    updatedChar.gold -= goldLost;

                    // Death Penalty / Respawn Logic
                    const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(updatedChar.currentLocation));
                    const location = locationSnap.data();
                    const hasCamp = location?.actions?.some((a: any) => a.type === "Camp");
                    const hasInfirmary = location?.actions?.some((a: any) => a.type === "Infirmary");

                    if (hasCamp && !hasInfirmary) {
                        // Monster Island defeat: stay at location, start 2-min healing timer
                        updatedChar.hp = 0;
                        updatedChar.healingState = { endTime: Date.now() + HEALING_DURATION_MS };
                    } else {
                        // Normal defeat: respawn at home
                        updatedChar.currentLocation = "Fogi Tail Island";
                        updatedChar.hp = 0;
                        updatedChar.healingState = null;
                    }

                    updatedChar.energy = character.maxEnergy;
                    recordLog(transaction, userId, "CombatLoss", `Defeated by ${enemy.name}`, -goldLost, 0);
                }

                updatedChar.combatState = {
                    ...combat,
                    isFinished: true,
                    playerWon,
                    goldEarned: playerWon ? enemy.goldReward : 0,
                    xpEarned: playerWon ? enemy.xpReward : 0,
                    loot: finalLoot,
                    logs
                };

                transaction.update(playerRef, updatedChar);
                return { success: true, isFinished: true, playerWon, logs };
            }
        } else {
            // Update combat state and swap turns if PvP
            if (combat.isPvP) {
                const nextLogs = [...logs, `It is now ${opponent.name}'s turn.`];

                // Issue 3-5: Synchronize Combat States
                transaction.update(playerRef, {
                    hp: playerHp,
                    energy: playerEnergy,
                    energyUpdatedAt: character.energyUpdatedAt,
                    inventory: character.inventory,
                    combatState: {
                        ...combat,
                        enemy: { ...combat.enemy, hp: opponent.hp }, // Opponent HP
                        playerTurn: false,
                        logs: nextLogs,
                        cooldowns: updatedCooldowns,
                        playerEffects: pActiveEffects,
                        turnExpiresAt: now + TURN_TIMEOUT_MS
                    }
                });

                const opponentCombat = {
                    ...opponent.combatState,
                    enemy: { ...opponent.combatState.enemy, hp: playerHp }, // Your HP
                    playerTurn: true,
                    logs: nextLogs,
                    turnExpiresAt: now + TURN_TIMEOUT_MS
                };
                transaction.update(opponentRef!, {
                    hp: opponent.hp,
                    combatState: opponentCombat
                });
            } else {
                transaction.update(playerRef, {
                    hp: playerHp,
                    energy: playerEnergy,
                    energyUpdatedAt: character.energyUpdatedAt,
                    inventory: character.inventory,
                    combatState: { ...combat, enemy, logs, turnCount: (combat.turnCount || 0) + 1, cooldowns: updatedCooldowns, playerEffects: pActiveEffects }
                });
            }
            return { success: true, logs };
        }
    });
});

export const challengeHighestRank = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const userId = context.auth.uid;
    const challengerRef = db.collection("players").doc(userId);
    const targetRef = db.collection("players").doc(targetId);

    return db.runTransaction(async (transaction) => {
        const [challengerSnap, targetSnap] = await Promise.all([
            transaction.get(challengerRef),
            transaction.get(targetRef)
        ]);

        if (!challengerSnap.exists || !targetSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Player not found.");
        }

        let challenger = challengerSnap.data() as any;
        challenger = processCharacterUpdates(challenger);

        assertCanPerformAction(challenger, "challenge for rank", { blockBusy: true, blockHealing: true });

        const target = targetSnap.data() as any;
        const targetWithUpdates = processCharacterUpdates(target);

        if (userId === targetId) throw new functions.https.HttpsError("invalid-argument", "You cannot challenge yourself.");
        if (challenger.faction !== target.faction) throw new functions.https.HttpsError("failed-precondition", "You can only challenge members of your own faction.");

        const highestRanks = ["Fleet Admiral", "Pirate King"];
        if (!highestRanks.includes(target.rank)) throw new functions.https.HttpsError("failed-precondition", "Target does not hold a challengeable highest rank.");

        const challengerCandidateRanks = ["Admiral", "Yonko"];
        if (!challengerCandidateRanks.includes(challenger.rank) && challenger.level < 300) {
            throw new functions.https.HttpsError("failed-precondition", "You must be Level 300 and at the rank of Admiral or Yonko to challenge.");
        }

        // Cooldown check: 2 days (172,800,000 ms)
        const cooldown = 2 * 24 * 60 * 60 * 1000;
        const now = Date.now();
        if (target.lastRankChallengeAt && (now - target.lastRankChallengeAt < cooldown)) {
            const remainingHours = Math.ceil((cooldown - (now - target.lastRankChallengeAt)) / (60 * 60 * 1000));
            throw new functions.https.HttpsError("failed-precondition", `This rank holder was recently challenged. Please wait ${remainingHours} more hours.`);
        }

        if (!target.isOnline) throw new functions.https.HttpsError("failed-precondition", "The rank holder is currently offline.");
        if (targetWithUpdates.combatState || targetWithUpdates.travelState || targetWithUpdates.trainingState || targetWithUpdates.healingState) {
            throw new functions.https.HttpsError("failed-precondition", "The rank holder is currently busy or resting.");
        }

        const challengerCombat = {
            opponentId: targetId,
            isPvP: true,
            isRankChallenge: true,
            playerTurn: true,
            logs: [`RANK CHALLENGE: You have challenged ${target.name} for the title of ${target.rank}!`],
            enemy: {
                name: target.name,
                level: target.level,
                hp: target.hp,
                maxHp: target.maxHp,
                stats: target.stats
            },
            isFinished: false,
            turnCount: 0,
            playerEffects: [],
            enemyEffects: [],
            cooldowns: {},
            turnExpiresAt: now + TURN_TIMEOUT_MS
        };

        const targetCombat = {
            opponentId: userId,
            isPvP: true,
            isRankChallenge: true,
            playerTurn: false,
            logs: [`RANK CHALLENGE: ${challenger.name} has challenged you for your title of ${target.rank}!`],
            enemy: {
                name: challenger.name,
                level: challenger.level,
                hp: challenger.hp,
                maxHp: challenger.maxHp,
                stats: challenger.stats
            },
            isFinished: false,
            turnCount: 0,
            playerEffects: [],
            enemyEffects: [],
            cooldowns: {},
            turnExpiresAt: now + TURN_TIMEOUT_MS
        };

        transaction.update(challengerRef, { ...challenger, combatState: challengerCombat });
        transaction.update(targetRef, {
            hp: targetWithUpdates.hp,
            energy: targetWithUpdates.energy,
            energyUpdatedAt: targetWithUpdates.energyUpdatedAt,
            healingState: targetWithUpdates.healingState,
            combatState: targetCombat
        });

        return { success: true };
    });
});

export const attackPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { defenderId } = data;
    const userId = context.auth.uid;
    const attackerRef = db.collection("players").doc(userId);
    const defenderRef = db.collection("players").doc(defenderId);

    return db.runTransaction(async (transaction) => {
        const [attackerSnap, defenderSnap] = await Promise.all([
            transaction.get(attackerRef),
            transaction.get(defenderRef)
        ]);

        if (!attackerSnap.exists || !defenderSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Player not found.");
        }

        let attacker = attackerSnap.data() as any;
        attacker = processCharacterUpdates(attacker);

        assertCanPerformAction(attacker, "fight", { blockBusy: true, blockHealing: true });

        const defender = defenderSnap.data() as any;
        const defenderWithHealing = processCharacterUpdates(defender);
        const defenderFinal = defenderWithHealing;

        if (userId === defenderId) throw new functions.https.HttpsError("invalid-argument", "You cannot attack yourself.");

        // Block list check
        if (defender.blocked && defender.blocked.includes(userId)) {
            throw new functions.https.HttpsError("permission-denied", "You have been blocked by this player.");
        }

        if (!defender.isOnline) throw new functions.https.HttpsError("failed-precondition", "Target is currently offline.");

        if (attacker.currentLocation !== defender.currentLocation) {
            throw new functions.https.HttpsError("failed-precondition", "Target is not at your location.");
        }

        // Safe zone check
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(attacker.currentLocation));
        if (locationSnap.exists && locationSnap.data()?.isSafe) {
            throw new functions.https.HttpsError("failed-precondition", "PvP is not allowed in safe zones.");
        }

        if (defenderFinal.combatState || defenderFinal.travelState || defenderFinal.trainingState || defenderFinal.healingState) {
            throw new functions.https.HttpsError("failed-precondition", "Target is already busy or resting.");
        }

        // Faction-based attack rules and infamy
        let infamyGain = 0;
        let kickFromFaction = false;
        let changeToPirate = false;

        if (attacker.faction === "Navy") {
            if (defender.faction === "Neutral") {
                infamyGain = 10;
            } else if (defender.faction === "Navy") {
                throw new functions.https.HttpsError("failed-precondition", "You cannot attack fellow Navy members.");
            }
            // Navy vs Pirate is fine
        } else if (attacker.faction === "Neutral") {
            if (defender.faction === "Navy") {
                infamyGain = 10;
            }
            // Neutral vs Pirate is fine, Neutral vs Neutral is fine (Pirates can attack anyone)
        }
        // Pirate vs anyone is fine

        if (infamyGain > 0) {
            attacker.infamy = (attacker.infamy || 0) + infamyGain;
            if (attacker.infamy >= 100) {
                attacker.infamy = 0;
                if (attacker.faction === "Navy") {
                    attacker.faction = "Neutral";
                    attacker.rank = "Dishonored Sailor";
                } else if (attacker.faction === "Neutral") {
                    attacker.faction = "Pirate";
                    attacker.rank = "Outlaw";
                }
            }
        }

        const attackerCombat = {
            opponentId: defenderId,
            isPvP: true,
            playerTurn: true,
            logs: [`You initiated a duel with ${defender.name}!`],
            enemy: {
                name: defender.name,
                level: defender.level,
                hp: defender.hp,
                maxHp: defender.maxHp,
                stats: defender.stats
            },
            isFinished: false,
            turnCount: 0,
            playerEffects: [],
            enemyEffects: [],
            cooldowns: {},
            turnExpiresAt: Date.now() + TURN_TIMEOUT_MS
        };

        const defenderCombat = {
            opponentId: userId,
            isPvP: true,
            playerTurn: false,
            logs: [`${attacker.name} has challenged you to a duel!`],
            enemy: {
                name: attacker.name,
                level: attacker.level,
                hp: attacker.hp,
                maxHp: attacker.maxHp,
                stats: attacker.stats
            },
            isFinished: false,
            turnCount: 0,
            playerEffects: [],
            enemyEffects: [],
            cooldowns: {},
            turnExpiresAt: Date.now() + TURN_TIMEOUT_MS
        };

        transaction.update(attackerRef, {
            ...attacker,
            combatState: attackerCombat
        });
        transaction.update(defenderRef, {
            hp: defenderFinal.hp,
            energy: defenderFinal.energy,
            energyUpdatedAt: defenderFinal.energyUpdatedAt,
            healingState: defenderFinal.healingState,
            combatState: defenderCombat
        });

        return { success: true };
    });
});

export const startHealing = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "heal", { requireHp: false, blockBusy: true });

        if (character.hp >= character.maxHp) throw new functions.https.HttpsError("failed-precondition", "You are already at full health.");
        if (character.healingState) throw new functions.https.HttpsError("failed-precondition", "You are already resting.");

        // Check if current location has an infirmary or a camp
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const hasHealingAction = location?.actions?.some((a: any) => a.type === "Infirmary" || a.type === "Camp");
        if (!hasHealingAction) throw new functions.https.HttpsError("failed-precondition", "There is no infirmary or camp at your current location.");

        const endTime = Date.now() + HEALING_DURATION_MS;
        transaction.update(playerRef, { healingState: { endTime } });
        return { success: true, endTime };
    });
});

export const instantHeal = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "heal", { requireHp: false, blockBusy: true });

        if (character.hp >= character.maxHp) throw new functions.https.HttpsError("failed-precondition", "You are already at full health.");
        if (character.gold < 50) throw new functions.https.HttpsError("failed-precondition", "Not enough gold for instant treatment.");

        // Check if current location has an infirmary or a camp
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const hasHealingAction = location?.actions?.some((a: any) => a.type === "Infirmary" || a.type === "Camp");
        if (!hasHealingAction) throw new functions.https.HttpsError("failed-precondition", "There is no infirmary or camp at your current location.");

        transaction.update(playerRef, {
            hp: character.maxHp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: null,
            gold: admin.firestore.FieldValue.increment(-50),
        });
        recordLog(transaction, userId, "InstantHeal", "Paid for immediate treatment", -50, 0);
        return { success: true };
    });
});

export const purchaseMedicalLicense = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "purchase a license", { blockBusy: true, blockHealing: true });

        if (character.hasMedicalLicense) throw new functions.https.HttpsError("already-exists", "You already have a medical license.");
        if (character.gold < 15000) throw new functions.https.HttpsError("failed-precondition", "Not enough gold (15,000 required).");

        if (character.mythicArt && !character.mythicArt.canLearnNonCombatSkills) {
            throw new functions.https.HttpsError("failed-precondition", "Your Mythic Art forbids practicing medicine.");
        }

        transaction.update(playerRef, {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            gold: admin.firestore.FieldValue.increment(-15000),
            hasMedicalLicense: true
        });
        recordLog(transaction, userId, "PurchaseMedicalLicense", "Obtained a medical license", -15000, 0);
        return { success: true };
    });
});

export const healPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { targetPlayerId } = data;
    const userId = context.auth.uid;
    const healerRef = db.collection("players").doc(userId);
    const targetRef = db.collection("players").doc(targetPlayerId);

    return db.runTransaction(async (transaction) => {
        const [healerSnap, targetSnap] = await Promise.all([
            transaction.get(healerRef),
            transaction.get(targetRef)
        ]);

        if (!healerSnap.exists || !targetSnap.exists) throw new functions.https.HttpsError("not-found", "Player not found.");
        let healer = healerSnap.data() as any;
        let target = targetSnap.data() as any;

        healer = processCharacterUpdates(healer);
        target = processCharacterUpdates(target);

        assertCanPerformAction(healer, "heal others", { blockBusy: true, blockHealing: true });

        if (userId === targetPlayerId) throw new functions.https.HttpsError("invalid-argument", "You cannot heal yourself for Medical skill XP. Use resting or items.");

        if (healer.mythicArt && !healer.mythicArt.canLearnNonCombatSkills) {
            throw new functions.https.HttpsError("failed-precondition", "Your Mythic Art forbids non-combat actions like healing.");
        }

        if (!healer.hasMedicalLicense) throw new functions.https.HttpsError("failed-precondition", "You do not have a medical license.");
        if (healer.currentLocation !== target.currentLocation) throw new functions.https.HttpsError("failed-precondition", "Target is not at your location.");

        // Check if current location has an infirmary or a camp
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(healer.currentLocation));
        const location = locationSnap.data();
        const hasHealingAction = location?.actions?.some((a: any) => a.type === "Infirmary" || a.type === "Camp");
        if (!hasHealingAction) throw new functions.https.HttpsError("failed-precondition", "There is no infirmary or camp at your current location.");

        if (!target.healingState) throw new functions.https.HttpsError("failed-precondition", "Target is not currently resting.");
        if (target.hp >= target.maxHp) throw new functions.https.HttpsError("failed-precondition", "Target is already at full health.");

        const medicalSkill = healer.professionStats?.medical || 0;
        const healAmount = 10 + (medicalSkill * 2);

        // Issue 10: Anti-abuse - calculate actual HP restored
        const actualHealed = Math.min(healAmount, target.maxHp - target.hp);
        const newHp = Math.min(target.maxHp, target.hp + healAmount);

        const targetUpdate: any = {
            hp: newHp,
            energy: target.energy,
            energyUpdatedAt: target.energyUpdatedAt,
            healingState: (newHp >= target.maxHp) ? null : target.healingState
        };

        // Issue 9: Award XP using checkLevelUp for immediate level processing
        let healerDataForUpdate = {
            ...healer,
            xp: healer.xp + (actualHealed >= 5 ? 10 : 0) // Only XP if restored at least 5 HP
        };

        if (actualHealed >= 5) {
            healerDataForUpdate = checkLevelUp(healerDataForUpdate);
        }

        const healerUpdate: any = {
            hp: healerDataForUpdate.hp,
            energy: healerDataForUpdate.energy,
            energyUpdatedAt: healerDataForUpdate.energyUpdatedAt,
            healingState: healerDataForUpdate.healingState,
            xp: healerDataForUpdate.xp,
            level: healerDataForUpdate.level,
            maxHp: healerDataForUpdate.maxHp,
            stats: healerDataForUpdate.stats,
            "professionStats.medical": admin.firestore.FieldValue.increment(actualHealed >= 5 ? 1 : 0)
        };

        transaction.update(targetRef, targetUpdate);
        transaction.update(healerRef, healerUpdate);

        recordLog(transaction, userId, "HealPlayer", `Healed ${target.name} for ${actualHealed} HP`, 0, actualHealed >= 5 ? 10 : 0);
        return { success: true, healedAmount: actualHealed, fullyHealed: newHp >= target.maxHp };
    });
});

export const startMonsterHunt = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "hunt monsters", { blockBusy: true, blockHealing: true });

        // Issue 13: Verify location exists and is not safe
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        if (!locationSnap.exists) {
            throw new functions.https.HttpsError("not-found", `Location ${character.currentLocation} definition not found.`);
        }
        const location = locationSnap.data();
        if (location?.isSafe) {
            throw new functions.https.HttpsError("failed-precondition", "There are no monsters to hunt in safe zones.");
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < 5) throw new functions.https.HttpsError("failed-precondition", "Not enough energy (5 required).");

        const enemy = generateEnemy(character.level);

        transaction.update(playerRef, {
            hp: character.hp,
            healingState: character.healingState,
            energy: energy - 5,
            energyUpdatedAt,
            combatState: {
                enemy: enemy,
                playerTurn: true,
                logs: [`You went out searching and found a ${enemy.name}!`],
                isFinished: false,
                playerWon: false,
                turnCount: 0,
                playerEffects: [],
                enemyEffects: [],
                cooldowns: {}
            }
        });

        recordLog(transaction, userId, "StartMonsterHunt", `Hunting a level ${enemy.level} ${enemy.name}`, 0, 0);
        return { success: true, enemy };
    });
});

// --- Existing Functions ---

export const completeMission = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { missionId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const missionRef = db.collection("gameData").doc("world").collection("missions").doc(missionId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, missionSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(missionRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!missionSnap.exists) throw new functions.https.HttpsError("not-found", "Mission not found.");

        let character = playerSnap.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processCharacterUpdates(character);

        // Issue 11: Use central action validation
        assertCanPerformAction(character, "complete missions", { blockBusy: true, blockHealing: true });

        const mission = missionSnap.data() as any;

        if (character.level < (mission.minLevel || 1)) {
            throw new functions.https.HttpsError("failed-precondition", "Level too low for this mission.");
        }

        if (mission.factionRequirement && mission.factionRequirement !== "Neutral" && character.faction !== mission.factionRequirement) {
            throw new functions.https.HttpsError("failed-precondition", "You do not belong to the required faction.");
        }

        if (mission.locationId && character.currentLocation !== mission.locationId) {
            throw new functions.https.HttpsError("failed-precondition", "You are not at the required location for this mission.");
        }

        if (mission.statRequirement) {
            for (const [stat, value] of Object.entries(mission.statRequirement)) {
                const charStat = character.stats[STAT_MAPPING[stat] || stat];
                if ((charStat || 0) < (value as number)) {
                    throw new functions.https.HttpsError("failed-precondition", `Need ${value} ${stat} for this mission.`);
                }
            }
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < mission.energyCost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        let updatedChar = {
            ...character,
            energy: energy - mission.energyCost,
            energyUpdatedAt: energyUpdatedAt,
            gold: character.gold + (mission.goldReward || 0),
            xp: character.xp + (mission.xpReward || 0)
        };

        if (mission.isRankUp && mission.targetRank) {
            if (mission.targetRank === "Fleet Admiral" || mission.targetRank === "Pirate King") {
                // Check if slots are full
                const holdersSnap = await db.collection("players").where("rank", "==", mission.targetRank).get();
                if (holdersSnap.size >= 2) {
                    throw new functions.https.HttpsError("failed-precondition", `There are already 2 players holding the title of ${mission.targetRank}. You must challenge one of them for their spot.`);
                }
            }
            updatedChar.rank = mission.targetRank;
        }

        updatedChar = checkLevelUp(updatedChar);
        transaction.update(playerRef, updatedChar);
        recordLog(transaction, userId, "MissionCompleted", `Completed mission ${mission.title || missionId}`, mission.goldReward, mission.xpReward);

        return { success: true, rewards: { gold: mission.goldReward, xp: mission.xpReward } };
    });
});

export const createCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { name, description } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const crewRef = db.collection("crews").doc();

    return db.runTransaction(async (transaction) => {
        const playerSnap = await transaction.get(playerRef);
        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = playerSnap.data() as any;
        if (character.crewId) throw new functions.https.HttpsError("already-exists", "Player is already in a crew.");
        if (character.gold < 10000) throw new functions.https.HttpsError("failed-precondition", "Need 10,000 Gold to start a crew.");

        const crewData = {
            id: crewRef.id,
            name: name,
            description: description,
            captainId: userId,
            members: [userId],
            totalBounty: character.bounty,
            level: 1,
            experience: 0
        };

        transaction.set(crewRef, crewData);
        transaction.update(playerRef, { crewId: crewRef.id, gold: character.gold - 10000 });
        recordLog(transaction, userId, "CreateCrew", `Created crew ${name}`, -10000, 0);

        return { success: true, crewId: crewRef.id };
    });
});

export const joinCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { crewId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const crewRef = db.collection("crews").doc(crewId);
    const inviteRef = db.collection("crewInvites").doc(`${crewId}_${userId}`);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, crewSnap, inviteSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(crewRef),
            transaction.get(inviteRef)
        ]);

        if (!inviteSnap.exists || inviteSnap.data()?.status !== "pending") {
            throw new functions.https.HttpsError("failed-precondition", "A pending invitation is required to join a crew.");
        }

        executeCrewJoin(transaction, userId, playerRef, playerSnap, crewRef, crewSnap, inviteRef, crewId);

        return { success: true };
    });
});

export const heartbeat = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) return { success: false };

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processCharacterUpdates(character);

        const updates: any = {
            lastOnline: Date.now(),
            isOnline: true,
            hp: character.hp,
            healingState: character.healingState,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt
        };

        // Admin Repair/Sync
        if (context.auth?.token.admin && !character.isAdmin) {
            updates.isAdmin = true;
        }

        // Rank Repair Logic: Ensure faction members have their correct starting rank if it was lost
        if (character.faction === "Navy" && (character.rank === "Novice Sailor" || !character.rank)) {
            updates.rank = "Navy Cadet";
        } else if (character.faction === "Pirate" && (character.rank === "Novice Sailor" || !character.rank)) {
            updates.rank = "Rogue";
        }

        // Skill Repair Logic: Ensure required techniques are learned (baseline + current Mythic Art)
        const currentTechs = character.learnedTechniques || [];
        const baseline = ["bash"];
        let requiredTechs = [...baseline];

        if (character.mythicArt) {
            const artTechs = character.mythicArt.techniques || [];
            requiredTechs = [...new Set([...baseline, ...artTechs])];
        }

        // Only add missing required techniques, don't remove existing ones
        const missingTechs = requiredTechs.filter(t => !currentTechs.includes(t));

        if (missingTechs.length > 0) {
            updates.learnedTechniques = admin.firestore.FieldValue.arrayUnion(...missingTechs);
        }

        // Random Travel Events (30% chance during heartbeat if traveling)
        if (character.travelState && character.travelState.arrivalTime > Date.now()) {
            if (Math.random() < 0.30) {
                const eventRoll = Math.random();

                if (eventRoll < 0.25) {
                    // 1. Ambush (Combat) during travel
                    const enemy = generateEnemy(character.level);
                    const arrivalTime = character.travelState.arrivalTime;
                    const destination = character.travelState.destination;
                    const startTime = character.travelState.startTime;

                    updates.combatState = {
                        enemy: enemy,
                        playerTurn: true,
                        logs: [`While voyaging to ${destination}, you were AMBUSHED by a ${enemy.name}!`],
                        isFinished: false,
                        playerWon: false,
                        turnCount: 0,
                        playerEffects: [],
                        enemyEffects: [],
                        cooldowns: {},
                        intendedDestination: destination,
                        intendedArrivalTime: arrivalTime,
                        intendedStartTime: startTime,
                        turnExpiresAt: Date.now() + TURN_TIMEOUT_MS
                    };
                    // Clear travel state while in combat to prevent timer issues, will be restored on win
                    updates.travelState = null;
                } else {
                    const travelEvents = [
                        { msg: "You spotted a pod of dolphins jumping alongside the ship.", gold: 0, energy: 5 },
                        { msg: "A brief rain shower washed the deck. The crew feels refreshed.", gold: 0, energy: 10 },
                        { msg: "You found a small pouch of gold stuck in a fishing net! +100 Gold", gold: 100, energy: 0 },
                        { msg: "The sunset is particularly beautiful tonight. It fills you with determination.", gold: 0, energy: 0, hp: 5 },
                        { msg: "A passing merchant ship shared some supplies. +200 Gold", gold: 200, energy: 0 },
                        { msg: "You discovered a small uncharted sandbar with some washed up crates. +300 Gold", gold: 300, energy: 0 },
                        { msg: "A calm sea allows for some extra rest. +20 Energy", gold: 0, energy: 20 },
                        { msg: "The crew caught a massive fish! Everyone is well-fed. +15 Energy", gold: 0, energy: 15 },
                        { msg: "Distant gulls cry out, signaling you're on the right path.", gold: 0, energy: 0 },
                        { msg: "The ship's cook made a special hardtack soup. +5 Energy", gold: 0, energy: 5 }
                    ];
                    const selected = travelEvents[Math.floor(Math.random() * travelEvents.length)];
                    const newEvent = { message: selected.msg, timestamp: Date.now() };

                    updates["travelState.events"] = admin.firestore.FieldValue.arrayUnion(newEvent);
                    if (selected.gold) updates.gold = admin.firestore.FieldValue.increment(selected.gold);
                    if (selected.energy) updates.energy = Math.min(character.maxEnergy, updates.energy + selected.energy);
                    if (selected.hp) updates.hp = Math.min(character.maxHp, updates.hp + selected.hp);
                }
            }
        }

        // --- Faction War Presence Scoring ---
        const warRef = db.collection("gameData").doc("world").collection("war").document("current");
        const warSnap = await transaction.get(warRef);
        if (warSnap.exists) {
            const war = warSnap.data() as any;
            if (war.isActive && war.targetLocation === character.currentLocation) {
                // Grant 1 contribution point per heartbeat in war zone
                updates.warContribution = admin.firestore.FieldValue.increment(1);
                if (character.faction === "Navy") {
                    transaction.update(warRef, { navyScore: admin.firestore.FieldValue.increment(1) });
                } else if (character.faction === "Pirate") {
                    transaction.update(warRef, { pirateScore: admin.firestore.FieldValue.increment(1) });
                }
            }
        }

        transaction.update(playerRef, updates);
        return { success: true };
    });
});

export const seedWorld = functions.https.onCall(async (data, context) => {
    // Basic admin check
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const batch = db.batch();

    const locations = [
        { name: "Fogi Tail Island", region: "East Blue", description: "A peaceful starting island with clear blue waters.", isSafe: true, weather: "Sunny", x: 0, y: 0, controlledBy: "Neutral", actions: [{ type: "Training", label: "Dojo", icon: "🥋" }, { type: "Kitchen", label: "Galley", icon: "🍳" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Medical Clinic", icon: "🏥" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }, { type: "Market", label: "General Store", icon: "🛍" }] },
        { name: "Ironcrest Isle", region: "East Blue", description: "A rocky island known for its iron mines and blacksmiths.", isSafe: false, weather: "Foggy", x: 640, y: 160, controlledBy: "Neutral", actions: [{ type: "Forge", label: "Grand Forge", icon: "⚒" }, { type: "Market", label: "Sword Shop", icon: "⚔", parameter: "Sword" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Amber Reach", region: "East Blue", description: "A trade hub known for its amber deposits.", isSafe: false, weather: "Sunny", x: -320, y: 600, controlledBy: "Neutral", actions: [{ type: "Market", label: "Ingredient Market", icon: "🥦", parameter: "Ingredient" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Sunken Reef", region: "East Blue", description: "A shallow reef area teeming with colorful fish and hidden treasures.", isSafe: false, weather: "Clear", x: 280, y: 360, controlledBy: "Neutral", actions: [{ type: "Fishing", label: "Fishing Spot", icon: "🎣" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }, { type: "Market", label: "Fishing Gear", icon: "🎣", parameter: "Fishing Rod" }] },
        { name: "Shadow Fen", region: "East Blue", description: "A murky swamp island filled with dangerous creatures.", isSafe: false, weather: "Overcast", x: -1200, y: -400, controlledBy: "Neutral", actions: [{ type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Tortuga Bay", region: "South Blue", description: "A bustling pirate haven filled with taverns and mystery.", isSafe: false, weather: "Tropical", x: 120, y: -840, controlledBy: "Pirate", actions: [{ type: "Tavern", label: "The Salty Dog", icon: "🍻" }, { type: "Market", label: "Bazaar", icon: "💰" }, { type: "Market", label: "Smuggler's Den", icon: "🕶️", parameter: "Pirate" }, { type: "Expedition", label: "Treasure Hunt", icon: "💎" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Pirate Doctor", icon: "🏥" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Pirate's Den", region: "South Blue", description: "An outlaw stronghold hidden within jagged cliffs.", isSafe: false, weather: "Stormy", x: 1400, y: -1400, controlledBy: "Pirate", actions: [{ type: "Arena", label: "Duel Pit", icon: "⚔" }, { type: "Market", label: "Smuggler's Den", icon: "🕶️", parameter: "Pirate" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Kraken's Rest", region: "South Blue", description: "A desolate island graveyard of sunken ships and sea monsters.", isSafe: false, weather: "Stormy", x: -1600, y: -1600, controlledBy: "Neutral", actions: [{ type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Aqua", region: "South Blue", description: "A strictly regulated military base maintaining order.", isSafe: false, weather: "Clear", x: -640, y: -440, controlledBy: "Navy", actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Navy Hospital", icon: "🏥" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Market", label: "Navy Armory", icon: "⚔️", parameter: "Navy" }, { type: "Market", label: "Navy Commendations", icon: "🎖️", parameter: "Navy" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Terra", region: "Grand Line", description: "A frontier navy post watching over the Grand Line entrance.", isSafe: false, weather: "Windy", x: -1200, y: 800, controlledBy: "Navy", actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Market", label: "Pistol Shop", icon: "🔫", parameter: "Pistol" }, { type: "Market", label: "Navy Armory", icon: "⚔️", parameter: "Navy" }, { type: "Market", label: "Navy Commendations", icon: "🎖️", parameter: "Navy" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Ignis", region: "Grand Line", description: "A strategic outpost near the volcanic islands.", isSafe: false, weather: "Hot", x: 1600, y: 1200, controlledBy: "Navy", actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Market", label: "Navy Armory", icon: "⚔️", parameter: "Navy" }, { type: "Market", label: "Navy Commendations", icon: "🎖️", parameter: "Navy" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Crystal Cove", region: "Grand Line", description: "An island made of glowing crystals and mysterious energy.", isSafe: false, weather: "Shimmering", x: 1120, y: 480, controlledBy: "Neutral", actions: [{ type: "BlackMarket", label: "Crystal Trader", icon: "💎" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Volcano Peak", region: "Grand Line", description: "An active volcano island with treacherous terrain.", isSafe: false, weather: "Ashy", x: 1680, y: 960, controlledBy: "Neutral", actions: [{ type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Whispering Woods", region: "Grand Line", description: "A dense forest where the trees seem to whisper secrets.", isSafe: false, weather: "Mist", x: -600, y: 720, controlledBy: "Neutral", actions: [{ type: "Cave", label: "Ancient Grotto", icon: "🕳" }, { type: "Market", label: "Sniper Shop", icon: "🎯", parameter: "Sniper" }, { type: "Observatory", label: "Star Gazing", icon: "🔭" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Serpent's Maw", region: "Grand Line", description: "A terrifying island shaped like a giant serpent's head.", isSafe: false, weather: "Foggy", x: 2000, y: 2000, controlledBy: "Neutral", actions: [{ type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Island of World Secrets", region: "Unknown", description: "A mystical island shrouded in secrets. Here, you can roll for Mythic Arts.", isSafe: true, weather: "Celestial", x: 4000, y: 4000, controlledBy: "Neutral", actions: [{ type: "MythicRoll", label: "Ancient Altar", icon: "✨" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Champion's Colosseum", region: "Grand Line", description: "A legendary island where the strongest warriors gather for ranked battles. Home to the world-renowned Arena.", isSafe: true, weather: "Clear", x: 2500, y: -2500, controlledBy: "Neutral", actions: [{ type: "Arena", label: "Grand Arena", icon: "🏟" }, { type: "Infirmary", label: "Arena Hospital", icon: "🏥" }, { type: "Market", label: "General Store", icon: "🛍" }, { type: "Market", label: "Armor Shop", icon: "🛡", parameter: "Armor" }, { type: "BlackMarket", label: "Gladiator's Black Market", icon: "🕵" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] }
    ];

    // Delete old/invalid locations
    const validLocationNames = locations.map(l => l.name);
    const existingLocationsSnap = await db.collection("gameData").doc("world").collection("locations").get();
    existingLocationsSnap.forEach(doc => {
        if (!validLocationNames.includes(doc.id)) {
            batch.delete(doc.ref);
        }
    });

    for (const loc of locations) {
        console.log(`Seeding location: ${loc.name}, Safe: ${loc.isSafe}`);
        const ref = db.collection("gameData").doc("world").collection("locations").doc(loc.name);
        batch.set(ref, { ...loc, id: loc.name });
    }

    const enemies = [
        { id: "sea_serpent", name: "Sea Serpent", minLevel: 1, maxLevel: 10, hp: 60, stats: { strength: 8, endurance: 8, agility: 5 }, goldRewardMin: 10, goldRewardMax: 30, xpReward: 20, dropTableId: "basic_sea_loot" },
        { id: "pirate_scout", name: "Pirate Scout", minLevel: 3, maxLevel: 15, hp: 80, stats: { strength: 10, endurance: 8, agility: 10 }, goldRewardMin: 20, goldRewardMax: 50, xpReward: 35, dropTableId: "basic_sea_loot" },
        { id: "giant_squid", name: "Giant Squid", minLevel: 10, maxLevel: 25, hp: 200, stats: { strength: 20, endurance: 20, agility: 5 }, goldRewardMin: 100, goldRewardMax: 200, xpReward: 100, dropTableId: "rare_sea_loot" },
        { id: "ghost_ship", name: "Ghost Pirate", minLevel: 15, maxLevel: 40, hp: 350, stats: { strength: 25, endurance: 25, agility: 15 }, goldRewardMin: 300, goldRewardMax: 600, xpReward: 250, dropTableId: "rare_sea_loot" }
    ];

    for (const enemy of enemies) {
        const ref = db.collection("gameData").doc("world").collection("enemies").doc(enemy.id);
        batch.set(ref, enemy);
    }

    const items = [
        { id: "fish_scales", name: "Fish Scales", description: "Shiny scales from a sea creature.", type: "Miscellaneous", rarity: "Common", price: 5 },
        { id: "sea_shell", name: "Sea Shell", description: "A pretty shell from the ocean floor.", type: "Miscellaneous", rarity: "Common", price: 10 },
        { id: "rusty_cutlass", name: "Rusty Cutlass", description: "An old, worn-out sword.", type: "Weapon", rarity: "Common", price: 50, levelRequirement: 1, statBonus: { strength: 2 }, statRequirements: { swordsmanship: 2 }, weaponCategory: "Sword" },
        { id: "steel_sabre", name: "Steel Sabre", description: "A sharp and reliable blade.", type: "Weapon", rarity: "Uncommon", price: 500, levelRequirement: 5, statBonus: { strength: 5, agility: 2 }, statRequirements: { swordsmanship: 10 }, weaponCategory: "Sword" },
        { id: "heavy_claymore", name: "Heavy Claymore", description: "A massive blade that requires great strength to wield.", type: "Weapon", rarity: "Uncommon", price: 1200, levelRequirement: 10, statBonus: { strength: 12 }, statRequirements: { swordsmanship: 20, strength: 15 }, weaponCategory: "Sword" },
        { id: "dual_blades", name: "Twin Daggers", description: "Fast and deadly pair of blades.", type: "Weapon", rarity: "Uncommon", price: 1500, levelRequirement: 12, statBonus: { agility: 8, strength: 4 }, statRequirements: { swordsmanship: 25, agility: 20 }, weaponCategory: "Sword" },
        { id: "katana", name: "Refined Katana", description: "A masterpiece of craftsmanship.", type: "Weapon", rarity: "Rare", price: 5000, levelRequirement: 15, statBonus: { strength: 10, agility: 10 }, statRequirements: { swordsmanship: 40 }, weaponCategory: "Sword" },
        { id: "greatsword", name: "Commander's Greatsword", description: "A weapon of high-ranking officers.", type: "Weapon", rarity: "Rare", price: 8500, levelRequirement: 20, statBonus: { strength: 18, endurance: 5 }, statRequirements: { swordsmanship: 60, strength: 30 }, weaponCategory: "Sword" },
        { id: "cursed_blade", name: "Shadowfang Katana", description: "A blade that whispers to its wielder.", type: "Weapon", rarity: "Epic", price: 45000, levelRequirement: 40, statBonus: { strength: 35, agility: 25, willpower: -10 }, statRequirements: { swordsmanship: 150, willpower: 50 }, weaponCategory: "Sword" },
        { id: "flintlock", name: "Old Flintlock", description: "A basic single-shot pistol.", type: "Weapon", rarity: "Common", price: 100, levelRequirement: 1, statBonus: { gunslinging: 2 }, statRequirements: { gunslinging: 5 }, weaponCategory: "Pistol" },
        { id: "navy_saber", name: "Navy Officer Saber", description: "A finely crafted saber issued to high-ranking Navy officers.", type: "Weapon", rarity: "Rare", price: 15000, levelRequirement: 30, statBonus: { swordsmanship: 25, agility: 10 }, statRequirements: { swordsmanship: 100 }, weaponCategory: "Navy", factionRequirement: "Navy" },
        { id: "navy_carbine", name: "Navy Carbine", description: "A powerful and accurate rifle for elite Marine units.", type: "Weapon", rarity: "Rare", price: 18000, levelRequirement: 35, statBonus: { sniper: 35, perception: 15 }, statRequirements: { sniper: 120 }, weaponCategory: "Navy", factionRequirement: "Navy" },
        { id: "navy_revolver", name: "Navy Revolver", description: "A standard issue marine sidearm.", type: "Weapon", rarity: "Uncommon", price: 800, levelRequirement: 10, statBonus: { gunslinging: 8 }, statRequirements: { gunslinging: 25 }, weaponCategory: "Pistol" },
        { id: "cursed_cutlass", name: "Cursed Cutlass", description: "A wicked blade that hums with dark energy. High damage, but carries a heavy burden.", type: "Weapon", rarity: "Rare", price: 12000, statBonus: { swordsmanship: 35, strength: 10, endurance: -5 }, statRequirements: { swordsmanship: 80 }, levelRequirement: 25, factionRequirement: "Pirate", weaponCategory: "Pirate" },
        { id: "plunderers_pistol", name: "Plunderer's Pistol", description: "A pirate's best friend. Guaranteed to find more loot.", type: "Weapon", rarity: "Uncommon", price: 4500, statBonus: { gunslinging: 12, luck: 10 }, statRequirements: { gunslinging: 30 }, levelRequirement: 15, factionRequirement: "Pirate", weaponCategory: "Pirate" },
        { id: "long_rifle", name: "Hunter's Long Rifle", description: "Accurate at long ranges.", type: "Weapon", rarity: "Uncommon", price: 1500, levelRequirement: 10, statBonus: { sniper: 12 }, statRequirements: { sniper: 30 }, weaponCategory: "Sniper" },
        { id: "scoped_musket", name: "Scoped Musket", description: "Equipped with a primitive but effective lens.", type: "Weapon", rarity: "Rare", price: 10000, levelRequirement: 25, statBonus: { sniper: 30, perception: 5 }, statRequirements: { sniper: 80 }, weaponCategory: "Sniper" },
        { id: "old_boots", name: "Old Boots", description: "Waterlogged but still wearable.", type: "Armor", rarity: "Common", price: 40, levelRequirement: 1, statBonus: { endurance: 2 } },
        { id: "leather_vest", name: "Leather Vest", description: "A simple vest for basic protection.", type: "Armor", rarity: "Common", price: 150, levelRequirement: 3, statBonus: { endurance: 5 } },
        { id: "iron_chestplate", name: "Iron Chestplate", description: "Solid iron protection.", type: "Armor", rarity: "Uncommon", price: 1200, levelRequirement: 10, statBonus: { endurance: 15, willpower: 2 } },
        { id: "iron_helmet", name: "Iron Helmet", description: "Protects your head from blunt trauma.", type: "Armor", rarity: "Uncommon", price: 200, levelRequirement: 8, statBonus: { endurance: 10, willpower: 2 } },
        { id: "navy_uniform", name: "Marine Uniform", description: "The standard issue blues.", type: "Armor", rarity: "Uncommon", price: 2500, levelRequirement: 15, statBonus: { endurance: 20, agility: 5 }, factionRequirement: "Navy" },
        { id: "navy_officer_uniform", name: "Navy Officer Uniform", description: "Commanding presence with reinforced protection.", type: "Armor", rarity: "Rare", price: 20000, levelRequirement: 30, statBonus: { endurance: 45, willpower: 10 }, weaponCategory: "Navy", factionRequirement: "Navy" },
        { id: "justice_cape", name: "Navy Justice Cape", description: "The iconic white cape with 'JUSTICE' emblazoned on the back.", type: "Armor", rarity: "Epic", price: 50000, levelRequirement: 50, statBonus: { endurance: 60, willpower: 30, luck: 10 }, weaponCategory: "Navy", factionRequirement: "Navy" },
        { id: "pirate_cloak", name: "Pirate Cloak", description: "A stylish cloak that helps you blend into the shadows.", type: "Armor", rarity: "Rare", price: 500, levelRequirement: 15, statBonus: { agility: 8, luck: 5 } },
        { id: "smugglers_cloak", name: "Smuggler's Cloak", description: "Enchanted to hide contraband and the wearer. High Agility and Luck.", type: "Armor", rarity: "Rare", price: 8000, levelRequirement: 20, statBonus: { agility: 20, luck: 15, endurance: -2 }, factionRequirement: "Pirate", weaponCategory: "Pirate" },
        { id: "marine_medal_valor", name: "Marine Medal of Valor", description: "A prestigious award for bravery in the line of duty.", type: "Accessory", rarity: "Rare", price: 5000, levelRequirement: 30, statBonus: { willpower: 15, endurance: 10 }, factionRequirement: "Navy", weaponCategory: "Navy" },
        { id: "admirals_whistle", name: "Admiral's Command Whistle", description: "The sound of authority. Boosts the morale of nearby allies.", type: "Accessory", rarity: "Epic", price: 25000, levelRequirement: 50, statBonus: { willpower: 40, perception: 10 }, factionRequirement: "Navy", weaponCategory: "Navy" },
        { id: "sea_captain_coat", name: "Sea Captain's Coat", description: "A heavy coat that commands respect.", type: "Armor", rarity: "Rare", price: 12000, levelRequirement: 25, statBonus: { endurance: 40, willpower: 15, luck: 5 } },
        { id: "reinforced_boots", name: "Reinforced Boots", description: "Sturdy boots for rough terrain.", type: "Armor", rarity: "Uncommon", price: 800, levelRequirement: 8, statBonus: { endurance: 8, agility: 3 } },
        { id: "steel_gloves", name: "Steel Plated Gloves", description: "Protect your hands during combat.", type: "Armor", rarity: "Uncommon", price: 1500, levelRequirement: 12, statBonus: { endurance: 10, strength: 5 } },
        { id: "iron_spear", name: "Iron Spear", description: "A long-reaching thrusting weapon.", type: "Weapon", rarity: "Common", price: 120, levelRequirement: 2, statBonus: { strength: 4 }, statRequirements: { spear: 5 }, weaponCategory: "Spear" },
        { id: "trident_of_the_deep", name: "Trident of the Deep", description: "A mystical trident found in the depths.", type: "Weapon", rarity: "Rare", price: 15000, levelRequirement: 30, statBonus: { strength: 30, willpower: 20 }, statRequirements: { spear: 100 }, weaponCategory: "Spear" },
        { id: "pirate_musket", name: "Blackbeard's Musket", description: "A pirate's favorite ranged weapon.", type: "Weapon", rarity: "Uncommon", price: 2000, levelRequirement: 12, statBonus: { gunslinging: 15 }, statRequirements: { gunslinging: 40 }, weaponCategory: "Pistol" },
        { id: "double_pistol", name: "Double-Barreled Pistol", description: "Two shots are better than one.", type: "Weapon", rarity: "Rare", price: 8000, levelRequirement: 20, statBonus: { gunslinging: 35 }, statRequirements: { gunslinging: 90 }, weaponCategory: "Pistol" },
        { id: "pearl", name: "Pearl", description: "A rare and valuable gem from a Giant Squid.", type: "Miscellaneous", rarity: "Rare", price: 200 },
        // Fishing Rods
        { id: "bamboo_rod", name: "Bamboo Fishing Rod", description: "A simple rod made from flexible bamboo.", type: "Tool", rarity: "Common", price: 150, weaponCategory: "Fishing Rod" },
        { id: "fiberglass_rod", name: "Fiberglass Rod", description: "A sturdy rod with a better grip.", type: "Tool", rarity: "Uncommon", price: 1200, weaponCategory: "Fishing Rod" },
        { id: "carbon_rod", name: "Carbon Fiber Rod", description: "The ultimate fishing tool. Light and strong.", type: "Tool", rarity: "Rare", price: 15000, weaponCategory: "Fishing Rod" },
        // Ingredients
        { id: "salt", name: "Sea Salt", description: "Essential for preserving and seasoning food.", type: "Ingredient", rarity: "Common", price: 10, weaponCategory: "Ingredient" },
        { id: "sugar", name: "Cane Sugar", description: "Adds sweetness to any dish.", type: "Ingredient", rarity: "Common", price: 20, weaponCategory: "Ingredient" },
        { id: "spices", name: "Exotic Spices", description: "Rare spices from across the Grand Line.", type: "Ingredient", rarity: "Uncommon", price: 100, weaponCategory: "Ingredient" },
        { id: "water_jug", name: "Jug of Fresh Water", description: "Crucial for cooking and survival.", type: "Ingredient", rarity: "Common", price: 5, weaponCategory: "Ingredient" },
        { id: "flour", name: "Wheat Flour", description: "The base for many baked goods.", type: "Ingredient", rarity: "Common", price: 30, weaponCategory: "Ingredient" },
        { id: "vegetables", name: "Fresh Vegetables", description: "Crisp greens from a fertile island.", type: "Ingredient", rarity: "Common", price: 40, weaponCategory: "Ingredient" },
        { id: "meat_chunk", name: "Chunk of Meat", description: "Raw meat from a wild creature.", type: "Ingredient", rarity: "Common", price: 60, weaponCategory: "Ingredient" },
        // Cooked Dishes
        { id: "sea_stew", name: "Sea Stew", description: "A warm stew made from fresh fish and vegetables.", type: "Food", rarity: "Common", price: 100, healAmount: 50 },
        { id: "spiced_fish", name: "Spiced Grilled Fish", description: "Perfectly grilled fish with exotic spices.", type: "Food", rarity: "Uncommon", price: 250, healAmount: 120 },
        { id: "pirate_feast", name: "Grand Pirate Feast", description: "A legendary meal that restores massive health.", type: "Food", rarity: "Rare", price: 1000, healAmount: 500 },
        // Bags
        { id: "bag_small", name: "Small Cotton Bag", description: "A simple bag that adds 5 slots to your inventory.", type: "Bag", rarity: "Common", price: 500, storageBonus: 5 },
        { id: "bag_medium", name: "Sturdy Leather Satchel", description: "A well-made satchel that adds 10 slots to your inventory.", type: "Bag", rarity: "Uncommon", price: 5000, storageBonus: 10 },
        { id: "bag_large", name: "Reinforced Sea-Chest Bag", description: "A massive bag for serious collectors. Adds 20 slots.", type: "Bag", rarity: "Rare", price: 50000, storageBonus: 20 },
        { id: "bag_legendary", name: "Infinite Void Pouch", description: "A pouch that seems to defy the laws of space. Adds 50 slots.", type: "Bag", rarity: "Legendary", price: 1000000, storageBonus: 50 },
        // Arena Items
        { id: "gladiator_helmet", name: "Gladiator's Helmet", description: "A heavy steel helmet worn by arena champions.", type: "Armor", rarity: "Rare", price: 15000, levelRequirement: 20, statBonus: { endurance: 15, agility: 5 } },
        { id: "champion_cape", name: "Champion's Cape", description: "A majestic red cape that inspires awe and terror.", type: "Accessory", rarity: "Epic", price: 50000, levelRequirement: 30, statBonus: { willpower: 20, luck: 10 } },
        { id: "arena_medallion", name: "Arena Medallion", description: "A symbol of prowess in the Champion's Colosseum.", type: "Accessory", rarity: "Rare", price: 25000, levelRequirement: 25, statBonus: { strength: 10, agility: 10 } }
    ];

    for (const item of items) {
        const ref = db.collection("gameData").doc("items").collection("all").doc(item.id);
        batch.set(ref, item);
    }

    const lootTables = [
        {
            id: "basic_sea_loot",
            entries: [
                { itemId: "fish_scales", chance: 0.6, minAmount: 1, maxAmount: 3 },
                { itemId: "sea_shell", chance: 0.4, minAmount: 1, maxAmount: 2 },
                { itemId: "rusty_cutlass", chance: 0.05, minAmount: 1, maxAmount: 1 }
            ]
        },
        {
            id: "rare_sea_loot",
            entries: [
                { itemId: "pearl", chance: 0.2, minAmount: 1, maxAmount: 1 },
                { itemId: "old_boots", chance: 0.1, minAmount: 1, maxAmount: 1 },
                { itemId: "reinforced_boots", chance: 0.05, minAmount: 1, maxAmount: 1 },
                { itemId: "fish_scales", chance: 0.5, minAmount: 2, maxAmount: 5 }
            ]
        },
        {
            id: "mythic_sea_loot",
            entries: [
                { itemId: "pearl", chance: 0.5, minAmount: 2, maxAmount: 4 },
                { itemId: "sea_captain_coat", chance: 0.05, minAmount: 1, maxAmount: 1 },
                { itemId: "trident_of_the_deep", chance: 0.02, minAmount: 1, maxAmount: 1 },
                { itemId: "cursed_blade", chance: 0.01, minAmount: 1, maxAmount: 1 }
            ]
        }
    ];

    for (const table of lootTables) {
        const ref = db.collection("gameData").doc("world").collection("lootTables").doc(table.id);
        batch.set(ref, table);
    }

    const techniques = Object.values(STATIC_TECHNIQUES);
    console.log(`Seeding ${techniques.length} techniques from STATIC_TECHNIQUES...`);
    for (const tech of techniques) {
        const ref = db.collection("gameData").doc("skills").collection("techniques").doc(tech.id);
        batch.set(ref, tech);
    }

    const rankUpMissions = [
        // Navy
        { id: "navy_rank_1", title: "Navy Recruit Trial", description: "Complete your basic training to become a Navy Recruit.", energyCost: 10, minLevel: 1, goldReward: 500, xpReward: 100, difficulty: 1, factionRequirement: "Navy", isRankUp: true, targetRank: "Navy Recruit" },
        { id: "navy_rank_2", title: "Petty Officer Exam", description: "Demonstrate your skills to be promoted to Petty Officer.", energyCost: 20, minLevel: 20, goldReward: 2000, xpReward: 500, difficulty: 2, factionRequirement: "Navy", isRankUp: true, targetRank: "Petty Officer" },
        { id: "navy_rank_3", title: "Chief Petty Officer Qualification", description: "Prove your leadership and combat prowess.", energyCost: 30, minLevel: 40, goldReward: 5000, xpReward: 1000, difficulty: 3, factionRequirement: "Navy", isRankUp: true, targetRank: "Chief Petty Officer" },
        { id: "navy_rank_4", title: "Ensign Commission", description: "Ascend to the rank of a commissioned officer.", energyCost: 40, minLevel: 60, goldReward: 10000, xpReward: 2000, difficulty: 4, factionRequirement: "Navy", isRankUp: true, targetRank: "Ensign" },
        { id: "navy_rank_5", title: "Lieutenant Promotion", description: "Face dangerous pirates to earn your stripes.", energyCost: 50, minLevel: 80, goldReward: 20000, xpReward: 4000, difficulty: 5, factionRequirement: "Navy", isRankUp: true, targetRank: "Lieutenant" },
        { id: "navy_rank_6", title: "Commander Selection", description: "Lead a fleet operation to prove you are Commander material.", energyCost: 60, minLevel: 100, goldReward: 50000, xpReward: 8000, difficulty: 6, factionRequirement: "Navy", isRankUp: true, targetRank: "Commander" },
        { id: "navy_rank_7", title: "Captaincy Trial", description: "Take command of your own ship and secure the seas.", energyCost: 70, minLevel: 130, goldReward: 100000, xpReward: 15000, difficulty: 7, factionRequirement: "Navy", isRankUp: true, targetRank: "Captain" },
        { id: "navy_rank_8", title: "Commodore Investiture", description: "Command multiple divisions in a strategic victory.", energyCost: 80, minLevel: 160, goldReward: 250000, xpReward: 30000, difficulty: 8, factionRequirement: "Navy", isRankUp: true, targetRank: "Commodore" },
        { id: "navy_rank_9", title: "Vice Admiral Ascension", description: "Face the terrors of the Grand Line and emerge victorious.", energyCost: 90, minLevel: 200, goldReward: 500000, xpReward: 60000, difficulty: 9, factionRequirement: "Navy", isRankUp: true, targetRank: "Vice Admiral" },
        { id: "navy_rank_10", title: "Admiral's Call", description: "A test of absolute justice. Defeat a legendary pirate.", energyCost: 100, minLevel: 250, goldReward: 1000000, xpReward: 120000, difficulty: 10, factionRequirement: "Navy", isRankUp: true, targetRank: "Admiral" },
        { id: "navy_rank_11", title: "Fleet Admiral's Zenith", description: "Reach the pinnacle of Navy leadership.", energyCost: 100, minLevel: 300, goldReward: 5000000, xpReward: 500000, difficulty: 12, factionRequirement: "Navy", isRankUp: true, targetRank: "Fleet Admiral" },
        // Pirates
        { id: "pirate_rank_1", title: "Rogue Sailor Initiation", description: "Prove your worth to the pirate code.", energyCost: 10, minLevel: 1, goldReward: 500, xpReward: 100, difficulty: 1, factionRequirement: "Pirate", isRankUp: true, targetRank: "Rogue Sailor" },
        { id: "pirate_rank_2", title: "Skirmisher's Greed", description: "Plunder enough gold to be known as a Skirmisher.", energyCost: 20, minLevel: 20, goldReward: 2000, xpReward: 500, difficulty: 2, factionRequirement: "Pirate", isRankUp: true, targetRank: "Skirmisher" },
        { id: "pirate_rank_3", title: "Deckhand Mastery", description: "Work your way up the pirate hierarchy.", energyCost: 30, minLevel: 40, goldReward: 5000, xpReward: 1000, difficulty: 3, factionRequirement: "Pirate", isRankUp: true, targetRank: "Deckhand" },
        { id: "pirate_rank_4", title: "Swashbuckler's Duel", description: "Win a high-stakes duel in a tavern brawl.", energyCost: 40, minLevel: 60, goldReward: 10000, xpReward: 2000, difficulty: 4, factionRequirement: "Pirate", isRankUp: true, targetRank: "Swashbuckler" },
        { id: "pirate_rank_5", title: "Marauder's Raid", description: "Lead a successful raid on a merchant convoy.", energyCost: 50, minLevel: 80, goldReward: 20000, xpReward: 4000, difficulty: 5, factionRequirement: "Pirate", isRankUp: true, targetRank: "Marauder" },
        { id: "pirate_rank_6", title: "Buccaneer's Infamy", description: "Build your reputation as a feared Buccaneer.", energyCost: 60, minLevel: 100, goldReward: 50000, xpReward: 8000, difficulty: 6, factionRequirement: "Pirate", isRankUp: true, targetRank: "Buccaneer" },
        { id: "pirate_rank_7", title: "Corsair's Ambition", description: "Command a crew of outlaws to claim your own territory.", energyCost: 70, minLevel: 130, goldReward: 100000, xpReward: 15000, difficulty: 7, factionRequirement: "Pirate", isRankUp: true, targetRank: "Corsair" },
        { id: "pirate_rank_8", title: "Dread Pirate Legend", description: "Let your name strike fear into the hearts of all.", energyCost: 80, minLevel: 160, goldReward: 250000, xpReward: 30000, difficulty: 8, factionRequirement: "Pirate", isRankUp: true, targetRank: "Dread Pirate" },
        { id: "pirate_rank_9", title: "Pirate Lord's Council", description: "Prove your strength to the existing Pirate Lords.", energyCost: 90, minLevel: 200, goldReward: 500000, xpReward: 60000, difficulty: 9, factionRequirement: "Pirate", isRankUp: true, targetRank: "Pirate Lord" },
        { id: "pirate_rank_10", title: "Emperor's Challenge", description: "Defeat a high-ranking Marine to be recognized as Yonko.", energyCost: 100, minLevel: 250, goldReward: 1000000, xpReward: 120000, difficulty: 10, factionRequirement: "Pirate", isRankUp: true, targetRank: "Yonko" },
        { id: "pirate_rank_11", title: "Pirate King's Legacy", description: "Find the ultimate treasure and claim the title of Pirate King.", energyCost: 100, minLevel: 300, goldReward: 5000000, xpReward: 500000, difficulty: 12, factionRequirement: "Pirate", isRankUp: true, targetRank: "Pirate King" }
    ];

    for (const m of rankUpMissions) {
        const ref = db.collection("gameData").doc("world").collection("missions").doc(m.id);
        batch.set(ref, m);
    }

    await batch.commit();
    const msg = `SUCCESS_V13: 17 islands, items, and ${techniques.length} techniques seeded.`;
    console.log(msg);
    return { success: true, message: msg };
});

export const sendMessage = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { message, channelId } = data;
    const userId = context.auth.uid;

    const playerSnap = await db.collection("players").doc(userId).get();
    if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
    const player = playerSnap.data() as any;

    if (player.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");
    if (player.mutedUntil && player.mutedUntil > Date.now()) {
        throw new functions.https.HttpsError("permission-denied", `You are muted until ${new Date(player.mutedUntil).toLocaleString()}. Reason: ${player.muteReason || "None"}`);
    }

    const chatRef = db.collection("chat").doc();
    await chatRef.set({
        senderId: userId,
        senderName: player.name,
        message: message,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        channelId: channelId || "global"
    });

    return { success: true };
});

export async function checkAdmin(context: functions.https.CallableContext) {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    // Check if user has admin claim
    if (context.auth.token.admin) return;

    // Fallback: Hardcoded check for "Sedna" as the primary admin
    const userId = context.auth.uid;
    const playerSnap = await db.collection("players").doc(userId).get();
    const adminNames = ["sedna", "von"];
    if (playerSnap.exists && adminNames.includes((playerSnap.data() as any).nameLower)) {
        // Grant admin claim permanently for this user
        await admin.auth().setCustomUserClaims(userId, { admin: true });
        // Sync to Firestore immediately
        await db.collection("players").doc(userId).update({ isAdmin: true });
        return;
    }

    throw new functions.https.HttpsError("permission-denied", "User is not an admin.");
}

export const adminAdjustGold = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { userId, amount, reason } = data;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        transaction.update(playerRef, { gold: admin.firestore.FieldValue.increment(amount) });
        recordLog(transaction, userId, "AdminAdjustGold", reason, amount, 0);
        return { success: true };
    });
});

export const adminTeleportPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { userId, location } = data;
    await db.collection("players").doc(userId).update({ currentLocation: location, travelState: null });
    return { success: true };
});

export const adminSendAnnouncement = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { message } = data;
    await db.collection("announcements").add({
        message,
        timestamp: Date.now(),
        authorId: context.auth.uid
    });
    return { success: true };
});

export const adminSendSystemMail = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { targetName, subject, body, rewards } = data;

    const targetSnap = await db.collection("players").where("nameLower", "==", targetName.toLowerCase()).get();
    if (targetSnap.empty) throw new functions.https.HttpsError("not-found", "Target player not found.");

    const targetId = targetSnap.docs[0].id;
    const mailRef = db.collection("players").doc(targetId).collection("mail").doc();

    await mailRef.set({
        id: mailRef.id,
        senderName: "System",
        subject: subject || "System Update",
        body: body || "The latest updates have been applied to your account.",
        timestamp: Date.now(),
        isRead: false,
        claimed: false,
        rewards: rewards || null
    });

    return { success: true };
});

export const adminMutePlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { userId, reason, durationHours } = data;
    const muteUntil = Date.now() + (durationHours * 3600000);

    await db.collection("players").doc(userId).update({
        mutedUntil: muteUntil,
        muteReason: reason
    });
    return { success: true };
});

export const adminBanPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    await checkAdmin(context);

    const { userId, reason } = data;

    await db.collection("players").doc(userId).update({
        isBanned: true,
        banReason: reason
    });
    return { success: true };
});

export const adminGrantTestItems = functions.https.onCall(async (data, context) => {
    console.log("adminGrantTestItems called by:", context.auth?.uid);
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    await checkAdmin(context);
    console.log("Admin check passed for:", context.auth.uid);

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) {
            console.log("Character not found for UID:", userId);
            throw new functions.https.HttpsError("not-found", "Character not found.");
        }

        const character = snapshot.data() as any;
        console.log("Found character:", character.name);
        const currentInventory = character.inventory || [];

        const testItems = [];
        const tiers = ["S", "S", "SS", "SS", "SSS", "SSS"];
        for (let i = 0; i < tiers.length; i++) {
            const tier = tiers[i];
            testItems.push({
                id: `test_artifact_${tier}_${Date.now()}_${i}`,
                name: `${tier} Tier Artifact (Test)`,
                description: `A mysterious artifact that contains a random ${tier} tier Mythic Art. Use it to awaken its power.`,
                type: "Artifact",
                rarity: getRarityForTier(tier),
                price: getPriceForTier(tier),
                mythicTier: tier,
                levelRequirement: 1
            });
        }

        console.log("Granting 6 artifacts to:", character.name);
        const maxCapacity = calculateMaxCapacity(character);
        if (currentInventory.length + testItems.length > maxCapacity) {
            throw new functions.https.HttpsError("failed-precondition", "Inventory full.");
        }
        transaction.update(playerRef, {
            inventory: [...currentInventory, ...testItems],
            gold: 900000000
        });

        return { success: true };
    });
});

// --- Social Functions ---

export const explicitLogout = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const userId = context.auth.uid;
    await db.collection("players").doc(userId).update({ isOnline: false });
    return { success: true };
});

export const sendFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const senderId = context.auth.uid;

    if (senderId === targetId) throw new functions.https.HttpsError("invalid-argument", "Cannot add yourself.");

    const targetSnap = await db.collection("players").doc(targetId).get();
    if (!targetSnap.exists) throw new functions.https.HttpsError("not-found", "Target player not found.");
    const target = targetSnap.data() as any;

    if (target.blocked && target.blocked.includes(senderId)) {
        throw new functions.https.HttpsError("permission-denied", "You have been blocked by this player.");
    }

    const requestRef = db.collection("friendRequests").doc(`${senderId}_${targetId}`);
    const existing = await requestRef.get();
    if (existing.exists) throw new functions.https.HttpsError("already-exists", "Request already sent.");

    await requestRef.set({
        senderId,
        receiverId: targetId,
        status: "pending",
        timestamp: Date.now()
    });

    return { success: true };
});

export const acceptFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { senderId } = data;
    const receiverId = context.auth.uid;

    const requestRef = db.collection("friendRequests").doc(`${senderId}_${receiverId}`);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(requestRef);
        if (!snapshot.exists || snapshot.data()?.status !== "pending") {
            throw new functions.https.HttpsError("not-found", "Request not found.");
        }

        transaction.update(requestRef, { status: "accepted" });
        transaction.update(db.collection("players").doc(receiverId), {
            friends: admin.firestore.FieldValue.arrayUnion(senderId)
        });
        transaction.update(db.collection("players").doc(senderId), {
            friends: admin.firestore.FieldValue.arrayUnion(receiverId)
        });

        return { success: true };
    });
});

export const declineFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { senderId } = data;
    const receiverId = context.auth.uid;
    const requestRef = db.collection("friendRequests").doc(`${senderId}_${receiverId}`);

    await requestRef.update({ status: "declined" });
    return { success: true };
});

export const removeFriend = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { friendId } = data;
    const userId = context.auth.uid;

    await db.runTransaction(async (transaction) => {
        transaction.update(db.collection("players").doc(userId), {
            friends: admin.firestore.FieldValue.arrayRemove(friendId)
        });
        transaction.update(db.collection("players").doc(friendId), {
            friends: admin.firestore.FieldValue.arrayRemove(userId)
        });
    });

    return { success: true };
});

export const blockPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const userId = context.auth.uid;

    await db.collection("players").doc(userId).update({
        blocked: admin.firestore.FieldValue.arrayUnion(targetId)
    });

    return { success: true };
});

export const unblockPlayer = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const userId = context.auth.uid;

    await db.collection("players").doc(userId).update({
        blocked: admin.firestore.FieldValue.arrayRemove(targetId)
    });

    return { success: true };
});

// --- Inventory & Equipment ---

export const equipItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId, slot } = data; // slot: Weapon, Armor, Accessory
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "change equipment", { blockBusy: true, blockHealing: true });

        const allowedSlots = ["Weapon", "Armor", "Accessory", "Bag", "Helmet", "Boots", "Gloves"];
        if (!allowedSlots.includes(slot)) throw new functions.https.HttpsError("invalid-argument", "Invalid equipment slot.");

        const inventory = character.inventory || [];
        const item = inventory.find((i: any) => i.id === itemId);

        if (!item) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");
        if (item.type !== slot) throw new functions.https.HttpsError("invalid-argument", `Item type ${item.type} does not match slot ${slot}.`);

        // Level Requirement Check
        if (character.level < (item.levelRequirement || 1)) {
            throw new functions.https.HttpsError("failed-precondition", "Level too low to equip this item.");
        }

        // Faction Requirement Check
        if (item.factionRequirement && item.factionRequirement !== "Neutral") {
            if (character.faction !== item.factionRequirement) {
                throw new functions.https.HttpsError("failed-precondition", `This item is exclusive to the ${item.factionRequirement} faction.`);
            }
        }

        // Stat Requirement Check
        if (item.statRequirements) {
            const charStats = character.stats || {};
            const reqs = item.statRequirements;
            for (const [stat, value] of Object.entries(reqs)) {
                if ((charStats[stat] || 0) < (value as number)) {
                    throw new functions.https.HttpsError("failed-precondition", `Insufficient ${stat}. Required: ${value}`);
                }
            }
        }

        const equipment = character.equipment || {};
        equipment[slot] = item;

        const updates: any = {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            equipment
        };

        if (item.type === "Bag") {
            updates.inventoryCapacity = calculateMaxCapacity({ ...character, equipment });
        }

        transaction.update(playerRef, updates);
        return { success: true };
    });
});

export const unequipItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { slot } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "change equipment", { blockBusy: true, blockHealing: true });

        const equipment = character.equipment || {};
        const unequippedItem = equipment[slot];
        delete equipment[slot];

        const updates: any = {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            equipment
        };

        if (unequippedItem && unequippedItem.type === "Bag") {
            updates.inventoryCapacity = calculateMaxCapacity({ ...character, equipment });
        }

        transaction.update(playerRef, updates);
        return { success: true };
    });
});

export const purchaseItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId, shopId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const itemRef = db.collection("gameData").doc("items").collection("all").doc(itemId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, itemSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(itemRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!itemSnap.exists) throw new functions.https.HttpsError("not-found", "Item not found.");

        let character = playerSnap.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "purchase items", { blockBusy: true, blockHealing: true });

        const item = itemSnap.data() as any;

        if (item.type === "Artifact") {
            throw new functions.https.HttpsError("failed-precondition", "Artifacts cannot be purchased from the market.");
        }

        // Validate location has the correct market
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data() as any;
        const marketAction = location?.actions?.find((a: any) =>
            (a.type === "Market" || a.type === "BlackMarket") && (!shopId || a.label === shopId || a.parameter === shopId)
        );

        if (!marketAction) {
            throw new functions.https.HttpsError("failed-precondition", "The specific shop is not available at your current location.");
        }

        // shopId validation (simplified for now, but enforces the concept)
        if (shopId === "BlackMarket" && marketAction.type !== "BlackMarket") {
             throw new functions.https.HttpsError("failed-precondition", "This item is only available in a Black Market.");
        }

        // Faction Requirement Check for Purchase
        if (item.factionRequirement && item.factionRequirement !== "Neutral") {
            if (character.faction !== item.factionRequirement) {
                throw new functions.https.HttpsError("failed-precondition", `The ${item.factionRequirement} will only sell this to their own members.`);
            }
        }

        // Currency Check
        let currencyType = "gold";
        let currencyLabel = "Gold";
        if (marketAction.parameter === "Navy") {
            currencyType = "justicePoints";
            currencyLabel = "Justice Points";
        } else if (marketAction.parameter === "Pirate") {
            currencyType = "pirateReputation";
            currencyLabel = "Pirate Reputation";
        }

        // --- Faction Control Economic Impact ---
        let price = item.price;
        if (currencyType === "gold") {
            if (location.controlledBy === character.faction && character.faction !== "Neutral") {
                price = Math.floor(price * 0.8); // 20% discount
            } else if (location.controlledBy !== "Neutral" && location.controlledBy !== character.faction) {
                price = Math.floor(price * 1.1); // 10% occupant tax
            }
        }

        if ((character[currencyType] || 0) < price) {
            throw new functions.https.HttpsError("failed-precondition", `Not enough ${currencyLabel}.`);
        }

        const inventory = character.inventory || [];
        const maxCapacity = calculateMaxCapacity(character);
        if (inventory.length >= maxCapacity) {
            throw new functions.https.HttpsError("failed-precondition", "Inventory is full.");
        }

        const newInventory = [...inventory, { ...item, id: `${item.id}_${Date.now()}` }];

        const updates: any = {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            inventory: newInventory
        };
        updates[currencyType] = (character[currencyType] || 0) - price;

        transaction.update(playerRef, updates);

        recordLog(transaction, userId, "PurchaseItem", `Purchased ${item.name} for ${price} ${currencyLabel}`, currencyType === "gold" ? -price : 0, 0);
        return { success: true };
    });
});

export const sellItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "sell items", { blockBusy: true, blockHealing: true });

        // Protection: Check if item is equipped
        const isEquipped = Object.values(character.equipment || {}).some((i: any) => i && i.id === itemId);
        if (isEquipped) {
            throw new functions.https.HttpsError("failed-precondition", "Cannot sell equipped items. Unequip it first.");
        }

        const inventory = character.inventory || [];
        const itemIndex = inventory.findIndex((i: any) => i.id === itemId);

        if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");

        const item = inventory[itemIndex];
        const sellPrice = Math.floor(item.price * 0.5);

        inventory.splice(itemIndex, 1);

        transaction.update(playerRef, {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            gold: admin.firestore.FieldValue.increment(sellPrice),
            inventory
        });

        recordLog(transaction, userId, "SellItem", `Sold ${item.name}`, sellPrice, 0);
        return { success: true, goldGained: sellPrice };
    });
});

export const useItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "use items", { blockBusy: true, blockHealing: true });

        const inventory = character.inventory || [];
        const itemIndex = inventory.findIndex((i: any) => i.id === itemId);

        if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");

        const item = inventory[itemIndex];

        if (item.type === "Artifact") {
            const tier = item.mythicTier;
            const arts = MYTHIC_ARTS[tier];
            if (!arts || arts.length === 0) throw new functions.https.HttpsError("not-found", "Mythic data not found.");

            const mythic = arts[Math.floor(Math.random() * arts.length)];

            const oldArt = character.mythicArt;
            const stats = character.stats || {};
            const learnedTechniques = character.learnedTechniques || [];

            // Revert old stats if exists
            if (oldArt && oldArt.bonusStats) {
                for (const [stat, value] of Object.entries(oldArt.bonusStats)) {
                    if (stats[stat] !== undefined) stats[stat] -= (value as number);
                }
            }

            // Apply new stats
            for (const [stat, value] of Object.entries(mythic.stats)) {
                if (stats[stat] !== undefined) stats[stat] += (value as number);
            }

            // Reset to baseline skills (bash) + new art techniques
            // This ensures players don't keep old skills if the logic fails
            const baselineSkills = ["bash"];
            const newTechniques = [...new Set([...baselineSkills, ...(mythic.techniques || [])])];

            const newMythicArt = {
                name: mythic.name,
                tier: tier,
                description: mythic.description,
                bonusStats: mythic.stats,
                skillMultiplier: mythic.skillMultiplier || 1.0,
                multipliedSkill: mythic.multipliedSkill || "Swordsmanship",
                techniques: mythic.techniques || [],
                hugeBuffType: mythic.hugeBuffType || null,
                hugeBuffValue: mythic.hugeBuffValue || 0,
                debuffPercentage: mythic.debuffPercentage || 0,
                energyRegainMultiplier: mythic.energyRegainMultiplier || 1.0,
                weakAgainst: mythic.weakAgainst || [],
                travelTimeMultiplier: mythic.travelTimeMultiplier || 1.0,
                canLearnNonCombatSkills: mythic.canLearnNonCombatSkills !== undefined ? mythic.canLearnNonCombatSkills : true,
                restrictedSkillTypes: mythic.restrictedSkillTypes || [],
                elements: mythic.elements || [],
                elementalWeaknesses: mythic.elementalWeaknesses || []
            };

            inventory.splice(itemIndex, 1);

            transaction.update(playerRef, {
                hp: character.hp,
                energy: character.energy,
                energyUpdatedAt: character.energyUpdatedAt,
                healingState: character.healingState,
                mythicArt: newMythicArt,
                stats,
                learnedTechniques: newTechniques,
                inventory
            });

            recordLog(transaction, userId, "UseArtifact", `Used ${item.name} and gained ${mythic.name}`, 0, 0);
            return { success: true, gainedArt: mythic.name };
        }

        if (item.type !== "Consumable") throw new functions.https.HttpsError("invalid-argument", "Item is not consumable.");

        let playerHp = character.hp;
        const healAmount = item.healAmount || 30;
        playerHp = Math.min(character.maxHp, playerHp + healAmount);

        let mpMsg = "";
        if (character.mythicArt) {
            const manaHeal = item.healAmount || 30;
            character.mythicMana = Math.min(character.maxMythicMana || 100, (character.mythicMana || 0) + manaHeal);
            character.mythicManaUpdatedAt = Date.now();
            mpMsg = ` and ${manaHeal} MP`;
        }

        inventory.splice(itemIndex, 1);

        transaction.update(playerRef, {
            hp: playerHp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            mythicMana: character.mythicMana || 0,
            mythicManaUpdatedAt: character.mythicManaUpdatedAt || Date.now(),
            healingState: character.healingState,
            inventory
        });

        recordLog(transaction, userId, "UseItem", `Used ${item.name}${mpMsg}`, 0, 0);
        return { success: true, newHp: playerHp, message: `Used ${item.name}${mpMsg}` };
    });
});

// --- Auction House ---

export const listAuctionItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId, price } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    if (!price || !Number.isSafeInteger(price) || price <= 0) throw new functions.https.HttpsError("invalid-argument", "Price must be a positive safe integer.");
    if (price > MAX_AUCTION_PRICE) throw new functions.https.HttpsError("invalid-argument", `Price exceeds maximum allowed (${MAX_AUCTION_PRICE}).`);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);
        assertCanPerformAction(character, "list items for auction", { blockBusy: true, blockHealing: true });

        const inventory = character.inventory || [];
        const itemIndex = inventory.findIndex((i: any) => i.id === itemId);

        if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");

        // Check if equipped (improved to handle duplicates)
        const equippedCount = Object.values(character.equipment || {}).filter((i: any) => i && i.id === itemId).length;
        const totalCount = inventory.filter((i: any) => i.id === itemId).length;
        if (totalCount <= equippedCount) throw new functions.https.HttpsError("failed-precondition", "All instances of this item are currently equipped.");

        const item = inventory[itemIndex];
        inventory.splice(itemIndex, 1);

        const listingRef = db.collection("auctions").doc();
        const listing = {
            id: listingRef.id,
            sellerId: userId,
            sellerName: character.name,
            item: item,
            price: price,
            timestamp: Date.now()
        };

        transaction.set(listingRef, listing);
        transaction.update(playerRef, {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            inventory
        });

        recordLog(transaction, userId, "AuctionList", `Listed ${item.name} for ${price} Gold`, 0, 0);
        return { success: true, listingId: listingRef.id };
    });
});

export const buyAuctionItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { listingId } = data;
    const buyerId = context.auth.uid;
    const buyerRef = db.collection("players").doc(buyerId);
    const listingRef = db.collection("auctions").doc(listingId);

    return db.runTransaction(async (transaction) => {
        const [buyerSnap, listingSnap] = await Promise.all([
            transaction.get(buyerRef),
            transaction.get(listingRef)
        ]);

        if (!buyerSnap.exists) throw new functions.https.HttpsError("not-found", "Buyer not found.");
        if (!listingSnap.exists) throw new functions.https.HttpsError("not-found", "Listing not found.");

        let buyer = buyerSnap.data() as any;
        buyer = processCharacterUpdates(buyer);
        const listing = listingSnap.data() as any;

        assertCanPerformAction(buyer, "buy items from auction", { blockBusy: true, blockHealing: true });

        if (buyerId === listing.sellerId) throw new functions.https.HttpsError("failed-precondition", "You cannot buy your own item.");
        if (buyer.gold < listing.price) throw new functions.https.HttpsError("failed-precondition", "Not enough gold.");

        const inventory = buyer.inventory || [];
        const maxCapacity = calculateMaxCapacity(buyer);
        if (inventory.length >= maxCapacity) {
            throw new functions.https.HttpsError("failed-precondition", "Inventory is full.");
        }

        // Transfer Item to Buyer
        inventory.push(listing.item);
        transaction.update(buyerRef, {
            hp: buyer.hp,
            energy: buyer.energy,
            energyUpdatedAt: buyer.energyUpdatedAt,
            healingState: buyer.healingState,
            gold: admin.firestore.FieldValue.increment(-listing.price),
            inventory
        });

        // Send Gold to Seller via Mail
        const sellerMailRef = db.collection("players").doc(listing.sellerId).collection("mail").doc();
        transaction.set(sellerMailRef, {
            id: sellerMailRef.id,
            senderName: "Auction House",
            subject: "Item Sold!",
            body: `Your ${listing.item.name} has been sold for ${listing.price} Gold!`,
            timestamp: Date.now(),
            isRead: false,
            claimed: false,
            rewards: { gold: listing.price }
        });

        transaction.delete(listingRef);

        recordLog(transaction, buyerId, "AuctionBuy", `Bought ${listing.item.name} for ${listing.price} Gold`, -listing.price, 0);
        return { success: true };
    });
});

export const cancelAuctionListing = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { listingId } = data;
    const userId = context.auth.uid;
    const listingRef = db.collection("auctions").doc(listingId);

    return db.runTransaction(async (transaction) => {
        const listingSnap = await transaction.get(listingRef);
        if (!listingSnap.exists) throw new functions.https.HttpsError("not-found", "Listing not found.");

        const listing = listingSnap.data() as any;
        if (listing.sellerId !== userId) throw new functions.https.HttpsError("permission-denied", "Not your listing.");

        const playerRef = db.collection("players").doc(userId);
        const playerSnap = await transaction.get(playerRef);
        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        let character = playerSnap.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "cancel auction listing", { blockBusy: true, blockHealing: true });

        const inventory = character.inventory || [];
        const maxCapacity = calculateMaxCapacity(character);
        if (inventory.length >= maxCapacity) {
             throw new functions.https.HttpsError("failed-precondition", "Inventory is full. Cannot return item.");
        }

        inventory.push(listing.item);
        transaction.update(playerRef, {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            inventory
        });
        transaction.delete(listingRef);

        recordLog(transaction, userId, "AuctionCancel", `Canceled listing for ${listing.item.name}`, 0, 0);
        return { success: true };
    });
});

// --- Crew System Completion ---

export const leaveCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;
        const crewId = character.crewId;
        if (!crewId) throw new functions.https.HttpsError("failed-precondition", "Player is not in a crew.");

        const crewRef = db.collection("crews").doc(crewId);
        const crewSnap = await transaction.get(crewRef);
        if (!crewSnap.exists) throw new functions.https.HttpsError("not-found", "Crew not found.");

        const crew = crewSnap.data() as any;
        if (crew.captainId === userId) {
            throw new functions.https.HttpsError("failed-precondition", "Captain cannot leave. Disband or promote someone else first.");
        }

        transaction.update(crewRef, {
            members: admin.firestore.FieldValue.arrayRemove(userId),
            totalBounty: admin.firestore.FieldValue.increment(-character.bounty)
        });
        transaction.update(playerRef, {
            hp: character.hp,
            energy: character.energy,
            energyUpdatedAt: character.energyUpdatedAt,
            healingState: character.healingState,
            crewId: null
        });

        return { success: true };
    });
});

export const inviteToCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const senderId = context.auth.uid;

    if (senderId === targetId) throw new functions.https.HttpsError("invalid-argument", "You cannot invite yourself to your own crew.");

    const senderRef = db.collection("players").doc(senderId);
    const targetRef = db.collection("players").doc(targetId);

    return db.runTransaction(async (transaction) => {
        const [senderSnap, targetSnap] = await Promise.all([
            transaction.get(senderRef),
            transaction.get(targetRef)
        ]);

        if (!senderSnap.exists || !targetSnap.exists) throw new functions.https.HttpsError("not-found", "Player not found.");
        const sender = senderSnap.data() as any;
        const target = targetSnap.data() as any;

        if (!sender.crewId) throw new functions.https.HttpsError("failed-precondition", "You are not in a crew.");
        if (target.crewId) throw new functions.https.HttpsError("failed-precondition", "Target is already in a crew.");

        const crewRef = db.collection("crews").doc(sender.crewId);
        const crewSnap = await transaction.get(crewRef);
        if (!crewSnap.exists) throw new functions.https.HttpsError("not-found", "Crew not found.");
        const crew = crewSnap.data() as any;

        if (crew.captainId !== senderId && (!crew.roles || crew.roles[senderId] !== "Officer")) {
            throw new functions.https.HttpsError("permission-denied", "You do not have permission to invite players.");
        }

        const inviteRef = db.collection("crewInvites").doc(`${sender.crewId}_${targetId}`);
        transaction.set(inviteRef, {
            crewId: sender.crewId,
            crewName: crew.name,
            senderId,
            targetId,
            status: "pending",
            timestamp: Date.now()
        });

        return { success: true };
    });
});

export const respondToInvite = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { crewId, accept } = data;
    const userId = context.auth.uid;
    const inviteRef = db.collection("crewInvites").doc(`${crewId}_${userId}`);

    return db.runTransaction(async (transaction) => {
        const inviteSnap = await transaction.get(inviteRef);
        if (!inviteSnap.exists || inviteSnap.data()?.status !== "pending") {
            throw new functions.https.HttpsError("not-found", "Invite not found.");
        }

        if (!accept) {
            transaction.update(inviteRef, { status: "rejected" });
            return { success: true, accepted: false };
        }

        const playerRef = db.collection("players").doc(userId);
        const crewRef = db.collection("crews").doc(crewId);
        const [playerSnap, crewSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(crewRef)
        ]);

        executeCrewJoin(transaction, userId, playerRef, playerSnap, crewRef, crewSnap, inviteRef, crewId);

        return { success: true, accepted: true };
    });
});

export const markMailAsRead = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    const mailRef = db.collection("players").doc(userId).collection("mail").doc(mailId);
    const mailSnap = await mailRef.get();
    if (!mailSnap.exists) throw new functions.https.HttpsError("not-found", "Mail not found.");
    await mailRef.update({ isRead: true });
    return { success: true };
});

export const deleteMail = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    const mailRef = db.collection("players").doc(userId).collection("mail").doc(mailId);

    return db.runTransaction(async (transaction) => {
        const mailSnap = await transaction.get(mailRef);
        if (!mailSnap.exists) throw new functions.https.HttpsError("not-found", "Mail not found.");
        const mail = mailSnap.data() as any;

        if (mail.rewards && !mail.claimed) {
            throw new functions.https.HttpsError("failed-precondition", "Cannot delete mail with unclaimed rewards.");
        }

        transaction.delete(mailRef);
        return { success: true };
    });
});

export const claimMailRewards = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const mailRef = playerRef.collection("mail").doc(mailId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, mailSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(mailRef)
        ]);

        if (!playerSnap.exists || !mailSnap.exists) throw new functions.https.HttpsError("not-found", "Not found.");
        const mail = mailSnap.data() as any;
        if (mail.claimed) throw new functions.https.HttpsError("failed-precondition", "Rewards already claimed.");
        if (!mail.rewards) throw new functions.https.HttpsError("failed-precondition", "No rewards to claim.");

        const character = playerSnap.data() as any;
        const rewards = mail.rewards;

        let updatedChar = {
            ...character,
            gold: character.gold + (rewards.gold || 0),
            xp: character.xp + (rewards.xp || 0),
        };

        if (rewards.items) {
             const inventory = character.inventory || [];
             const maxCapacity = calculateMaxCapacity(character);
             if (inventory.length + rewards.items.length > maxCapacity) {
                  throw new functions.https.HttpsError("resource-exhausted", "Inventory full.");
             }
             updatedChar.inventory = [...inventory, ...rewards.items.map((i: any) => ({ ...i, id: `${i.id}_${Date.now()}_${Math.random()}` }))];
        }

        updatedChar = checkLevelUp(updatedChar);

        transaction.update(playerRef, updatedChar);
        transaction.update(mailRef, { claimed: true, isRead: true });

        return { success: true };
    });
});

export const promoteMember = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId, rank } = data; // rank: Officer
    const captainId = context.auth.uid;

    const playerSnap = await db.collection("players").doc(captainId).get();
    const captain = playerSnap.data() as any;
    const crewRef = db.collection("crews").doc(captain.crewId);

    return db.runTransaction(async (transaction) => {
        const crewSnap = await transaction.get(crewRef);
        const crew = crewSnap.data() as any;

        if (crew.captainId !== captainId) throw new functions.https.HttpsError("permission-denied", "Only Captain can promote.");

        const roles = crew.roles || {};
        roles[targetId] = rank;
        transaction.update(crewRef, { roles });

        return { success: true };
    });
});

export const startFishing = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "fish", { blockBusy: true, blockHealing: true });

        if (character.mythicArt?.tier === "Z") {
            throw new functions.https.HttpsError("failed-precondition", "Your Mythic Art is too powerful for such a mundane activity as fishing.");
        }

        const inventory = character.inventory || [];
        const hasRod = inventory.some((it: any) => it.id.startsWith("rod_") || (it.type === "Tool" && it.name.includes("Rod")));
        if (!hasRod) {
             throw new functions.https.HttpsError("failed-precondition", "You need a fishing rod to fish!");
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < 5) throw new functions.https.HttpsError("failed-precondition", "Not enough energy (5 required).");

        const fishId = determineCaughtFish(character.level);

        transaction.update(playerRef, {
            hp: character.hp,
            energy: energy - 5,
            energyUpdatedAt,
            healingState: character.healingState,
            fishingState: {
                startTime: Date.now(),
                expectedFishId: fishId
            }
        });

        return { success: true, fishId };
    });
});

export const catchFish = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        const fishing = character.fishingState;
        if (!fishing) throw new functions.https.HttpsError("failed-precondition", "No active fishing session.");

        if (Date.now() - fishing.startTime < 2000) {
            throw new functions.https.HttpsError("failed-precondition", "You reeled in too quickly!");
        }

        const inventory = character.inventory || [];
        const maxCapacity = calculateMaxCapacity(character);
        if (inventory.length >= maxCapacity) {
            throw new functions.https.HttpsError("failed-precondition", "Inventory is full.");
        }

        const fishId = fishing.expectedFishId;
        if (!fishId || !FISH_TYPES[fishId]) {
            throw new functions.https.HttpsError("internal", "Corrupted fishing state: missing fish ID.");
        }
        const caught = FISH_TYPES[fishId];
        const fishItem = {
            id: `fish_${fishId}_${Date.now()}`,
            name: caught.name,
            type: "Fish",
            price: caught.price,
            healAmount: caught.healAmount,
            rarity: (fishId === "kraken_tentacle") ? "Legendary" : (fishId === "swordfish" ? "Rare" : "Common")
        };

        transaction.update(playerRef, {
            inventory: admin.firestore.FieldValue.arrayUnion(fishItem),
            "professionStats.fishing": admin.firestore.FieldValue.increment(1),
            xp: admin.firestore.FieldValue.increment(5),
            fishingState: null
        });

        recordLog(transaction, userId, "CatchFish", `Caught a ${caught.name}`, 0, 5);

        return { success: true, fish: caught.name };
    });
});

export const cook = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { recipeId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "cook", { blockBusy: true, blockHealing: true });

        const recipe = RECIPES[recipeId];
        if (!recipe) throw new functions.https.HttpsError("not-found", "Recipe not found.");

        const cookingLevel = character.professionStats?.cooking || 0;
        if (cookingLevel < recipe.levelRequirement) {
            throw new functions.https.HttpsError("failed-precondition", `Your cooking level (${cookingLevel}) is too low. Required: ${recipe.levelRequirement}`);
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < 5) throw new functions.https.HttpsError("failed-precondition", "Not enough energy (5 required).");

        const inventory = [...(character.inventory || [])];

        // Verify and consume ingredients
        for (const req of recipe.ingredients) {
            let count = 0;
            const indicesToRemove: number[] = [];
            for (let i = inventory.length - 1; i >= 0; i--) {
                const item = inventory[i];
                // Check base ID (e.g., 'salt' matches 'salt_123')
                const matchesId = item.id === req.itemId || item.id.startsWith(`${req.itemId}_`);
                const matchesType = req.type ? item.type === req.type : true;

                if (matchesId && matchesType) {
                    indicesToRemove.push(i);
                    count++;
                    if (count === req.quantity) break;
                }
            }

            if (count < req.quantity) {
                throw new functions.https.HttpsError("failed-precondition", `Missing ingredients for ${recipe.name}. Need ${req.quantity}x ${req.itemId}.`);
            }

            // Remove ingredients
            indicesToRemove.sort((a, b) => b - a).forEach(index => inventory.splice(index, 1));
        }

        const resultItem = {
            ...recipe.result,
            id: `${recipe.result.id}_${Date.now()}`
        };
        inventory.push(resultItem);

        const updates: any = {
            energy: energy - 5,
            energyUpdatedAt: energyUpdatedAt,
            inventory: inventory,
            hp: character.hp,
            healingState: character.healingState,
            "professionStats.cooking": admin.firestore.FieldValue.increment(2),
            xp: admin.firestore.FieldValue.increment(20)
        };

        transaction.update(playerRef, updates);
        recordLog(transaction, userId, "Cook", `Cooked ${recipe.name}`, 0, 20);

        return { success: true, cookedItem: recipe.name };
    });
});

export const getRecipes = functions.https.onCall(async (data, context) => {
    return Object.values(RECIPES);
});

export const cookFish = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        let character = snapshot.data() as any;
        character = processCharacterUpdates(character);

        assertCanPerformAction(character, "cook", { blockBusy: true, blockHealing: true });

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < 2) throw new functions.https.HttpsError("failed-precondition", "Not enough energy (2 required).");

        const inventory = character.inventory || [];
        const fishIndex = inventory.findIndex((i: any) => i.id === itemId && i.type === "Fish");
        if (fishIndex === -1) throw new functions.https.HttpsError("not-found", "Fish not found in inventory.");

        const fish = inventory[fishIndex];
        const cookedItem = {
            id: `cooked_${fish.id}`,
            name: `Cooked ${fish.name}`,
            type: "Consumable",
            healAmount: (fish.healAmount || 10) * 2,
            price: fish.price * 1.5,
            rarity: "Common"
        };

        inventory.splice(fishIndex, 1);
        inventory.push(cookedItem);

        const updates: any = {
            energy: energy - 2,
            energyUpdatedAt: energyUpdatedAt,
            inventory: inventory,
            hp: character.hp,
            healingState: character.healingState,
            "professionStats.cooking": admin.firestore.FieldValue.increment(1),
            xp: admin.firestore.FieldValue.increment(10)
        };

        transaction.update(playerRef, updates);
        recordLog(transaction, userId, "CookFish", `Cooked ${fish.name}`, 0, 10);

        return { success: true, cookedItem: cookedItem.name };
    });
});

export const spawnDailyRaid = functions.pubsub.schedule("30 17 * * *")
    .timeZone("America/New_York")
    .onRun(async (context) => {
        return spawnRaidLogic();
    });

export const forceSpawnRaid = functions.https.onCall(async (data, context) => {
    await checkAdmin(context);
    return spawnRaidLogic();
});

async function spawnRaidLogic() {
    const boss = WORLD_BOSSES[Math.floor(Math.random() * WORLD_BOSSES.length)];
    const location = RAID_LOCATIONS[Math.floor(Math.random() * RAID_LOCATIONS.length)];
    const raidId = `raid_${Date.now()}`;

    const raidData = {
        id: raidId,
        enemy: { ...boss, hp: boss.hp },
        locationId: location,
        totalDamageTaken: 0,
        participants: {},
        status: "Active",
        spawnTime: Date.now(),
        endTime: null
    };

    const activeRaidsSnap = await db.collection("gameData").doc("world").collection("raids")
        .where("status", "==", "Active")
        .get();

    const batch = db.batch();
    activeRaidsSnap.forEach(doc => {
        batch.update(doc.ref, { status: "Expired", endTime: Date.now() });
    });

    const raidRef = db.collection("gameData").doc("world").collection("raids").doc(raidId);
    batch.set(raidRef, raidData);

    const announcementRef = db.collection("gameData").doc("world").collection("announcements").doc();
    batch.set(announcementRef, {
        text: `⚠️ WORLD RAID ALERT: ${boss.name} has appeared at ${location}!`,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    await batch.commit();
    return { success: true, boss: boss.name, location };
}

export const raidCombatAction = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { raidId, action, techniqueId, itemId } = data;
    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);
    const raidRef = db.collection("gameData").doc("world").collection("raids").doc(raidId);

    return db.runTransaction(async (transaction) => {
        const [playerSnap, raidSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(raidRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!raidSnap.exists) throw new functions.https.HttpsError("not-found", "Raid not found.");

        const character = playerSnap.data() as any;
        const raid = raidSnap.data() as any;

        if (raid.status !== "Active") throw new functions.https.HttpsError("failed-precondition", "Raid is no longer active.");
        if (character.hp <= 0) throw new functions.https.HttpsError("failed-precondition", "You are too wounded to fight!");

        // Simplified Combat Logic for Raid
        const pStats = calculateCombatStats(character, []);
        const eStats = calculateCombatStats(raid.enemy, []);

        let damage = 0;
        let logs: string[] = [];

        if (action === "Attack") {
            const hitRoll = Math.random() * 100;
            if (hitRoll < pStats.accuracy - eStats.dodge) {
                const isCrit = Math.random() * 100 < pStats.critChance;
                damage = calculateDamage(pStats, eStats, [], [], isCrit, getHighestCombatSkill(character), null, character.mythicArt);
                logs.push(`You hit ${raid.enemy.name} for ${damage} damage!`);
            } else {
                logs.push(`You missed ${raid.enemy.name}!`);
            }
        } else if (action === "Technique") {
            // Very basic technique support for now
            const tech = STATIC_TECHNIQUES[techniqueId];
            if (!tech) throw new functions.https.HttpsError("not-found", "Technique not found.");

            const cost = tech.energyCost || 0;
            if (character.energy < cost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

            transaction.update(playerRef, { energy: character.energy - cost });

            damage = Math.floor(pStats.strength * tech.power * 1.5);
            logs.push(`You use ${tech.name} and deal ${damage} damage!`);
        }

        // Apply damage to raid boss
        const newDamageTaken = raid.totalDamageTaken + damage;
        const remainingHp = raid.enemy.hp - damage;

        const participants = raid.participants || {};
        const pData = participants[userId] || { userId, userName: character.name, totalDamage: 0 };
        pData.totalDamage += damage;
        pData.lastHitAt = Date.now();
        participants[userId] = pData;

        const raidUpdate: any = {
            totalDamageTaken: newDamageTaken,
            participants: participants,
            "enemy.hp": Math.max(0, remainingHp)
        };

        if (remainingHp <= 0) {
            raidUpdate.status = "Defeated";
            raidUpdate.endTime = Date.now();

            // System Announcement for Victory
            const announcementRef = db.collection("gameData").doc("world").collection("announcements").doc();
            transaction.set(announcementRef, {
                text: `🏆 VICTORY: ${raid.enemy.name} has been defeated by the combined forces of the sea!`,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
        }

        transaction.update(raidRef, raidUpdate);

        return { success: true, damage, logs, bossDefeated: remainingHp <= 0 };
    });
});

export const onRaidDefeated = functions.firestore
    .document("gameData/world/raids/{raidId}")
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const oldData = change.before.data();

        if (newData?.status === "Defeated" && oldData?.status !== "Defeated") {
            await distributeRaidRewards(newData);
        }
    });

async function distributeRaidRewards(raid: any) {
    const participants = Object.values(raid.participants || {});
    if (participants.length === 0) return;

    const bossName = raid.enemy.name;
    const totalDmg = raid.totalDamageTaken || 1;

    for (let i = 0; i < participants.length; i += 400) {
        const batch = db.batch();
        const chunk = participants.slice(i, i + 400);

        for (const p of chunk as any[]) {
            const mailRef = db.collection("players").doc(p.userId).collection("mail").doc();
            const participationFactor = 0.5;
            const performanceFactor = (p.totalDamage / totalDmg) * 0.5;
            const baseGold = raid.enemy.level * 200;
            const goldReward = Math.floor(baseGold * (participationFactor + performanceFactor));
            const xpReward = raid.enemy.level * 100;

            batch.set(mailRef, {
                id: mailRef.id,
                senderName: "World Event",
                subject: `Raid Rewards: ${bossName}`,
                body: `The ${bossName} has fallen! For your bravery and ${p.totalDamage} damage dealt, the world thanks you.`,
                timestamp: Date.now(),
                isRead: false,
                claimed: false,
                rewards: {
                    gold: goldReward,
                    xp: xpReward
                }
            });
        }
        await batch.commit();
    }
}

// --- Faction War System ---

export const startWar = functions.https.onCall(async (data, context) => {
    await checkAdmin(context);
    const { locationId } = data;

    const warState = {
        id: "current",
        targetLocation: locationId,
        startTime: Date.now(),
        endTime: Date.now() + (60 * 60 * 1000), // 1 hour duration
        navyScore: 0,
        pirateScore: 0,
        isActive: true
    };

    await db.collection("gameData").doc("world").collection("war").document("current").set(warState);

    const announcementRef = db.collection("gameData").doc("world").collection("announcements").doc();
    await announcementRef.set({
        text: `⚔️ WAR ALERT: The battle for ${locationId} has begun! Factions, mobilize!`,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true };
});

export const endWar = functions.https.onCall(async (data, context) => {
    await checkAdmin(context);

    const warRef = db.collection("gameData").doc("world").collection("war").document("current");
    const warSnap = await warRef.get();
    if (!warSnap.exists) return { success: false, message: "No active war found." };

    const war = warSnap.data() as any;
    if (!war.isActive) return { success: false, message: "War is not active." };

    const winner = war.navyScore > war.pirateScore ? "Navy" : (war.pirateScore > war.navyScore ? "Pirate" : "Neutral");

    const batch = db.batch();

    // Update location control
    const locRef = db.collection("gameData").doc("world").collection("locations").doc(war.targetLocation);
    batch.update(locRef, { controlledBy: winner });

    // Close war
    batch.update(warRef, { isActive: false, endTime: Date.now() });

    const announcementRef = db.collection("gameData").doc("world").collection("announcements").doc();
    batch.set(announcementRef, {
        text: `🏆 WAR OVER: The ${winner} has taken control of ${war.targetLocation}!`,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    await batch.commit();
    return { success: true, winner };
});
