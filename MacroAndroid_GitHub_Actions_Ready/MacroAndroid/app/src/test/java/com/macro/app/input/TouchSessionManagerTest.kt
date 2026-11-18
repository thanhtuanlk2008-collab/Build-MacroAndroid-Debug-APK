package com.macro.app.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchSessionManagerTest {
    private val manager = TouchSessionManager()

    @Test
    fun physicalAndMacroSessionsAreTrackedSeparately() {
        val physical = TouchSessionKey(TouchSource.PHYSICAL_OBSERVED, 0)
        val macro = TouchSessionKey(TouchSource.MACRO, 0)
        val sample = TouchSample(10f, 20f, 100L)

        assertTrue(manager.begin(physical, sample))
        assertTrue(manager.begin(macro, sample))
        assertEquals(2, manager.activeCount())
        assertEquals(1, manager.activeCount(TouchSource.PHYSICAL_OBSERVED))
        assertEquals(1, manager.activeCount(TouchSource.MACRO))
    }

    @Test
    fun duplicateBeginIsRejectedAndEndRemovesOnlyMatchingSession() {
        val physical = TouchSessionKey(TouchSource.PHYSICAL_OBSERVED, 2)
        val macro = TouchSessionKey(TouchSource.MACRO, 2)
        val sample = TouchSample(1f, 2f, 3L)

        assertTrue(manager.begin(physical, sample))
        assertFalse(manager.begin(physical, sample))
        assertTrue(manager.begin(macro, sample))
        assertEquals(sample, manager.end(physical))
        assertNull(manager.end(physical))
        assertEquals(1, manager.activeCount())
        assertEquals(1, manager.activeCount(TouchSource.MACRO))
    }

    @Test
    fun cancellingOneSourceDoesNotRemoveTheOtherSource() {
        val physical = TouchSessionKey(TouchSource.PHYSICAL_OBSERVED, 0)
        val macro = TouchSessionKey(TouchSource.MACRO, 0)
        val sample = TouchSample(5f, 6f, 7L)

        manager.begin(physical, sample)
        manager.begin(macro, sample)
        manager.cancel(TouchSource.MACRO)

        assertEquals(1, manager.activeCount())
        assertEquals(1, manager.activeCount(TouchSource.PHYSICAL_OBSERVED))
        assertEquals(0, manager.activeCount(TouchSource.MACRO))
    }
}
