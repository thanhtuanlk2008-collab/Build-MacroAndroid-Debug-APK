package com.soulknight.matrixai.macro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroConfigTest {
    @Test
    fun validatedClampsIntervalsToSupportedRanges() {
        val config = MacroConfig(
            attackIntervalMs = 1L,
            skillIntervalMs = 90_000L,
            switchWeaponIntervalMs = 10L
        ).validated()

        assertEquals(100L, config.attackIntervalMs)
        assertEquals(60_000L, config.skillIntervalMs)
        assertEquals(500L, config.switchWeaponIntervalMs)
    }

    @Test
    fun actionFlagsAreIndependent() {
        val config = MacroConfig(
            attackEnabled = true,
            skillEnabled = false,
            switchWeaponEnabled = true
        )

        assertTrue(config.isEnabled(MacroAction.ATTACK))
        assertFalse(config.isEnabled(MacroAction.USE_SKILL))
        assertTrue(config.isEnabled(MacroAction.SWITCH_WEAPON))
    }
}
