package com.soulknight.matrixai.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementAdaptiveMonitorTest {
    @Test fun ignoresSmallSpeedVariation() {
        val monitor = MovementAdaptiveMonitor(changeThreshold = 0.12f)
        val base = MovementProfile(100f, 100f, 120f, confidence = 0.8f, updatedAtMillis = 10L)
        val result = monitor.updateIfChanged(
            base,
            MovementSample(0f, 0f, 108f, 0f, 1000L, MovementDirection.HORIZONTAL),
            20L
        )
        assertEquals(base, result)
    }

    @Test fun updatesProfileWhenSpeedChangesBeyondThreshold() {
        val monitor = MovementAdaptiveMonitor(changeThreshold = 0.12f)
        val base = MovementProfile(100f, 100f, 120f, confidence = 0.8f, updatedAtMillis = 10L)
        val result = monitor.updateIfChanged(
            base,
            MovementSample(0f, 0f, 160f, 0f, 1000L, MovementDirection.HORIZONTAL),
            20L
        )
        assertTrue(result.horizontalPixelsPerSecond > base.horizontalPixelsPerSecond)
        assertEquals(20L, result.updatedAtMillis)
    }
}
