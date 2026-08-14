import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// --- Constants & Config ---
const ENERGY_REGEN_RATE_MS = 3 * 60 * 1000; // 1 energy per 3 minutes
const MAX_ENERGY = 100;
const INVENTORY_CAPACITY = 20;
const TURN_TIMEOUT_MS = 60 * 1000; // 1 minute per turn
const HEALING_DURATION_MS = 2 * 60 * 1000; // 2 minutes
const TRAINING_DURATION_MS = 20 * 1000; // 20 seconds
const TRAINING_GOLD_COST = 50;
const MYTHIC_ROLL_GOLD_COST = 1000000;

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
    element?: string,
    elementalWeaknesses?: string[],
    travelTimeMultiplier?: number,
    canLearnNonCombatSkills?: boolean,
    restrictedSkillTypes?: string[]
}>> = {
    "F": [
        { name: "Novice Strike", description: "A basic strike taught to every beginner.", stats: { strength: 1 }, skillMultiplier: 1.10, multipliedSkill: "Swordsmanship", techniques: ["Horizontal Slash"], hugeBuffType: "Strength", hugeBuffValue: 0.05, weakAgainst: ["MartialArts"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Rusty Guard", description: "Using a worn blade to deflect blows.", stats: { endurance: 1 }, skillMultiplier: 1.10, multipliedSkill: "Blacksmith", techniques: ["Sturdy Block"], hugeBuffType: "Endurance", hugeBuffValue: 0.05, weakAgainst: ["Brawling"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Quick Step", description: "A simple movement to reposition.", stats: { agility: 1 }, skillMultiplier: 1.10, multipliedSkill: "Navigating", techniques: ["Dash"], hugeBuffType: "Agility", hugeBuffValue: 0.05, weakAgainst: ["Sniper"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Dull Edge", description: "Attacking with a poorly maintained weapon.", stats: { strength: 1, agility: 1 }, skillMultiplier: 1.10, multipliedSkill: "Brawling", techniques: ["Heavy Chop"], hugeBuffType: "Strength", hugeBuffValue: 0.05, weakAgainst: ["MartialArts"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Simple Thrust", description: "A straightforward piercing attack.", stats: { perception: 1 }, skillMultiplier: 1.10, multipliedSkill: "Spear", techniques: ["Point Strike"], hugeBuffType: "Perception", hugeBuffValue: 0.05, weakAgainst: ["Swordsmanship"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Steady Breath", description: "Focusing on breathing to maintain stamina.", stats: { willpower: 1 }, skillMultiplier: 1.10, multipliedSkill: "Medical", techniques: ["Calm State"], hugeBuffType: "Willpower", hugeBuffValue: 0.05, weakAgainst: ["Gunslinging"], element: "Light", elementalWeaknesses: ["Dark"] },
        { name: "Lucky Swipe", description: "An unplanned attack that somehow lands.", stats: { luck: 1 }, skillMultiplier: 1.10, multipliedSkill: "TreasureHunting", techniques: ["Wild Swing"], hugeBuffType: "Luck", hugeBuffValue: 0.05, weakAgainst: ["Spear"], element: "Light", elementalWeaknesses: ["Dark"] },
        { name: "Basic Flourish", description: "A simple showy move with no real power.", stats: { agility: 1, luck: 1 }, skillMultiplier: 1.10, multipliedSkill: "Cooking", techniques: ["Distraction"], hugeBuffType: "Agility", hugeBuffValue: 0.05, weakAgainst: ["Sniper"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Fisherman's Hook", description: "A technique derived from daily chores.", stats: { strength: 1, perception: 1 }, skillMultiplier: 1.10, multipliedSkill: "Fishing", techniques: ["Pull"], hugeBuffType: "Strength", hugeBuffValue: 0.05, weakAgainst: ["MartialArts"], element: "Water", elementalWeaknesses: ["Lightning"] },
        { name: "Sailor's Balance", description: "Maintaining footing on uneven ground.", stats: { agility: 1, endurance: 1 }, skillMultiplier: 1.10, multipliedSkill: "Navigating", techniques: ["Brace"], hugeBuffType: "Endurance", hugeBuffValue: 0.05, weakAgainst: ["Brawling"], element: "Water", elementalWeaknesses: ["Lightning"] }
    ],
    "E": [
        { name: "Steel Bite", description: "A more focused strike that pierces deeper.", stats: { strength: 4 }, skillMultiplier: 1.30, multipliedSkill: "Gunslinging", techniques: ["Deep Cut"], hugeBuffType: "Strength", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Vanguard Defense", description: "A defensive stance used by front-line soldiers.", stats: { endurance: 4 }, skillMultiplier: 1.30, multipliedSkill: "MartialArts", techniques: ["Iron Wall"], hugeBuffType: "Endurance", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Fleet Foot", description: "Agile movements that baffle the inexperienced.", stats: { agility: 4 }, skillMultiplier: 1.30, multipliedSkill: "Sniper", techniques: ["Evasion"], hugeBuffType: "Agility", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Sharpened Senses", description: "Heightened awareness on the battlefield.", stats: { perception: 4 }, skillMultiplier: 1.30, multipliedSkill: "MysticArts", techniques: ["Pre-empt"], hugeBuffType: "Perception", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Lightning", elementalWeaknesses: ["Earth"] },
        { name: "Stone Heart", description: "Resisting fear and mental pressure.", stats: { willpower: 4 }, skillMultiplier: 1.30, multipliedSkill: "Brawling", techniques: ["Unshakable"], hugeBuffType: "Willpower", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Gambler's Strike", description: "A high-risk, high-reward attack.", stats: { luck: 5 }, skillMultiplier: 1.30, multipliedSkill: "TreasureHunting", techniques: ["Double or Nothing"], hugeBuffType: "Luck", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Chaos", elementalWeaknesses: ["Void"] },
        { name: "Twin Fang", description: "A rapid two-hit combination.", stats: { strength: 2, agility: 3 }, skillMultiplier: 1.30, multipliedSkill: "Swordsmanship", techniques: ["Double Slash"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Crushing Weight", description: "Leveraging body weight into a strike.", stats: { strength: 5 }, skillMultiplier: 1.30, multipliedSkill: "Brawling", techniques: ["Slam"], hugeBuffType: "Strength", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Eagle Eye", description: "Spotting weaknesses from a distance.", stats: { perception: 5 }, skillMultiplier: 1.30, multipliedSkill: "Sniper", techniques: ["Precision Hit"], hugeBuffType: "Perception", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Lightning", elementalWeaknesses: ["Earth"] },
        { name: "Brave Charge", description: "Rushing forward with reckless abandon.", stats: { willpower: 5, strength: 2 }, skillMultiplier: 1.30, multipliedSkill: "MartialArts", techniques: ["Stampede"], hugeBuffType: "Strength", hugeBuffValue: 0.15, weakAgainst: ["Gunslinging"], element: "Fire", elementalWeaknesses: ["Water"] }
    ],
    "D": [
        { name: "Rippling Blade", description: "A fluid attack that bypasses simple parries.", stats: { swordsmanship: 7, agility: 2 }, skillMultiplier: 1.60, multipliedSkill: "Swordsmanship", techniques: ["Flowing Strike"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Water", elementalWeaknesses: ["Lightning"] },
        { name: "Mountain's Resolve", description: "Standing firm against a tide of enemies.", stats: { endurance: 8, willpower: 2 }, skillMultiplier: 1.60, multipliedSkill: "Blacksmith", techniques: ["Immovable"], hugeBuffType: "Endurance", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Whirlwind Spin", description: "A spinning attack that hits multiple targets.", stats: { agility: 8, strength: 2 }, skillMultiplier: 1.60, multipliedSkill: "Spear", techniques: ["Cyclone"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Hunter's Mark", description: "Tracking a target with lethal intent.", stats: { perception: 8, luck: 2 }, skillMultiplier: 1.60, multipliedSkill: "Sniper", techniques: ["Focused Fire"], hugeBuffType: "Sniper", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Lightning", elementalWeaknesses: ["Earth"] },
        { name: "Iron Fist", description: "Combining martial arts with swordplay.", stats: { martialArts: 7, strength: 3 }, skillMultiplier: 1.60, multipliedSkill: "MartialArts", techniques: ["Grip Smash"], hugeBuffType: "MartialArts", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Silent Step", description: "Moving without a sound to ambush foes.", stats: { agility: 10 }, skillMultiplier: 1.60, multipliedSkill: "TreasureHunting", techniques: ["Shadow Strike"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Piercing Gale", description: "A thrust that carries the force of a gust.", stats: { strength: 10 }, skillMultiplier: 1.60, multipliedSkill: "Spear", techniques: ["Air Piercer"], hugeBuffType: "Spear", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Serpent's Coil", description: "A deceptive technique that traps weapons.", stats: { agility: 7, perception: 3 }, skillMultiplier: 1.60, multipliedSkill: "MysticArts", techniques: ["Disarm"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Water", elementalWeaknesses: ["Lightning"] },
        { name: "Thunderous Clap", description: "An explosive strike that dazes opponents.", stats: { strength: 9, willpower: 3 }, skillMultiplier: 1.60, multipliedSkill: "Brawling", techniques: ["Shockwave"], hugeBuffType: "Strength", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Lightning", elementalWeaknesses: ["Earth"] },
        { name: "Mirror Image", description: "A feint that leaves an afterimage.", stats: { agility: 9, luck: 4 }, skillMultiplier: 1.60, multipliedSkill: "MysticArts", techniques: ["Flicker"], hugeBuffType: "Agility", hugeBuffValue: 0.30, weakAgainst: ["Sniper"], element: "Air", elementalWeaknesses: ["Ice"] }
    ],
    "C": [
        { name: "Azure Flow", description: "Mastering the rhythm of combat.", stats: { swordsmanship: 12, agility: 5 }, skillMultiplier: 2.00, multipliedSkill: "Swordsmanship", techniques: ["Water Slicer"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Water", elementalWeaknesses: ["Lightning"] },
        { name: "Grizzly Crush", description: "An overwhelming strike with brute force.", stats: { strength: 15, endurance: 5 }, skillMultiplier: 2.00, multipliedSkill: "Brawling", techniques: ["Bone Breaker"], hugeBuffType: "Strength", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Wind Runner", description: "Moving as fast as the breeze.", stats: { agility: 15, luck: 5 }, skillMultiplier: 2.00, multipliedSkill: "Navigating", techniques: ["Breeze Step"], hugeBuffType: "Agility", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Watcher's Gaze", description: "Seeing through illusions and feints.", stats: { perception: 15, willpower: 5 }, skillMultiplier: 2.00, multipliedSkill: "Sniper", techniques: ["True Vision"], hugeBuffType: "Perception", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Lightning", elementalWeaknesses: ["Earth"] },
        { name: "Soul Shield", description: "Protecting the mind from dark arts.", stats: { willpower: 15, mysticArts: 5 }, skillMultiplier: 2.00, multipliedSkill: "MysticArts", techniques: ["Purge"], hugeBuffType: "MysticArts", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Light", elementalWeaknesses: ["Dark"] },
        { name: "Crimson Edge", description: "A blood-soaked blade that thirsts for battle.", stats: { luck: 15, strength: 5 }, skillMultiplier: 2.00, multipliedSkill: "Swordsmanship", techniques: ["Bleed Out"], hugeBuffType: "Swordsmanship", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Dark", elementalWeaknesses: ["Light"] },
        { name: "Storm Caller", description: "Infusing attacks with static energy.", stats: { mysticArts: 12, agility: 5 }, skillMultiplier: 2.00, multipliedSkill: "MysticArts", techniques: ["Bolt Strike"], hugeBuffType: "MysticArts", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Lightning", elementalWeaknesses: ["Earth"] },
        { name: "Earth Shaker", description: "Striking the ground to disrupt balance.", stats: { strength: 14, endurance: 6 }, skillMultiplier: 2.00, multipliedSkill: "Blacksmith", techniques: ["Tremor"], hugeBuffType: "Strength", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Desert Mirage", description: "A shimmering technique that hides intent.", stats: { agility: 14, perception: 6 }, skillMultiplier: 2.00, multipliedSkill: "TreasureHunting", techniques: ["Sand Trap"], hugeBuffType: "Agility", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Air", elementalWeaknesses: ["Ice"] },
        { name: "Phoenix Rise", description: "Recovering from the brink with newfound vigor.", stats: { willpower: 14, luck: 6 }, skillMultiplier: 2.00, multipliedSkill: "Medical", techniques: ["Rebirth"], hugeBuffType: "Willpower", hugeBuffValue: 0.60, weakAgainst: ["Swordsmanship"], element: "Fire", elementalWeaknesses: ["Water"] }
    ],
    "B": [
        { name: "Dragon's Breath", description: "Exhaling power through the blade.", stats: { swordsmanship: 40, mysticArts: 20, strength: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Fire Slash", "Heat Haze"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Fire", elementalWeaknesses: ["Water"] },
        { name: "Titan's Grip", description: "Wielding massive weapons with ease.", stats: { strength: 50, endurance: 20, willpower: 10 }, skillMultiplier: 4.00, multipliedSkill: "Blacksmith", techniques: ["Colossus Strike", "Earth Breaker"], hugeBuffType: "Strength", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Lightning Reflex", description: "Reacting before the thought even forms.", stats: { agility: 50, perception: 20, luck: 10 }, skillMultiplier: 4.00, multipliedSkill: "Gunslinging", techniques: ["Flash Step", "Afterimage"], hugeBuffType: "Agility", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Lightning", elementalWeaknesses: ["Earth"] },
        { name: "Oracle's Whisper", description: "Hearing the future of the fight.", stats: { perception: 50, willpower: 20, agility: 10 }, skillMultiplier: 4.00, multipliedSkill: "Navigating", techniques: ["Prevision", "Mind Link"], hugeBuffType: "Perception", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Light", elementalWeaknesses: ["Dark"] },
        { name: "Void Anchor", description: "Grounding oneself in the fabric of reality.", stats: { willpower: 50, endurance: 20, mysticArts: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Nullify", "Gravity Field"], hugeBuffType: "Willpower", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Void", elementalWeaknesses: ["Chaos"] },
        { name: "Fortune's Favor", description: "Destiny smiles upon your every move.", stats: { luck: 60, agility: 10, perception: 10 }, skillMultiplier: 4.00, multipliedSkill: "TreasureHunting", techniques: ["Destiny Strike", "Jackpot"], hugeBuffType: "Luck", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Celestial", elementalWeaknesses: ["Void"] },
        { name: "Frost Bite", description: "Freezing the enemy's movements.", stats: { mysticArts: 40, agility: 20, endurance: 10 }, skillMultiplier: 4.00, multipliedSkill: "Cooking", techniques: ["Ice Prison", "Glacial Wall"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Ice", elementalWeaknesses: ["Air"] },
        { name: "Raging Torrent", description: "A relentless barrage of attacks.", stats: { swordsmanship: 45, strength: 15, agility: 10 }, skillMultiplier: 4.00, multipliedSkill: "Fishing", techniques: ["Flood", "Tidal Wave"], hugeBuffType: "Swordsmanship", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Water", elementalWeaknesses: ["Lightning"] },
        { name: "Shadow Weaver", description: "Manipulating shadows to bind foes.", stats: { mysticArts: 45, perception: 15, luck: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Dark Bind", "Nightmare"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Dark", elementalWeaknesses: ["Light"] },
        { name: "Celestial Alignment", description: "Drawing power from the stars.", stats: { willpower: 45, luck: 15, mysticArts: 10 }, skillMultiplier: 4.00, multipliedSkill: "MysticArts", techniques: ["Starfall", "Sunbeam"], hugeBuffType: "MysticArts", hugeBuffValue: 1.50, weakAgainst: ["MysticArts"], element: "Celestial", elementalWeaknesses: ["Void"] }
    ],
    "A": [
        { name: "Nebula Strike", description: "A cosmic strike that transcends dimensions.", stats: { swordsmanship: 80, mysticArts: 50, strength: 30 }, skillMultiplier: 10.00, multipliedSkill: "MysticArts", techniques: ["Cosmic Tear", "Black Hole", "Nova"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Celestial", elementalWeaknesses: ["Void"] },
        { name: "Atlas Burden", description: "Holding the weight of the heavens.", stats: { strength: 90, endurance: 50, willpower: 30 }, skillMultiplier: 10.00, multipliedSkill: "Blacksmith", techniques: ["Heavenly Smash", "Sky Cracker", "Final Pillar"], hugeBuffType: "Strength", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Chronos Step", description: "Moving through time for a brief moment.", stats: { agility: 90, perception: 50, luck: 30 }, skillMultiplier: 10.00, multipliedSkill: "Navigating", techniques: ["Time Warp", "Stutter", "Future Echo"], hugeBuffType: "Agility", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Void", elementalWeaknesses: ["Chaos"] },
        { name: "Spirit Reaper", description: "Striking at the very soul of the opponent.", stats: { mysticArts: 90, willpower: 50, perception: 30 }, skillMultiplier: 10.00, multipliedSkill: "Medical", techniques: ["Soul Rend", "Spirit Bind", "Essence Theft"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Dark", elementalWeaknesses: ["Light"] },
        { name: "Eternal Bastion", description: "An unbreakable defense that reflects damage.", stats: { endurance: 90, luck: 50, strength: 30 }, skillMultiplier: 10.00, multipliedSkill: "Blacksmith", techniques: ["Mirror Shield", "Fortress", "Aegis"], hugeBuffType: "Endurance", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "King's Authority", description: "Commanding the battlefield with presence.", stats: { willpower: 90, perception: 50, agility: 30 }, skillMultiplier: 10.00, multipliedSkill: "Navigating", techniques: ["Overawe", "Command", "Domination"], hugeBuffType: "Willpower", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Divine", elementalWeaknesses: ["Chaos"] },
        { name: "Nature's Wrath", description: "Harnessing the power of the natural world.", stats: { mysticArts: 80, strength: 40, endurance: 30 }, skillMultiplier: 10.00, multipliedSkill: "Fishing", techniques: ["Entangle", "Root Spike", "Thorn Hail"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Earth", elementalWeaknesses: ["Air"] },
        { name: "Silver Lining", description: "Finding victory in the direst situations.", stats: { luck: 100, agility: 40, perception: 30 }, skillMultiplier: 10.00, multipliedSkill: "TreasureHunting", techniques: ["Miracle", "Lucky Break", "Twist of Fate"], hugeBuffType: "Luck", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Celestial", elementalWeaknesses: ["Void"] },
        { name: "Solar Flare", description: "Blinding enemies with the brilliance of the sun.", stats: { mysticArts: 85, perception: 40, willpower: 30 }, skillMultiplier: 10.00, multipliedSkill: "Cooking", techniques: ["Sunburst", "Blinding Light", "Solar Storm"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Fire", elementalWeaknesses: ["Water"] },
        { name: "Abyssal Maw", description: "Consuming the light and hope of foes.", stats: { mysticArts: 85, willpower: 40, endurance: 30 }, skillMultiplier: 10.00, multipliedSkill: "MysticArts", techniques: ["Devour", "Void Pull", "Darkness Falls"], hugeBuffType: "MysticArts", hugeBuffValue: 5.00, weakAgainst: ["Spear"], element: "Dark", elementalWeaknesses: ["Light"] }
    ],
    "S": [
        { name: "Godspeed", description: "Surpassing the limits of human speed.", stats: { agility: 60, perception: 30 }, skillMultiplier: 25.00, multipliedSkill: "Sniper", techniques: ["Sonic Boom", "Infinite Afterimage", "Flash Step", "Time Warp", "Evasion", "Shadow Strike", "Afterimage", "Stutter", "Future Echo", "Dash"], hugeBuffType: "Agility", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, weakAgainst: ["Brawling"], element: "Void", elementalWeaknesses: ["Chaos"] },
        { name: "World Sunderer", description: "A strike capable of splitting islands.", stats: { strength: 70, swordsmanship: 40 }, skillMultiplier: 25.00, multipliedSkill: "Swordsmanship", techniques: ["Great Divide", "Earth Quake", "Universal Cut", "One Strike", "Alpha Strike", "Colossus Strike", "Earth Breaker", "Sky Cracker", "Final Pillar", "Heavenly Smash"], hugeBuffType: "Strength", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, weakAgainst: ["Brawling"], element: "Chaos", elementalWeaknesses: ["Void"] },
        { name: "Maelstrom of Souls", description: "A vortex of spiritual energy.", stats: { mysticArts: 65, willpower: 45 }, skillMultiplier: 25.00, multipliedSkill: "MysticArts", techniques: ["Soul Suck", "Spirit Explosion", "Soul Rend", "Spirit Bind", "Essence Theft", "Darkness Falls", "Void Pull", "Black Hole", "Nightmare", "Dark Bind"], hugeBuffType: "MysticArts", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, weakAgainst: ["Brawling"], element: "Void", elementalWeaknesses: ["Chaos"] },
        { name: "Absolute Zero", description: "Freezing time and space itself.", stats: { mysticArts: 68, endurance: 52 }, skillMultiplier: 25.00, multipliedSkill: "Medical", techniques: ["Frozen Domain", "Shatter", "Glacial Wall", "Ice Prison", "Iron Wall", "Immovable", "Mirror Shield", "Fortress", "Aegis", "Sturdy Block"], hugeBuffType: "MysticArts", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, weakAgainst: ["Brawling"], element: "Ice", elementalWeaknesses: ["Air"] },
        { name: "Divine Providence", description: "Guided by the hand of fate.", stats: { luck: 100, willpower: 50 }, skillMultiplier: 25.00, multipliedSkill: "TreasureHunting", techniques: ["Fate's Seal", "Unstoppable Force", "Miracle", "Jackpot", "Lucky Break", "Twist of Fate", "Destiny Strike", "Double or Nothing", "Butterfly Effect", "Entropy"], hugeBuffType: "Luck", hugeBuffValue: 15.00, debuffPercentage: 0.10, energyRegainMultiplier: 1.50, weakAgainst: ["Brawling"], element: "Divine", elementalWeaknesses: ["Chaos"] }
    ],
    "SS": [
        { name: "Chaos Theory", description: "Mastering the unpredictability of existence.", stats: { luck: 150, mysticArts: 100, perception: 50 }, skillMultiplier: 100.00, multipliedSkill: "MysticArts", techniques: ["Entropy", "Butterfly Effect", "Singularity", "Fate's Seal", "Destiny Strike", "Jackpot", "Twist of Fate", "Flicker", "Shadow Strike", "Time Warp"], hugeBuffType: "Luck", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, weakAgainst: ["MartialArts"], element: "Chaos", elementalWeaknesses: ["Void"] },
        { name: "Elysium's Gate", description: "Opening the doors to a higher plane.", stats: { willpower: 150, endurance: 100, mysticArts: 50 }, skillMultiplier: 100.00, multipliedSkill: "Medical", techniques: ["Ascension", "Holy Rain", "Judgment", "Rebirth", "Purge", "Sunbeam", "Solar Storm", "Blinding Light", "Sunburst", "Miracle"], hugeBuffType: "Willpower", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, weakAgainst: ["MartialArts"], element: "Celestial", elementalWeaknesses: ["Void"] },
        { name: "Void Reaver", description: "Erasing anything the blade touches.", stats: { swordsmanship: 120, strength: 100, agility: 80 }, skillMultiplier: 100.00, multipliedSkill: "Swordsmanship", techniques: ["Erasure", "Non-Existence", "Dark Matter", "Universal Cut", "One Strike", "Alpha Strike", "End of All", "Finality", "Rewrite", "Delete"], hugeBuffType: "Swordsmanship", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, weakAgainst: ["MartialArts"], element: "Void", elementalWeaknesses: ["Celestial"] },
        { name: "Genesis", description: "The power of creation at your fingertips.", stats: { strength: 80, endurance: 80, agility: 80, perception: 80, willpower: 80, luck: 80, swordsmanship: 80, brawling: 80, gunslinging: 80, spear: 80, martialArts: 80, sniper: 80, mysticArts: 80 }, skillMultiplier: 100.00, multipliedSkill: "Cooking", techniques: ["Creation", "Renewal", "Alpha Strike", "Horizontal Slash", "Sturdy Block", "Dash", "Heavy Chop", "Point Strike", "Calm State", "Wild Swing"], hugeBuffType: "MysticArts", hugeBuffValue: 50.00, debuffPercentage: 0.05, energyRegainMultiplier: 1.75, weakAgainst: ["MartialArts"], element: "Genesis", elementalWeaknesses: ["Void"] }
    ],
    "SSS": [
        { name: "Zenith", description: "The absolute pinnacle of martial prowess.", stats: { strength: 300, agility: 300, swordsmanship: 500, perception: 200 }, skillMultiplier: 500.00, multipliedSkill: "Swordsmanship", techniques: ["One Strike", "Universal Cut", "End of All", "Great Divide", "Earth Quake", "Sonic Boom", "Infinite Afterimage", "Alpha Strike", "Flash Step", "Time Warp", "Cosmic Tear", "Black Hole", "Nova", "Final Pillar"], hugeBuffType: "Swordsmanship", hugeBuffValue: 250.00, debuffPercentage: 0.0, energyRegainMultiplier: 2.0, element: "Divine", elementalWeaknesses: ["Chaos"] },
        { name: "Omegalyth", description: "The beginning and the end of all things.", stats: { mysticArts: 500, willpower: 400, luck: 300, endurance: 300 }, skillMultiplier: 500.00, multipliedSkill: "MysticArts", techniques: ["Erasure", "Rebirth", "Finality", "Soul Suck", "Spirit Explosion", "Frozen Domain", "Shatter", "Ascension", "Holy Rain", "Judgment", "Cosmic Tear", "Black Hole", "Nova", "Singularity"], hugeBuffType: "MysticArts", hugeBuffValue: 250.00, debuffPercentage: 0.0, energyRegainMultiplier: 2.0, element: "Void", elementalWeaknesses: ["Creation"] },
        { name: "The Author's Pen", description: "Rewriting the very laws of reality.", stats: { luck: 999, willpower: 999, perception: 999 }, skillMultiplier: 500.00, multipliedSkill: "TreasureHunting", techniques: ["Rewrite", "Delete", "Absolute Command", "Fate's Seal", "Unstoppable Force", "Miracle", "Singularity", "Entropy", "Butterfly Effect", "Creation", "Renewal", "Alpha Strike", "Non-Existence", "Erasure"], hugeBuffType: "Willpower", hugeBuffValue: 250.00, debuffPercentage: 0.0, energyRegainMultiplier: 2.0, element: "Creation", elementalWeaknesses: ["Annihilation"] }
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
            element: "Annihilation", elementalWeaknesses: ["Creation"]
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
            element: "Creation", elementalWeaknesses: ["Annihilation"]
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
    "Ironcrest Isle": { x: 160, y: 40, region: "East Blue" },
    "Amber Reach": { x: -80, y: 150, region: "East Blue" },
    "Sunken Reef": { x: 70, y: 90, region: "East Blue" },
    "Tortuga Bay": { x: 30, y: -210, region: "South Blue" },
    "Pirate\u0027s Den": { x: 350, y: -350, region: "South Blue" },
    "Navy Outpost Aqua": { x: -160, y: -110, region: "South Blue" },
    "Navy Outpost Terra": { x: -300, y: 200, region: "Grand Line" },
    "Navy Outpost Ignis": { x: 400, y: 300, region: "Grand Line" },
    "Crystal Cove": { x: 280, y: 120, region: "Grand Line" },
    "Volcano Peak": { x: 420, y: 240, region: "Grand Line" },
    "Whispering Woods": { x: -150, y: 180, region: "Grand Line" },
    "Serpent\u0027s Maw": { x: 500, y: 500, region: "Grand Line" },
    "Kraken\u0027s Rest": { x: -400, y: -400, region: "South Blue" },
    "Shadow Fen": { x: -300, y: -100, region: "East Blue" },
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

export function calculateCurrentEnergy(character: any): { energy: number, energyUpdatedAt: number } {
    const now = Date.now();
    const elapsed = now - character.energyUpdatedAt;
    const regenerated = Math.floor(elapsed / ENERGY_REGEN_RATE_MS);

    const currentMaxEnergy = character.maxEnergy || MAX_ENERGY;
    if (regenerated <= 0) return { energy: character.energy, energyUpdatedAt: character.energyUpdatedAt };

    const newEnergy = Math.min(currentMaxEnergy, character.energy + regenerated);
    const newTimestamp = character.energy + regenerated >= currentMaxEnergy ? now : character.energyUpdatedAt + (regenerated * ENERGY_REGEN_RATE_MS);

    return { energy: newEnergy, energyUpdatedAt: newTimestamp };
}

function checkLevelUp(character: any) {
    let { level, xp, stats, maxEnergy, energy, maxHp, hp } = character;
    const MAX_LEVEL = 300;

    if (level >= MAX_LEVEL) {
        return { ...character, level: MAX_LEVEL, xp: 0, leveledUp: false };
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
        } else {
            xp = 0; // Cap XP at level 300
        }
        leveledUp = true;
    }

    return { ...character, level, xp, stats, maxEnergy, energy, maxHp, hp, leveledUp };
}

export const rollMythicArt = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const userId = context.auth.uid;
    const playerRef = db.collection("players").doc(userId);

    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(playerRef);
        if (!snapshot.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = snapshot.data() as any;

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
        if (inventory.length + 3 > INVENTORY_CAPACITY) {
            throw new functions.https.HttpsError("failed-precondition", "Inventory does not have enough space for 3 artifacts.");
        }

        const rolledArtifacts = [];
        for (let i = 0; i < 3; i++) {
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

            const artifactItem = {
                id: `mythic_artifact_${tier}_${Date.now()}_${i}`,
                name: `${tier} Tier Artifact`,
                description: `A mysterious artifact that contains a random ${tier} tier Mythic Art. Use it to awaken its power.`,
                type: "Artifact",
                rarity: getRarityForTier(tier),
                price: getPriceForTier(tier),
                mythicTier: tier,
                levelRequirement: 1
            };
            rolledArtifacts.push(artifactItem);
        }

        const updates: any = {
            inventory: admin.firestore.FieldValue.arrayUnion(...rolledArtifacts)
        };

        if (freeRolls > 0) {
            updates.freeMythicRolls = freeRolls - 1;
        } else {
            updates.gold = admin.firestore.FieldValue.increment(-goldCost);
        }

        transaction.update(playerRef, updates);
        const rolledTiers = rolledArtifacts.map(a => a.mythicTier).join(", ");
        recordLog(transaction, userId, "RollMythicArt", `Rolled 3 artifacts: ${rolledTiers}`, -goldCost, 0);

        return { success: true, tiers: rolledTiers };
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

function processHealing(character: any): any {
    if (character.healingState && character.healingState.endTime <= Date.now()) {
        return {
            ...character,
            hp: character.maxHp,
            healingState: null
        };
    }
    return character;
}

// --- Player Management ---

export const createCharacter = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { name, gender, race } = data;
    const userId = context.auth.uid;

    // Validate if character already exists for this user
    const playerDoc = await db.collection("players").doc(userId).get();
    if (playerDoc.exists) {
        throw new functions.https.HttpsError("already-exists", "You already have a character.");
    }

    // Name Validation
    const trimmedName = (name || "").trim();
    if (trimmedName.length < 3 || trimmedName.length > 16) {
        throw new functions.https.HttpsError("invalid-argument", "Name must be between 3 and 16 characters.");
    }
    if (!/^[a-zA-Z0-9_]+$/.test(trimmedName)) {
        throw new functions.https.HttpsError("invalid-argument", "Name contains invalid characters.");
    }

    const reservedNames = ["admin", "system", "moderator", "game-master", "gm"];
    if (reservedNames.includes(trimmedName.toLowerCase())) {
        throw new functions.https.HttpsError("invalid-argument", "This name is reserved.");
    }

    // Case-insensitive uniqueness check
    const nameLower = trimmedName.toLowerCase();
    const nameQuery = await db.collection("players").where("nameLower", "==", nameLower).get();
    if (!nameQuery.empty) {
        throw new functions.https.HttpsError("already-exists", "Character name is already taken.");
    }

    // Race/Gender Validation
    const allowedRaces = ["Human", "Abyssal", "Beastkin", "Celestian", "Automaton"];
    const allowedGenders = ["Male", "Female", "Other"];
    if (!allowedRaces.includes(race)) throw new functions.https.HttpsError("invalid-argument", "Invalid race.");
    if (!allowedGenders.includes(gender)) throw new functions.https.HttpsError("invalid-argument", "Invalid gender.");

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
        currentLocation: "Fogi Tail Island",
        freeMythicRolls: 3,
        mythicArt: null,
        rank: "Novice Sailor",
        title: "",
        unlockedTitles: [],
        pvpWins: 0,
        pvpLosses: 0,
        faction: "Neutral",
        stats: {
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
        },
        professionStats: {
            cooking: 0,
            navigating: 0,
            treasureHunting: 0,
            blacksmith: 0,
            fishing: 0
        },
        inventory: [],
        equipment: {},
        travelState: null,
        combatState: null,
        learnedTechniques: ["bash"],
        healingState: null,
        ship: { id: "row_boat", name: "Row Boat", price: 0, speedMultiplier: 1.0 }
    };

    await db.collection("players").doc(userId).set(character);
    return { success: true };
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
        if (character.faction !== "Neutral") {
            throw new functions.https.HttpsError("failed-precondition", "You already belong to a faction.");
        }

        if (faction === "Pirate") {
            if (character.currentLocation !== "Pirate\u0027s Den") {
                throw new functions.https.HttpsError("failed-precondition", "You must be at the Pirate\u0027s Den to join the Pirates.");
            }
            transaction.update(playerRef, { faction: "Pirate", rank: "Rogue Sailor" });
            recordLog(transaction, userId, "JoinFaction", "Became a Pirate", 0, 0);
        } else if (faction === "Navy") {
            if (character.currentLocation !== "Navy Outpost Aqua") {
                throw new functions.https.HttpsError("failed-precondition", "You must be at the Navy Outpost Aqua to enlist in the Navy.");
            }
            transaction.update(playerRef, { faction: "Navy", rank: "Navy Recruit" });
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
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processHealing(character);

        if (character.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to train. Visit an infirmary.");
        }

        if (character.trainingState) {
             throw new functions.https.HttpsError("failed-precondition", "You are already training.");
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
        if (character.gold < TRAINING_GOLD_COST) throw new functions.https.HttpsError("failed-precondition", "Not enough gold.");

        const endTime = Date.now() + TRAINING_DURATION_MS;

        transaction.update(playerRef, {
            energy: energy - 10,
            energyUpdatedAt,
            gold: admin.firestore.FieldValue.increment(-TRAINING_GOLD_COST),
            trainingState: { endTime, statType }
        });

        recordLog(transaction, userId, "TrainStart", `Started training ${statType}`, -TRAINING_GOLD_COST, 0);

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

        // Ensure stats object exists
        const stats = { ...character.stats };
        const pStats = { ...(character.professionStats || {}) };

        if (stats[mappedStat] !== undefined) {
            stats[mappedStat] = (stats[mappedStat] || 0) + 1;
        } else if (pStats[mappedStat] !== undefined) {
            pStats[mappedStat] = (pStats[mappedStat] || 0) + 1;
        } else {
            // Fallback for new stats
            const combatStats = ["swordsmanship", "brawling", "gunslinging", "spear", "martialArts", "sniper", "mysticArts"];
            if (combatStats.includes(mappedStat)) {
                stats[mappedStat] = (stats[mappedStat] || 0) + 1;
            } else {
                pStats[mappedStat] = (pStats[mappedStat] || 0) + 1;
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
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processHealing(character);

        if (character.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to travel. Visit an infirmary.");
        }

        if (character.healingState) {
            throw new functions.https.HttpsError("failed-precondition", "You cannot travel while resting in the infirmary.");
        }

        if (character.travelState || character.combatState || character.trainingState) {
            throw new functions.https.HttpsError("failed-precondition", "Player is already busy.");
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

        const travelDuration = calculateTravelTime(character.currentLocation, destination, speedMultiplier * (1.0 / travelMultiplier));
        const arrivalTime = Date.now() + travelDuration;

        // Potential for random encounter here (Pirates, Monsters)
        if (travelDuration > 10000 && Math.random() < 0.25) {
            const enemy = generateEnemy(character.level);

            transaction.update(playerRef, {
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
        }

        transaction.update(playerRef, {
            travelState: { destination, arrivalTime, startTime: Date.now() }
        });
        return { success: true, arrivalTime, travelDuration };
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

        const character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

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
        { name: "Sea Serpent", minLevel: 1, maxLevel: 10, dropTableId: "basic_sea_loot" },
        { name: "Pirate Scout", minLevel: 3, maxLevel: 15, dropTableId: "basic_sea_loot" },
        { name: "Giant Squid", minLevel: 10, maxLevel: 25, dropTableId: "rare_sea_loot" },
        { name: "Ghost Pirate", minLevel: 15, maxLevel: 40, dropTableId: "rare_sea_loot" },
        { name: "Feral Crab", minLevel: 1, maxLevel: 5, dropTableId: "basic_sea_loot" },
        { name: "Rogue Sloop", minLevel: 5, maxLevel: 20, dropTableId: "basic_sea_loot" },
        { name: "Navy Enforcer", minLevel: 8, maxLevel: 30, dropTableId: "rare_sea_loot" }
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

function calculateDamage(attackerStats: CombatStats, defenderStats: CombatStats, attackerEffects: any[], defenderEffects: any[], isCrit: boolean, attackerCombatType?: string, defenderMythicArt?: any, attackerMythicArt?: any): number {
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
    if (attackerMythicArt && attackerMythicArt.element && defenderMythicArt && defenderMythicArt.elementalWeaknesses) {
        if (defenderMythicArt.elementalWeaknesses.includes(attackerMythicArt.element)) {
            damage *= 1.5; // 50% more damage if elemental advantage
        }
    }

    let defense = defenderStats.defense;
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

        let combat = character.combatState;
        if (!combat || combat.isFinished) throw new functions.https.HttpsError("failed-precondition", "No active combat.");

        // Turn Timeout Check
        const now = Date.now();
        if (combat.turnExpiresAt && now > combat.turnExpiresAt) {
            combat.logs.push(`${combat.playerTurn ? "You" : "Opponent"} took too long! Forfeiting turn.`);
            combat.playerTurn = !combat.playerTurn;
            combat.turnExpiresAt = now + TURN_TIMEOUT_MS;
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
                    const hitChance = pStats.accuracy - targetStats.dodge;

                    if (hitRoll < hitChance) {
                        const isCrit = Math.random() * 100 < pStats.critChance;
                        const attackerCombatType = getHighestCombatSkill(character);
                        const defenderMythicArt = combat.isPvP ? opponent.mythicArt : enemy.mythicArt;
                        const attackerMythicArt = character.mythicArt;
                        const damage = calculateDamage(pStats, targetStats, pActiveEffects, targetEffects, isCrit, attackerCombatType, defenderMythicArt, attackerMythicArt);

                        if (isCrit) logs.push(`CRITICAL! You strike for ${damage} damage!`);
                        else logs.push(`You hit for ${damage} damage.`);

                        if (combat.isPvP) opponent.hp = Math.max(0, opponent.hp - damage);
                        else enemy.hp = Math.max(0, enemy.hp - damage);
                    } else {
                        logs.push("You missed your attack.");
                    }
                } else if (action === "Technique") {
                    if (!techniqueId) throw new functions.https.HttpsError("invalid-argument", "Missing technique ID.");

                    // Technique validation
                    if (!character.learnedTechniques || !character.learnedTechniques.includes(techniqueId)) {
                        throw new functions.https.HttpsError("failed-precondition", "You have not learned this technique.");
                    }

                    const techSnap = await db.collection("gameData").doc("skills").collection("techniques").doc(techniqueId).get();
                    if (!techSnap.exists) throw new functions.https.HttpsError("not-found", "Technique not found.");

                    const tech = techSnap.data() as any;
                    if (playerEnergy < tech.energyCost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");
                    if ((combat.cooldowns || {})[techniqueId] > 0) throw new functions.https.HttpsError("failed-precondition", "Technique on cooldown.");

                    playerEnergy -= tech.energyCost;
                    const cooldowns = { ...(combat.cooldowns || {}) };
                    cooldowns[techniqueId] = tech.cooldown;
                    combat.cooldowns = cooldowns;

                    const mappedTechSkill = STAT_MAPPING[tech.type] || "strength";
                    const techSkillVal = (pStats as any)[mappedTechSkill] || pStats.strength;
                    let techDamage = Math.floor(techSkillVal * tech.power * 2);

                    // Apply weakness to technique damage
                    const defenderMythicArt = combat.isPvP ? opponent.mythicArt : enemy.mythicArt;
                    if (defenderMythicArt && defenderMythicArt.weakAgainst && tech.type) {
                        if (defenderMythicArt.weakAgainst.includes(tech.type)) {
                            techDamage = Math.floor(techDamage * 1.5);
                        }
                    }

                    techDamage = Math.max(1, Math.floor(techDamage - targetStats.defense * 0.3));

                    if (combat.isPvP) {
                        opponent.hp = Math.max(0, opponent.hp - techDamage);
                        if (tech.effects) {
                            opponent.combatState.playerEffects = [...(opponent.combatState.playerEffects || []), ...tech.effects];
                        }
                    } else {
                        enemy.hp = Math.max(0, enemy.hp - techDamage);
                        if (tech.effects) {
                            combat.enemyEffects = [...(combat.enemyEffects || []), ...tech.effects];
                        }
                    }
                    logs.push(`You use ${tech.name}! Target takes ${techDamage} damage.`);
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
                    const healAmount = item.healAmount || 30;
                    playerHp = Math.min(character.maxHp, playerHp + healAmount);
                    logs.push(`You used ${item.name} and recovered ${healAmount} HP.`);

                    // Remove item from inventory
                    character.inventory.splice(itemIndex, 1);
                } else if (action === "Flee") {
                    if (combat.isPvP) throw new functions.https.HttpsError("failed-precondition", "Cannot flee from a duel.");
                    const fleeChance = 40 + (pStats.agility - targetStats.agility) * 2;
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
                            const abilityDamage = Math.floor(eStats.strength * 2.5);
                            playerHp = Math.max(0, playerHp - abilityDamage);
                            logs.push(`${enemy.name} uses a special ability and strikes you for ${abilityDamage} damage!`);
                        } else {
                            const eHitRoll = Math.random() * 100;
                            const eHitChance = eStats.accuracy - pStats.dodge;
                            if (eHitRoll < eHitChance) {
                                const attackerCombatType = getHighestCombatSkill(enemy);
                                const defenderMythicArt = character.mythicArt;
                                const attackerMythicArt = enemy.mythicArt;
                                let eDamage = calculateDamage(eStats, pStats, eActiveEffects, pActiveEffects, false, attackerCombatType, defenderMythicArt, attackerMythicArt);
                                if (combat.defending) {
                                    eDamage = Math.floor(eDamage * 0.5);
                                    combat.defending = false;
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

                const stealAmount = Math.floor(loser.gold * 0.15);
                const collectedBounty = loser.bounty || 0;
                const totalGoldGained = stealAmount + collectedBounty;

                // Update Winner
                const winnerUpdate: any = {
                    gold: winner.gold + totalGoldGained,
                    pvpWins: (winner.pvpWins || 0) + 1,
                    combatState: null,
                    hp: playerWon ? playerHp : winner.hp,
                    energy: playerWon ? playerEnergy : winner.energy
                };

                // Only Pirates increase their bounty through victory
                if (winner.faction === "Pirate") {
                    winnerUpdate.bounty = (winner.bounty || 0) + 100;
                }

                if (playerWon) winnerUpdate.inventory = character.inventory;
                transaction.update(winnerRef, winnerUpdate);

                // Update Winner's Crew Bounty
                if (winner.crewId && winner.faction === "Pirate") {
                    transaction.update(db.collection("crews").doc(winner.crewId), {
                        totalBounty: admin.firestore.FieldValue.increment(100)
                    });
                }

                // Update Loser
                const loserUpdate: any = {
                    gold: loser.gold - stealAmount,
                    bounty: 0, // Bounty is collected, reset it
                    pvpLosses: (loser.pvpLosses || 0) + 1,
                    combatState: null,
                    hp: playerWon ? 0 : playerHp, // Ensure loser ends with 0 HP if playerWon, otherwise take attacker's HP
                    energy: playerWon ? loser.energy : playerEnergy,
                    currentLocation: "Fogi Tail Island" // Respawn
                };
                if (!playerWon) loserUpdate.inventory = character.inventory;
                transaction.update(loserRef, loserUpdate);

                recordLog(transaction, winner.id, "PvPWin", `Defeated ${loser.name} and collected ${collectedBounty}B bounty`, totalGoldGained, 0);
                recordLog(transaction, loser.id, "PvPLoss", `Lost to ${winner.name}`, -stealAmount, 0);

                return { success: true, isFinished: true, playerWon, bountyCollected: collectedBounty };
            } else {
                // PvE Finish Logic
                let updatedChar = { ...character, hp: playerHp, energy: playerEnergy, combatState: null };
                const loot = playerWon && enemy.dropTableId ? await processLoot(enemy.dropTableId) : [];

                if (playerWon) {
                    updatedChar.gold += enemy.goldReward;
                    updatedChar.xp += enemy.xpReward;
                    if (loot.length > 0) {
                        const currentInv = updatedChar.inventory || [];
                        const freeSlots = INVENTORY_CAPACITY - currentInv.length;
                        const lootToAdd = loot.slice(0, Math.max(0, freeSlots));

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
                transaction.update(playerRef, updatedChar);
                return { success: true, isFinished: true, playerWon, logs };
            }
        } else {
            // Update combat state and swap turns if PvP
            if (combat.isPvP) {
                const nextLogs = [...logs, `It is now ${opponent.name}'s turn.`];
                const updatedEnemy = { ...combat.enemy, hp: opponent.hp };

                transaction.update(playerRef, {
                    hp: playerHp,
                    energy: playerEnergy,
                    inventory: character.inventory,
                    combatState: { ...combat, enemy: updatedEnemy, playerTurn: false, logs: nextLogs, cooldowns: updatedCooldowns, playerEffects: pActiveEffects, turnExpiresAt: now + TURN_TIMEOUT_MS }
                });

                const opponentCombat = {
                    ...opponent.combatState,
                    enemy: { ...opponent.combatState.enemy, hp: playerHp }, // Sync attacker's HP to defender
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
                    inventory: character.inventory,
                    combatState: { ...combat, enemy, logs, turnCount: (combat.turnCount || 0) + 1, cooldowns: updatedCooldowns, playerEffects: pActiveEffects }
                });
            }
            return { success: true, logs };
        }
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
        attacker = processHealing(attacker);

        if (attacker.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to fight. Visit an infirmary.");
        }

        const defender = defenderSnap.data() as any;

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

        if (attacker.combatState || attacker.travelState) throw new functions.https.HttpsError("failed-precondition", "You are already busy.");
        if (defender.combatState || defender.travelState) throw new functions.https.HttpsError("failed-precondition", "Target is already busy.");

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

        transaction.update(attackerRef, attacker); // Apply processHealing, infamy and faction changes
        transaction.update(attackerRef, { combatState: attackerCombat });
        transaction.update(defenderRef, { combatState: defenderCombat });

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
        const character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

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
        const character = snapshot.data() as any;
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        if (character.hp >= character.maxHp) throw new functions.https.HttpsError("failed-precondition", "You are already at full health.");
        if (character.gold < 50) throw new functions.https.HttpsError("failed-precondition", "Not enough gold for instant treatment.");

        // Check if current location has an infirmary or a camp
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const hasHealingAction = location?.actions?.some((a: any) => a.type === "Infirmary" || a.type === "Camp");
        if (!hasHealingAction) throw new functions.https.HttpsError("failed-precondition", "There is no infirmary or camp at your current location.");

        transaction.update(playerRef, {
            hp: character.maxHp,
            gold: admin.firestore.FieldValue.increment(-50),
            healingState: null
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
        const character = snapshot.data() as any;

        if (character.hasMedicalLicense) throw new functions.https.HttpsError("already-exists", "You already have a medical license.");
        if (character.gold < 15000) throw new functions.https.HttpsError("failed-precondition", "Not enough gold (15,000 required).");

        if (character.mythicArt && !character.mythicArt.canLearnNonCombatSkills) {
            throw new functions.https.HttpsError("failed-precondition", "Your Mythic Art forbids practicing medicine.");
        }

        transaction.update(playerRef, {
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
        const healer = healerSnap.data() as any;
        const target = targetSnap.data() as any;

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
        const newHp = Math.min(target.maxHp, target.hp + healAmount);

        const targetUpdate: any = { hp: newHp };
        if (newHp >= target.maxHp) {
            targetUpdate.healingState = null;
        }

        const healerUpdate: any = {
            xp: admin.firestore.FieldValue.increment(10),
            "professionStats.medical": admin.firestore.FieldValue.increment(1)
        };

        transaction.update(targetRef, targetUpdate);
        transaction.update(healerRef, healerUpdate);

        recordLog(transaction, userId, "HealPlayer", `Healed ${target.name} for ${healAmount} HP`, 0, 10);
        return { success: true, healedAmount: healAmount, fullyHealed: newHp >= target.maxHp };
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
        if (character.isBanned) throw new functions.https.HttpsError("permission-denied", "User is banned.");

        character = processHealing(character);

        if (character.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to hunt monsters. Visit a camp.");
        }

        if (character.travelState || character.combatState || character.trainingState || character.healingState) {
            throw new functions.https.HttpsError("failed-precondition", "Player is already busy.");
        }

        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        if (location?.isSafe) {
            throw new functions.https.HttpsError("failed-precondition", "There are no monsters to hunt in safe zones.");
        }

        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);
        if (energy < 5) throw new functions.https.HttpsError("failed-precondition", "Not enough energy (5 required).");

        const enemy = generateEnemy(character.level);

        transaction.update(playerRef, {
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

        character = processHealing(character);

        if (character.hp <= 0) {
            throw new functions.https.HttpsError("failed-precondition", "You are too injured to go on missions. Visit an infirmary.");
        }

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

    return db.runTransaction(async (transaction) => {
        const [playerSnap, crewSnap] = await Promise.all([
            transaction.get(playerRef),
            transaction.get(crewRef)
        ]);

        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");
        if (!crewSnap.exists) throw new functions.https.HttpsError("not-found", "Crew not found.");

        const character = playerSnap.data() as any;
        const crew = crewSnap.data() as any;

        if (character.crewId) throw new functions.https.HttpsError("already-exists", "Player is already in a crew.");
        if (crew.members.length >= 20) throw new functions.https.HttpsError("resource-exhausted", "Crew is full.");

        transaction.update(crewRef, {
            members: admin.firestore.FieldValue.arrayUnion(userId),
            totalBounty: admin.firestore.FieldValue.increment(character.bounty)
        });
        transaction.update(playerRef, { crewId: crewId });

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

        character = processHealing(character);

        const updates: any = {
            lastOnline: Date.now(),
            isOnline: true,
            hp: character.hp,
            healingState: character.healingState
        };

        // ADMIN GOLD BOOST
        const admins = ["sedna", "von"];
        const charNameLower = (character.nameLower || character.name || "").toLowerCase();

        if (admins.includes(charNameLower) && (character.gold || 0) < 900000000) {
            updates.gold = 900000000;
        }

        // TEST MYTHICS FOR ADMINS
        if (admins.includes(charNameLower)) {
            const hasHighTier = (character.inventory || []).some((i: any) => i.id.startsWith("test_artifact_SSS_"));
            if (!hasHighTier) {
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
                updates.inventory = admin.firestore.FieldValue.arrayUnion(...testItems);
            }
        }

        // Rank Repair Logic: Ensure faction members have their correct starting rank if it was lost
        if (character.faction === "Navy" && (character.rank === "Novice Sailor" || !character.rank)) {
            updates.rank = "Navy Recruit";
        } else if (character.faction === "Pirate" && (character.rank === "Novice Sailor" || !character.rank)) {
            updates.rank = "Rogue Sailor";
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
        { name: "Fogi Tail Island", region: "East Blue", description: "A peaceful starting island with clear blue waters.", isSafe: true, weather: "Sunny", x: 0, y: 0, actions: [{ type: "Training", label: "Dojo", icon: "🥋" }, { type: "Kitchen", label: "Galley", icon: "🍳" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Medical Clinic", icon: "🏥" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Ironcrest Isle", region: "East Blue", description: "A rocky island known for its iron mines and blacksmiths.", isSafe: false, weather: "Foggy", x: 640, y: 160, actions: [{ type: "Forge", label: "Grand Forge", icon: "⚒" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Sunken Reef", region: "East Blue", description: "A shallow reef area teeming with colorful fish and hidden treasures.", isSafe: false, weather: "Clear", x: 280, y: 360, actions: [{ type: "Fishing", label: "Fishing Spot", icon: "🎣" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Shadow Fen", region: "East Blue", description: "A murky swamp island filled with dangerous creatures.", isSafe: false, weather: "Overcast", x: -1200, y: -400, actions: [{ type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Tortuga Bay", region: "South Blue", description: "A bustling pirate haven filled with taverns and mystery.", isSafe: false, weather: "Tropical", x: 120, y: -840, actions: [{ type: "Tavern", label: "The Salty Dog", icon: "🍻" }, { type: "Market", label: "Bazaar", icon: "💰" }, { type: "Expedition", label: "Treasure Hunt", icon: "💎" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Pirate Doctor", icon: "🏥" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Pirate's Den", region: "South Blue", description: "An outlaw stronghold hidden within jagged cliffs.", isSafe: false, weather: "Stormy", x: 1400, y: -1400, actions: [{ type: "Arena", label: "Duel Pit", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Kraken's Rest", region: "South Blue", description: "A desolate island graveyard of sunken ships and sea monsters.", isSafe: false, weather: "Stormy", x: -1600, y: -1600, actions: [{ type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Aqua", region: "South Blue", description: "A strictly regulated military base maintaining order.", isSafe: false, weather: "Clear", x: -640, y: -440, actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Infirmary", label: "Navy Hospital", icon: "🏥" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Terra", region: "Grand Line", description: "A frontier navy post watching over the Grand Line entrance.", isSafe: false, weather: "Windy", x: -1200, y: 800, actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Navy Outpost Ignis", region: "Grand Line", description: "A strategic outpost near the volcanic islands.", isSafe: false, weather: "Hot", x: 1600, y: 1200, actions: [{ type: "Bounties", label: "Bounty Board", icon: "📜" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Training", label: "Dojo", icon: "🥋" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Crystal Cove", region: "Grand Line", description: "An island made of glowing crystals and mysterious energy.", isSafe: false, weather: "Shimmering", x: 1120, y: 480, actions: [{ type: "BlackMarket", label: "Crystal Trader", icon: "💎" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Volcano Peak", region: "Grand Line", description: "An active volcano island with treacherous terrain.", isSafe: false, weather: "Ashy", x: 1680, y: 960, actions: [{ type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Whispering Woods", region: "Grand Line", description: "A dense forest where the trees seem to whisper secrets.", isSafe: false, weather: "Mist", x: -600, y: 720, actions: [{ type: "Cave", label: "Ancient Grotto", icon: "🕳" }, { type: "Observatory", label: "Star Gazing", icon: "🔭" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Serpent's Maw", region: "Grand Line", description: "A terrifying island shaped like a giant serpent's head.", isSafe: false, weather: "Foggy", x: 2000, y: 2000, actions: [{ type: "Camp", label: "Wilderness Camp", icon: "⛺" }, { type: "Grind", label: "Monster Hunt", icon: "⚔" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] },
        { name: "Island of World Secrets", region: "Unknown", description: "A mystical island shrouded in secrets. Here, you can roll for Mythic Arts.", isSafe: true, weather: "Celestial", x: 4000, y: 4000, actions: [{ type: "MythicRoll", label: "Ancient Altar", icon: "✨" }, { type: "Docks", label: "Docks", icon: "⛵" }, { type: "Shipyard", label: "Shipyard", icon: "🏗" }] }
    ];

    for (const loc of locations) {
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
        { id: "rusty_cutlass", name: "Rusty Cutlass", description: "An old, worn-out sword.", type: "Weapon", rarity: "Common", price: 50, levelRequirement: 1, statBonus: { strength: 2 } },
        { id: "old_boots", name: "Old Boots", description: "Waterlogged but still wearable.", type: "Armor", rarity: "Common", price: 40, levelRequirement: 1, statBonus: { endurance: 2 } },
        { id: "pearl", name: "Pearl", description: "A rare and valuable gem from a Giant Squid.", type: "Miscellaneous", rarity: "Rare", price: 200 },
        // Artifacts
        { id: "artifact_f", name: "Shattered Slate (F)", description: "A common artifact containing a faint whisper of power.", type: "Artifact", rarity: "Common", price: 1000, mythicTier: "F" },
        { id: "artifact_e", name: "Rusty Relic (E)", description: "A simple relic that holds basic knowledge.", type: "Artifact", rarity: "Common", price: 5000, mythicTier: "E" },
        { id: "artifact_d", name: "Ancient Shard (D)", description: "A shard from a bygone era, pulsating with energy.", type: "Artifact", rarity: "Uncommon", price: 20000, mythicTier: "D" },
        { id: "artifact_c", name: "Glowing Core (C)", description: "A core of energy that contains specialized techniques.", type: "Artifact", rarity: "Uncommon", price: 100000, mythicTier: "C" },
        { id: "artifact_b", name: "Jade Idol (B)", description: "A beautifully crafted idol that resonates with your spirit.", type: "Artifact", rarity: "Rare", price: 500000, mythicTier: "B" },
        { id: "artifact_a", name: "Dragon Scale (A)", description: "A scale from a legendary dragon, containing immense power.", type: "Artifact", rarity: "Rare", price: 2000000, mythicTier: "A" },
        { id: "artifact_s", name: "Phoenix Feather (S)", description: "A feather that never stops burning with mythical energy.", type: "Artifact", rarity: "Epic", price: 10000000, mythicTier: "S" },
        { id: "artifact_ss", name: "God's Tear (SS)", description: "A crystalline tear said to fall from the heavens.", type: "Artifact", rarity: "Epic", price: 50000000, mythicTier: "SS" },
        { id: "artifact_sss", name: "Void Essence (SSS)", description: "The pure essence of the void. The pinnacle of power.", type: "Artifact", rarity: "Legendary", price: 250000000, mythicTier: "SSS" }
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
                { itemId: "fish_scales", chance: 0.5, minAmount: 2, maxAmount: 5 }
            ]
        }
    ];

    for (const table of lootTables) {
        const ref = db.collection("gameData").doc("world").collection("lootTables").doc(table.id);
        batch.set(ref, table);
    }

    await batch.commit();
    return { success: true, message: `SUCCESS_V4: 15 islands seeded. Safety zones updated.` };
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

        const character = snapshot.data() as any;
        const inventory = character.inventory || [];
        const item = inventory.find((i: any) => i.id === itemId);

        if (!item) throw new functions.https.HttpsError("not-found", "Item not found in inventory.");
        if (item.type !== slot) throw new functions.https.HttpsError("invalid-argument", `Item cannot be equipped in ${slot} slot.`);

        // Level Requirement Check
        if (character.level < (item.levelRequirement || 1)) {
            throw new functions.https.HttpsError("failed-precondition", "Level too low to equip this item.");
        }

        const equipment = character.equipment || {};
        equipment[slot] = item;

        transaction.update(playerRef, { equipment });
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

        const character = snapshot.data() as any;
        const equipment = character.equipment || {};
        delete equipment[slot];

        transaction.update(playerRef, { equipment });
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

        const character = playerSnap.data() as any;
        const item = itemSnap.data() as any;

        // Validate location has a market
        const locationSnap = await transaction.get(db.collection("gameData").doc("world").collection("locations").doc(character.currentLocation));
        const location = locationSnap.data();
        const marketAction = location?.actions?.find((a: any) => a.type === "Market" || a.type === "BlackMarket");

        if (!marketAction) {
            throw new functions.https.HttpsError("failed-precondition", "There is no market at your current location.");
        }

        // shopId validation (simplified for now, but enforces the concept)
        if (shopId === "BlackMarket" && marketAction.type !== "BlackMarket") {
             throw new functions.https.HttpsError("failed-precondition", "This item is only available in a Black Market.");
        }

        if (character.gold < item.price) {
            throw new functions.https.HttpsError("failed-precondition", "Not enough gold.");
        }

        const inventory = character.inventory || [];
        if (inventory.length >= INVENTORY_CAPACITY) {
            throw new functions.https.HttpsError("failed-precondition", "Inventory is full.");
        }

        const newInventory = [...inventory, { ...item, id: `${item.id}_${Date.now()}` }];

        transaction.update(playerRef, {
            gold: character.gold - item.price,
            inventory: newInventory
        });

        recordLog(transaction, userId, "PurchaseItem", `Purchased ${item.name}`, -item.price, 0);
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

        const character = snapshot.data() as any;

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

        const character = snapshot.data() as any;
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

            // Remove old techniques from previous Mythic Art if they exist
            let newTechniques = [...learnedTechniques];
            if (character.mythicArt && character.mythicArt.techniques) {
                const oldTechs = character.mythicArt.techniques;
                newTechniques = newTechniques.filter((tech: string) => !oldTechs.includes(tech));
            }

            // Add new techniques
            if (mythic.techniques) {
                for (const tech of mythic.techniques) {
                    if (!newTechniques.includes(tech)) {
                        newTechniques.push(tech);
                    }
                }
            }

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
                restrictedSkillTypes: mythic.restrictedSkillTypes || []
            };

            inventory.splice(itemIndex, 1);

            transaction.update(playerRef, {
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

        inventory.splice(itemIndex, 1);

        transaction.update(playerRef, {
            hp: playerHp,
            inventory
        });

        recordLog(transaction, userId, "UseItem", `Used ${item.name}`, 0, 0);
        return { success: true, newHp: playerHp };
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
        transaction.update(playerRef, { crewId: null });

        return { success: true };
    });
});

export const inviteToCrew = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const senderId = context.auth.uid;

    const senderSnap = await db.collection("players").doc(senderId).get();
    const sender = senderSnap.data() as any;
    if (!sender.crewId) throw new functions.https.HttpsError("failed-precondition", "You are not in a crew.");

    const inviteRef = db.collection("crewInvites").doc(`${sender.crewId}_${targetId}`);
    await inviteRef.set({
        crewId: sender.crewId,
        crewName: (await db.collection("crews").doc(sender.crewId).get()).data()?.name,
        senderId,
        targetId,
        status: "pending",
        timestamp: Date.now()
    });

    return { success: true };
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

        const character = playerSnap.data() as any;
        const crew = crewSnap.data() as any;

        if (character.crewId) throw new functions.https.HttpsError("already-exists", "Already in a crew.");
        if (crew.members.length >= 20) throw new functions.https.HttpsError("resource-exhausted", "Crew is full.");

        transaction.update(crewRef, {
            members: admin.firestore.FieldValue.arrayUnion(userId),
            totalBounty: admin.firestore.FieldValue.increment(character.bounty)
        });
        transaction.update(playerRef, { crewId });
        transaction.update(inviteRef, { status: "accepted" });

        return { success: true, accepted: true };
    });
});

export const markMailAsRead = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    await db.collection("players").doc(userId).collection("mail").doc(mailId).update({ isRead: true });
    return { success: true };
});

export const deleteMail = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    const { mailId } = data;
    const userId = context.auth.uid;
    await db.collection("players").doc(userId).collection("mail").doc(mailId).delete();
    return { success: true };
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

        const updates: any = {
            gold: character.gold + (rewards.gold || 0),
            xp: character.xp + (rewards.xp || 0),
        };

        if (rewards.items) {
             const inventory = character.inventory || [];
             if (inventory.length + rewards.items.length > INVENTORY_CAPACITY) {
                  throw new functions.https.HttpsError("resource-exhausted", "Inventory full.");
             }
             updates.inventory = [...inventory, ...rewards.items.map((i: any) => ({ ...i, id: `${i.id}_${Date.now()}_${Math.random()}` }))];
        }

        transaction.update(playerRef, updates);
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
