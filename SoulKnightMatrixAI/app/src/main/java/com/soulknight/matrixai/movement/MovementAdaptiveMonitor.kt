package com.soulknight.matrixai.movement

/** Updates the speed profile when a new measured segment differs from the estimate. */
class MovementAdaptiveMonitor(
    private val calibrator: MovementCalibrator = MovementCalibrator(),
    private val changeThreshold: Float = 0.12f
) {
    init { require(changeThreshold in 0f..1f) }

    fun updateIfChanged(
        profile: MovementProfile,
        sample: MovementSample,
        nowMillis: Long = System.currentTimeMillis()
    ): MovementProfile {
        val measured = calibrator.measure(sample)
        val expected = when (sample.direction) {
            MovementDirection.HORIZONTAL -> profile.horizontalPixelsPerSecond
            MovementDirection.VERTICAL -> profile.verticalPixelsPerSecond
            MovementDirection.DIAGONAL -> profile.diagonalPixelsPerSecond
        }
        if (expected <= 0f || kotlin.math.abs(measured - expected) / expected >= changeThreshold) {
            return calibrator.update(profile, sample, nowMillis)
        }
        return profile
    }
}
