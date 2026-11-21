package com.soulknight.matrixai.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementCalibratorTest {
    @Test fun measuresSpeedAndCalculatesTravelDuration() {
        val calibrator = MovementCalibrator()
        val sample = MovementSample(0f, 0f, 100f, 0f, 1000L, MovementDirection.HORIZONTAL)
        val profile = calibrator.update(MovementProfile.UNCALIBRATED, sample, 10L)
        assertEquals(100f, profile.horizontalPixelsPerSecond, 0.001f)
        assertEquals(1000L, profile.durationForDistance(100f))
    }

    @Test fun adaptsAfterSpeedBuff() {
        val monitor = MovementAdaptiveMonitor(changeThreshold = 0.1f)
        val base = MovementProfile(100f, 100f, 120f, confidence = 0.8f)
        val buffed = monitor.updateIfChanged(
            base, MovementSample(0f, 0f, 200f, 0f, 1000L, MovementDirection.HORIZONTAL), 20L
        )
        assertTrue(buffed.horizontalPixelsPerSecond > base.horizontalPixelsPerSecond)
    }
}
