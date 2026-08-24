package com.alifeatseammo

import com.alifeatseammo.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DerivedStatsTest {

    @Test
    fun testDerivedStatsCalculation() {
        val stats = Stats(
            perception = 10.0,
            luck = 10.0,
            agility = 10.0,
            endurance = 10.0,
            willpower = 10.0
        )
        val character = Character(stats = stats)
        val derived = character.getDerivedStats()

        // critChance: (10 * 0.1) + (10 * 0.05) = 1.0 + 0.5 = 1.5
        assertEquals(1.5, derived.criticalChance, 0.001)
        
        // dodgeChance: 10 * 0.15 = 1.5
        assertEquals(1.5, derived.dodgeChance, 0.001)
        
        // blockEffectiveness: 10 * 0.2 = 2.0
        assertEquals(2.0, derived.blockEffectiveness, 0.001)
        
        // manaRegenPerSecond: 10 * 0.01 = 0.1
        assertEquals(0.1, derived.manaRegenPerSecond, 0.001)
    }

    @Test
    fun testElementalStatsPersistence() {
        val stats = Stats(
            elementalResistances = mapOf(ElementType.Fire to 10.0, ElementType.Water to -5.0),
            elementalMastery = mapOf(ElementType.Lightning to 15.0)
        )
        
        assertEquals(10.0, stats.elementalResistances[ElementType.Fire] ?: 0.0, 0.001)
        assertEquals(-5.0, stats.elementalResistances[ElementType.Water] ?: 0.0, 0.001)
        assertEquals(15.0, stats.elementalMastery[ElementType.Lightning] ?: 0.0, 0.001)
        assertEquals(0.0, stats.elementalMastery[ElementType.Fire] ?: 0.0, 0.001)
    }
}
