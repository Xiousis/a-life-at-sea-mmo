package com.alifeatseammo

import com.alifeatseammo.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class GameLogicTest {

    @Test
    fun testLevelUpCalculation() {
        // Level 1 needs 1*1*100 = 100 XP
        val character = Character(level = 1, xp = 100)
        val leveledUp = character.checkLevelUp()
        
        assertEquals(2, leveledUp.level)
        assertEquals(0, leveledUp.xp)
        assertEquals(120, leveledUp.maxHp) // 100 + 20
        assertEquals(100, leveledUp.maxEnergy) // No energy increase until level 5
    }

    @Test
    fun testMultiLevelUp() {
        // Level 1: 100 XP -> Level 2
        // Level 2: 400 XP -> Level 3
        // Total needed for Level 3: 500 XP
        val character = Character(level = 1, xp = 550)
        val leveledUp = character.checkLevelUp()
        
        assertEquals(3, leveledUp.level)
        assertEquals(50, leveledUp.xp)
        assertEquals(140, leveledUp.maxHp) // 100 + 20*2
    }

    @Test
    fun testEnergyRegen() {
        val now = System.currentTimeMillis()
        val regenRate = 3 * 60 * 1000L // 3 minutes per energy
        
        // Character with 50 energy, updated 6 minutes ago
        val character = Character(
            energy = 50,
            maxEnergy = 100,
            energyUpdatedAt = now - (regenRate * 2)
        )
        
        // We can't easily mock System.currentTimeMillis() without additional tools,
        // but since getCurrentEnergy() uses it, we can test that it regens at least 2 energy
        // if we set the updated time in the past.
        val currentEnergy = character.getCurrentEnergy()
        
        assertEquals(52, currentEnergy)
    }

    @Test
    fun testEnergyRegenWithMythicArt() {
        val now = System.currentTimeMillis()
        val baseRegenRate = 3 * 60 * 1000L
        
        // Mythic art with 2x regen speed
        val mythicArt = MythicArt(energyRegainMultiplier = 2.0f)
        val character = Character(
            energy = 50,
            maxEnergy = 100,
            energyUpdatedAt = now - baseRegenRate, // 3 mins ago
            mythicArt = mythicArt
        )
        
        // With 2x regen, 3 minutes should give 2 energy instead of 1
        val currentEnergy = character.getCurrentEnergy()
        assertEquals(52, currentEnergy)
    }
    
    @Test
    fun testMaxLevelCap() {
        val character = Character(level = 300, xp = 1000000)
        val capped = character.checkLevelUp()
        
        assertEquals(300, capped.level)
        assertEquals(0, capped.xp)
    }
}
