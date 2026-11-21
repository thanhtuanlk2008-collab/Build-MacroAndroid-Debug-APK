package com.soulknight.matrixai.movement

import org.junit.Assert.assertEquals
import org.junit.Test

class MovementProfileTest {
    @Test fun keepsHorizontalVerticalAndDiagonalSpeedsSeparate() {
        val calibrator = MovementCalibrator(smoothing = 1f)
        var profile = MovementProfile.UNCALIBRATED
        profile = calibrator.update(profile, MovementSample(0f, 0f, 80f, 0f, 400L, MovementDirection.HORIZONTAL), 400L)
        profile = calibrator.update(profile, MovementSample(0f, 0f, 0f, 60f, 400L, MovementDirection.VERTICAL), 800L)
        profile = calibrator.update(profile, MovementSample(0f, 0f, 60f, 60f, 400L, MovementDirection.DIAGONAL), 1200L)
        assertEquals(200f, profile.horizontalPixelsPerSecond, 0.001f)
        assertEquals(150f, profile.verticalPixelsPerSecond, 0.001f)
        assertEquals(kotlin.math.hypot(60f, 60f) * 2.5f, profile.diagonalPixelsPerSecond, 0.01f)
    }
}
