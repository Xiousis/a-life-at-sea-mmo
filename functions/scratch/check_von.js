const admin = require("firebase-admin");

try {
    admin.initializeApp({
        projectId: "a-life-at-sea-mmo"
    });
} catch (e) {}

const db = admin.firestore();

async function checkPlayer() {
    console.log("Searching for character with name/nameLower 'von'...");
    const snap1 = await db.collection("players").where("nameLower", "==", "von").get();
    const snap2 = await db.collection("players").where("name", "==", "Von").get();

    if (snap1.empty && snap2.empty) {
        console.log("No character found with name 'Von' or 'von'.");

        // Let's list some characters to see what's there
        const all = await db.collection("players").limit(5).get();
        console.log("Existing characters sample:");
        all.forEach(d => console.log(` - ${d.data().name} (${d.data().nameLower})`));

        process.exit(1);
    }

    const doc = snap1.empty ? snap2.docs[0] : snap1.docs[0];
    const data = doc.data();
    console.log("PLAYER DATA FOUND:");
    console.log(JSON.stringify(data, null, 2));
    process.exit(0);
}

checkPlayer().catch(err => {
    console.error("Error:", err);
    process.exit(1);
});
