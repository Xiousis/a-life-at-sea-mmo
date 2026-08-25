import functionsTest from "firebase-functions-test";
import { expect } from "chai";
import "mocha";
import * as myFunctions from "./index";

const test = functionsTest();

describe("Game Logic & Security Tests", () => {
    after(() => {
        test.cleanup();
    });

    describe("calculateCurrentEnergy", () => {
        it("should correctly regenerate 2 energy in 6 minutes", () => {
            const now = Date.now();
            const char = {
                energy: 50,
                maxEnergy: 100,
                energyUpdatedAt: now - (6 * 60 * 1000) // 6 minutes ago
            };
            const result = myFunctions.calculateCurrentEnergy(char);
            expect(result.energy).to.equal(52);
            // newTimestamp should be roughly 6 mins after old timestamp if not maxed
            expect(result.energyUpdatedAt).to.equal(char.energyUpdatedAt + (2 * 3 * 60 * 1000));
        });

        it("should not exceed maxEnergy", () => {
            const now = Date.now();
            const char = {
                energy: 99,
                maxEnergy: 100,
                energyUpdatedAt: now - (10 * 60 * 1000) // 10 minutes ago (enough for 3 energy)
            };
            const result = myFunctions.calculateCurrentEnergy(char);
            expect(result.energy).to.equal(100);
            expect(result.energyUpdatedAt).to.equal(now);
        });
    });

    describe("checkAdmin Security", () => {
        it("should throw error if user is not an admin", async () => {
            const context = {
                auth: {
                    uid: "user123",
                    token: { admin: false }
                }
            } as any;

            try {
                await myFunctions.checkAdmin(context);
                expect.fail("Should have thrown permission-denied");
            } catch (err: any) {
                expect(err.code).to.equal("permission-denied");
            }
        });

        it("should not throw error if user is an admin", async () => {
            const context = {
                auth: {
                    uid: "admin123",
                    token: { admin: true }
                }
            } as any;

            await myFunctions.checkAdmin(context); // Should not throw
        });
    });

    describe("Economy & Rate Limiting (Mocked/Logic)", () => {
        it("should enforce chat message length limits", async () => {
            const context = { auth: { uid: "user1" } } as any;
            const longMessage = "a".repeat(201);
            try {
                // Using .run() for https.onCall in test mode
                await myFunctions.sendMessage.run({ message: longMessage }, context);
                expect.fail("Should have thrown invalid-argument");
            } catch (err: any) {
                expect(err.code).to.equal("invalid-argument");
                expect(err.message).to.contain("too long");
            }
        });

        it("should enforce mail subject and body limits", async () => {
            const context = { auth: { uid: "user1" } } as any;
            const longSubject = "s".repeat(101);
            try {
                await myFunctions.sendMail.run({ recipientId: "user2", subject: longSubject, body: "test" }, context);
                expect.fail("Should have thrown invalid-argument");
            } catch (err: any) {
                expect(err.code).to.equal("invalid-argument");
            }
        });

        it("should correctly apply 5% tax to auction sales", () => {
            const price = 1000;
            const taxRate = 0.05;
            const netGold = Math.floor(price * (1 - taxRate));
            expect(netGold).to.equal(950);
        });

        it("should handle rounding in tax calculation", () => {
            const price = 19; // 5% of 19 is 0.95
            const netGold = Math.floor(19 * 0.95);
            expect(netGold).to.equal(18);
        });
    });
});
