const admin = require("firebase-admin");

// Try to initialize with explicit project ID
try {
    admin.initializeApp({
        projectId: "a-life-at-sea-mmo"
    });
} catch (e) {
    // If already initialized or failed, ignore
}

const db = admin.firestore();

async function addGold() {
    console.log("Searching for character 'Sedna'...");
    const snapshot = await db.collection("players").where("nameLower", "==", "sedna").get();

    if (snapshot.empty) {
        console.log("No character found with name 'Sedna'.");
        process.exit(1);
    }

    const doc = snapshot.docs[0];
    const character = doc.data();
    console.log(`Found character: ${character.name} (ID: ${doc.id})`);
    console.log(`Current Gold: ${character.gold}`);

    const newGold = (character.gold || 0) + 900000000;
    await doc.ref.update({ gold: newGold });

    console.log(`Successfully updated gold! New balance: ${newGold}`);
    process.exit(0);
}

addGold().catch(err => {
    console.error("Error:", err);
    process.exit(1);
});
