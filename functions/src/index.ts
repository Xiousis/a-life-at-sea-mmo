import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// --- Constants & Config ---
const ENERGY_REGEN_RATE_MS = 3 * 60 * 1000; // 1 energy per 3 minutes
const MAX_ENERGY = 100;
const INVENTORY_CAPACITY = 20;

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
    "DualBlades": "dualBlades"
};

const LOCATION_DATA: Record<string, { x: number, y: number, region: string }> = {
    "Fogi Tail Island": { x: 0, y: 0, region: "East Blue" },
    "Ironcrest Isle": { x: 50, y: 20, region: "East Blue" },
    "Amber Reach": { x: -30, y: 40, region: "East Blue" },
    "Tortuga Bay": { x: 10, y: -100, region: "South Blue" },
    "Crystal Cove": { x: 120, y: 80, region: "Grand Line" },
};

function calculateTravelTime(from: string, to: string): number {
    const start = LOCATION_DATA[from] || { x: 0, y: 0, region: "Unknown" };
    const end = LOCATION_DATA[to] || { x: 0, y: 0, region: "Unknown" };

    const dist = Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
    let baseTime = dist * 2000; // 1 unit = 2 seconds

    if (start.region !== end.region) {
        baseTime += 60000; // Extra minute for inter-region travel
    }

    return Math.max(10000, Math.floor(baseTime)); // Min 10 seconds
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
    let xpNeeded = level * 100;

    let leveledUp = false;
    while (xp >= xpNeeded) {
        level++;
        xp -= xpNeeded;
        maxEnergy += 5;
        energy = maxEnergy;
        stats.endurance += 1;
        maxHp = 50 + (stats.endurance * 10);
        hp = maxHp;

        stats.strength += 1;
        stats.agility += 1;
        stats.perception += 1;
        stats.willpower += 1;
        stats.luck += 1;

        xpNeeded = level * 100;
        leveledUp = true;
    }

    return { ...character, level, xp, stats, maxEnergy, energy, maxHp, hp, leveledUp };
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
    const allowedRaces = ["Human", "Fishman", "Mink", "Skypiean", "Cyborg"];
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
        hp: 100,
        maxHp: 100,
        energy: 100,
        maxEnergy: 100,
        energyUpdatedAt: Date.now(),
        lastOnline: Date.now(),
        isOnline: true,
        currentLocation: "Fogi Tail Island",
        title: "Novice Sailor",
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
            dualBlades: 0
        },
        inventory: [],
        equipment: {},
        travelState: null,
        combatState: null
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
            transaction.update(playerRef, { faction: "Pirate", title: "Rogue Sailor" });
            recordLog(transaction, userId, "JoinFaction", "Became a Pirate", 0, 0);
        } else if (faction === "Navy") {
            transaction.update(playerRef, { faction: "Navy", title: "Navy Recruit" });
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

        const character = snapshot.data() as any;
        const { energy, energyUpdatedAt } = calculateCurrentEnergy(character);

        if (energy < 10) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");

        // Ensure stats object exists
        const stats = { ...character.stats };
        stats[mappedStat] = (stats[mappedStat] || 0) + 1;

        const updatedChar = { ...character, energy: energy - 10, energyUpdatedAt, xp: character.xp + 5, stats };

        const finalChar = checkLevelUp(updatedChar);
        transaction.set(playerRef, finalChar);
        recordLog(transaction, userId, "Train", `Trained ${statType}`, 0, 5);

        return { success: true, newEnergy: finalChar.energy };
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

        const character = snapshot.data() as any;
        if (character.travelState || character.combatState) {
            throw new functions.https.HttpsError("failed-precondition", "Player is already busy.");
        }

        if (character.currentLocation === destination) {
            throw new functions.https.HttpsError("invalid-argument", "Already at destination.");
        }

        if (!LOCATION_DATA[destination]) {
            throw new functions.https.HttpsError("invalid-argument", "Invalid destination.");
        }

        const travelDuration = calculateTravelTime(character.currentLocation, destination);
        const arrivalTime = Date.now() + travelDuration;

        // Potential for random encounter here
        if (travelDuration > 20000 && Math.random() < 0.20) {
            const enemy = generateEnemy(character.level);
            transaction.update(playerRef, {
                combatState: {
                    enemy: enemy,
                    playerTurn: true,
                    logs: [`While traveling to ${destination}, you were ambushed by a ${enemy.name}!`],
                    isFinished: false,
                    playerWon: false,
                    turnCount: 0
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

        const character = snapshot.data() as any;
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

// --- Combat Engine ---

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
    dualBlades?: number;
    defense: number;
    accuracy: number;
    dodge: number;
    critChance: number;
}

function calculateCombatStats(charOrEnemy: any): CombatStats {
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

    // Derived stats
    const defense = Math.floor(endurance * 1.5 + level);
    const accuracy = 80 + agility * 0.5 + perception * 0.5;
    const dodge = agility * 0.8 + luck * 0.2;
    const critChance = 5 + luck * 0.5 + perception * 0.2;

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
        dualBlades: stats.dualBlades || 0,
        defense,
        accuracy,
        dodge,
        critChance
    };
}

function generateEnemy(playerLevel: number) {
    const names = ["Sea Serpent", "Pirate Scout", "Feral Crab", "Ghost Ship", "Navy Enforcer"];
    const name = names[Math.floor(Math.random() * names.length)];
    const level = Math.max(1, playerLevel + Math.floor(Math.random() * 3) - 1);

    const stats = {
        strength: 5 + level * 2,
        endurance: 5 + level * 2,
        agility: 5 + level,
        perception: 5 + level,
        willpower: 5 + level,
        luck: 5 + level / 2
    };

    const maxHp = 40 + (level * 15);

    // Basic drop table logic for random encounters
    const dropTableId = level > 5 ? "basic_loot" : null;

    return {
        name,
        level,
        hp: maxHp,
        maxHp: maxHp,
        stats,
        goldReward: 20 * level,
        xpReward: 15 * level,
        dropTableId
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
                droppedItems.push({ ...item, id: `${item.id}_${Date.now()}_${Math.floor(Math.random() * 1000)}` });
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
            case "Weaken":
                // Handled during damage calculation
                break;
            case "Fortify":
                // Handled during defense calculation
                break;
        }

        if (effect.duration > 1) {
            activeEffects.push({ ...effect, duration: effect.duration - 1 });
        }
    }

    return { character: updatedChar, activeEffects };
}

function calculateDamage(attackerStats: CombatStats, defenderStats: CombatStats, attackerEffects: any[], defenderEffects: any[], isCrit: boolean): number {
    let damage = attackerStats.strength * 2 + (attackerStats.swordsmanship || 0) * 1.5;

    // Apply Weaken effect
    if (attackerEffects.some(e => e.type === "Weaken")) {
        damage *= 0.7;
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
        let combat = character.combatState;
        if (!combat || combat.isFinished) throw new functions.https.HttpsError("failed-precondition", "No active combat.");
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
            const pStats = calculateCombatStats(character);
            const targetStats = combat.isPvP ? calculateCombatStats(opponent) : calculateCombatStats(enemy);
            const targetEffects = combat.isPvP ? (opponent.combatState?.playerEffects || []) : (combat.enemyEffects || []);

            const isStunned = (pActiveEffects || []).some((e: any) => e.type === "Stun");

            if (!isStunned) {
                // --- Player Action ---
                if (action === "Attack") {
                    const hitRoll = Math.random() * 100;
                    const hitChance = pStats.accuracy - targetStats.dodge;

                    if (hitRoll < hitChance) {
                        const isCrit = Math.random() * 100 < pStats.critChance;
                        const damage = calculateDamage(pStats, targetStats, pActiveEffects, targetEffects, isCrit);

                        if (isCrit) logs.push(`CRITICAL! You strike for ${damage} damage!`);
                        else logs.push(`You hit for ${damage} damage.`);

                        if (combat.isPvP) opponent.hp = Math.max(0, opponent.hp - damage);
                        else enemy.hp = Math.max(0, enemy.hp - damage);
                    } else {
                        logs.push("You missed your attack.");
                    }
                } else if (action === "Technique") {
                    const techSnap = await db.collection("gameData").doc("skills").collection("techniques").doc(techniqueId).get();
                    if (!techSnap.exists) throw new functions.https.HttpsError("not-found", "Technique not found.");

                    const tech = techSnap.data() as any;
                    if (playerEnergy < tech.energyCost) throw new functions.https.HttpsError("failed-precondition", "Not enough energy.");
                    if ((combat.cooldowns || {})[techniqueId] > 0) throw new functions.https.HttpsError("failed-precondition", "Technique on cooldown.");

                    playerEnergy -= tech.energyCost;
                    const cooldowns = { ...(combat.cooldowns || {}) };
                    cooldowns[techniqueId] = tech.cooldown;
                    combat.cooldowns = cooldowns;

                    let techDamage = Math.floor(pStats.strength * tech.power * 2);
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
                    const inventory = character.inventory || [];
                    const itemIndex = inventory.findIndex((i: any) => i.id === itemId);
                    if (itemIndex === -1) throw new functions.https.HttpsError("not-found", "Item not found.");

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
                const eStats = calculateCombatStats(enemy);
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
                                let eDamage = calculateDamage(eStats, pStats, eActiveEffects, pActiveEffects, false);
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

                const stealAmount = Math.floor(loser.gold * 0.1);

                // Update Winner
                const winnerUpdate: any = {
                    gold: winner.gold + stealAmount,
                    bounty: (winner.bounty || 0) + 100,
                    pvpWins: (winner.pvpWins || 0) + 1,
                    combatState: null,
                    hp: playerWon ? playerHp : winner.hp,
                    energy: playerWon ? playerEnergy : winner.energy
                };
                if (playerWon) winnerUpdate.inventory = character.inventory;
                transaction.update(winnerRef, winnerUpdate);

                // Update Winner's Crew Bounty
                if (winner.crewId) {
                    transaction.update(db.collection("crews").doc(winner.crewId), {
                        totalBounty: admin.firestore.FieldValue.increment(100)
                    });
                }

                // Update Loser
                const loserUpdate: any = {
                    gold: loser.gold - stealAmount,
                    pvpLosses: (loser.pvpLosses || 0) + 1,
                    combatState: null,
                    hp: playerWon ? loser.hp : playerHp,
                    energy: playerWon ? loser.energy : playerEnergy,
                    currentLocation: "Fogi Tail Island" // Respawn
                };
                if (!playerWon) loserUpdate.inventory = character.inventory;
                transaction.update(loserRef, loserUpdate);

                recordLog(transaction, winner.id, "PvPWin", `Defeated ${loser.name}`, stealAmount, 0);
                recordLog(transaction, loser.id, "PvPLoss", `Lost to ${winner.name}`, -stealAmount, 0);

                return { success: true, isFinished: true, playerWon };
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
                    recordLog(transaction, userId, "CombatWin", `Defeated ${enemy.name}`, enemy.goldReward, enemy.xpReward);
                } else {
                    const goldLost = Math.floor(updatedChar.gold * 0.1);
                    updatedChar.gold -= goldLost;
                    updatedChar.currentLocation = "Fogi Tail Island";
                    updatedChar.hp = character.maxHp;
                    updatedChar.energy = character.maxEnergy;
                    recordLog(transaction, userId, "CombatLoss", `Defeated by ${enemy.name}`, -goldLost, 0);
                }
                transaction.set(playerRef, updatedChar);
                return { success: true, isFinished: true, playerWon, logs };
            }
        } else {
            // Update combat state and swap turns if PvP
            if (combat.isPvP) {
                const nextLogs = [...logs, `It is now ${opponent.name}'s turn.`];
                // Unified opponent HP in enemy field for UI
                const updatedEnemy = { ...combat.enemy, hp: opponent.hp };

                transaction.update(playerRef, {
                    hp: playerHp,
                    energy: playerEnergy,
                    inventory: character.inventory,
                    combatState: { ...combat, enemy: updatedEnemy, playerTurn: false, logs: nextLogs, cooldowns: updatedCooldowns, playerEffects: pActiveEffects }
                });

                const opponentCombat = {
                    ...opponent.combatState,
                    enemy: { ...opponent.combatState.enemy, hp: playerHp }, // Sync attacker's HP to defender
                    playerTurn: true,
                    logs: nextLogs
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

        const attacker = attackerSnap.data() as any;
        const defender = defenderSnap.data() as any;

        if (userId === defenderId) {
            throw new functions.https.HttpsError("invalid-argument", "You cannot attack yourself.");
        }

        if (attacker.currentLocation !== defender.currentLocation) {
            throw new functions.https.HttpsError("failed-precondition", "Target is not at your location.");
        }

        if (attacker.combatState || attacker.travelState) {
            throw new functions.https.HttpsError("failed-precondition", "You are already busy.");
        }

        if (defender.combatState || defender.travelState) {
            throw new functions.https.HttpsError("failed-precondition", "Target is already busy.");
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
            cooldowns: {}
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
            cooldowns: {}
        };

        transaction.update(attackerRef, { combatState: attackerCombat });
        transaction.update(defenderRef, { combatState: defenderCombat });

        return { success: true };
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

        const character = playerSnap.data() as any;
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
        transaction.set(playerRef, updatedChar);
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
    await db.collection("players").doc(userId).update({
        lastOnline: Date.now(),
        isOnline: true
    });
    return { success: true };
});

// --- Admin Tools ---

export async function checkAdmin(context: functions.https.CallableContext) {
    if (!context.auth?.token.admin) {
        throw new functions.https.HttpsError("permission-denied", "User is not an admin.");
    }
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

// --- Social Functions ---

export const sendFriendRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");

    const { targetId } = data;
    const senderId = context.auth.uid;

    if (senderId === targetId) throw new functions.https.HttpsError("invalid-argument", "Cannot add yourself.");

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
        const hasMarket = location?.actions?.some((a: any) => a.type === "Market" || a.type === "BlackMarket");

        if (!hasMarket) {
            throw new functions.https.HttpsError("failed-precondition", "There is no market at your current location.");
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
        const playerSnap = await transaction.get(playerRef);
        if (!playerSnap.exists) throw new functions.https.HttpsError("not-found", "Character not found.");

        const character = playerSnap.data() as any;
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
