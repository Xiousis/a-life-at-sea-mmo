import * as functionsTest from "firebase-functions-test";
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
});
