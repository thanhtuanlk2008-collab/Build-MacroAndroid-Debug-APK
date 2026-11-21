package com.soulknight.matrixai.movement

import kotlin.math.hypot

data class MovementSample(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val elapsedMillis: Long,
    val direction: MovementDirection
)

enum class MovementDirection { HORIZONTAL, VERTICAL, DIAGONAL }

class MovementCalibrator(private val smoothing: Float = 0.35f) {
    init { require(smoothing in 0f..1f) }

    fun measure(sample: MovementSample): Float {
        require(sample.elapsedMillis > 0)
        val distance = when (sample.direction) {
            MovementDirection.HORIZONTAL -> kotlin.math.abs(sample.endX - sample.startX)
            MovementDirection.VERTICAL -> kotlin.math.abs(sample.endY - sample.startY)
            MovementDirection.DIAGONAL -> hypot(sample.endX - sample.startX, sample.endY - sample.startY)
        }
        return distance * 1000f / sample.elapsedMillis
    }

    fun update(previous: MovementProfile, sample: MovementSample, nowMillis: Long = System.currentTimeMillis()): MovementProfile {
        val measured = measure(sample)
        fun blend(old: Float) = if (old <= 0f) measured else old * (1f - smoothing) + measured * smoothing
        val next = when (sample.direction) {
            MovementDirection.HORIZONTAL -> previous.copy(horizontalPixelsPerSecond = blend(previous.horizontalPixelsPerSecond))
            MovementDirection.VERTICAL -> previous.copy(verticalPixelsPerSecond = blend(previous.verticalPixelsPerSecond))
            MovementDirection.DIAGONAL -> previous.copy(diagonalPixelsPerSecond = blend(previous.diagonalPixelsPerSecond))
        }
        return next.copy(confidence = (previous.confidence + 0.2f).coerceAtMost(1f), updatedAtMillis = nowMillis)
    }
}
