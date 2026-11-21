package com.soulknight.matrixai.input

/**
 * Converts a normalized direction into a short, bounded virtual-joystick drag.
 * The caller must get explicit user confirmation and supply the actual screen bounds.
 */
class JoystickController(private val input: InputController) {
    fun move(
        center: InputTarget,
        directionX: Float,
        directionY: Float,
        radiusPixels: Float,
        durationMillis: Long,
        screenWidth: Int,
        screenHeight: Int,
        callback: (InputResult) -> Unit = {}
    ): Long {
        if (!center.isValid() || !directionX.isFinite() || !directionY.isFinite() ||
            !radiusPixels.isFinite() || radiusPixels !in MIN_RADIUS..MAX_RADIUS ||
            screenWidth <= 0 || screenHeight <= 0
        ) {
            callback(InputResult.InvalidCoordinates)
            return -1L
        }
        if (durationMillis !in MIN_DURATION_MS..MAX_DURATION_MS) {
            callback(InputResult.InvalidDuration)
            return -1L
        }
        val magnitude = kotlin.math.sqrt(directionX * directionX + directionY * directionY)
        if (magnitude == 0f || magnitude > 1.001f) {
            callback(InputResult.InvalidCoordinates)
            return -1L
        }
        val dx = directionX / magnitude * radiusPixels
        val dy = directionY / magnitude * radiusPixels
        val endX = center.x + dx
        val endY = center.y + dy
        if (center.x >= screenWidth || center.y >= screenHeight ||
            endX < 0f || endX >= screenWidth.toFloat() ||
            endY < 0f || endY >= screenHeight.toFloat()
        ) {
            callback(InputResult.InvalidCoordinates)
            return -1L
        }
        return input.swipe(center.x, center.y, endX, endY, durationMillis, callback)
    }

    companion object {
        const val MIN_RADIUS = 8f
        const val MAX_RADIUS = 180f
        const val MIN_DURATION_MS = 80L
        const val MAX_DURATION_MS = 600L
    }
}
