package com.soulknight.matrixai.movement

data class MovementProfile(
    val horizontalPixelsPerSecond: Float,
    val verticalPixelsPerSecond: Float,
    val diagonalPixelsPerSecond: Float,
    val responseDelayMillis: Long = 0L,
    val stopDelayMillis: Long = 0L,
    val confidence: Float = 0f,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(horizontalPixelsPerSecond >= 0f && horizontalPixelsPerSecond.isFinite())
        require(verticalPixelsPerSecond >= 0f && verticalPixelsPerSecond.isFinite())
        require(diagonalPixelsPerSecond >= 0f && diagonalPixelsPerSecond.isFinite())
        require(responseDelayMillis >= 0 && stopDelayMillis >= 0)
        require(confidence in 0f..1f)
    }

    fun durationForDistance(distancePixels: Float, diagonal: Boolean = false): Long {
        require(distancePixels >= 0f && distancePixels.isFinite())
        val speed = if (diagonal) diagonalPixelsPerSecond else horizontalPixelsPerSecond
        if (speed <= 0f) return 0L
        return (distancePixels / speed * 1000f).toLong().coerceAtLeast(1L)
    }

    companion object { val UNCALIBRATED = MovementProfile(0f, 0f, 0f) }
}
