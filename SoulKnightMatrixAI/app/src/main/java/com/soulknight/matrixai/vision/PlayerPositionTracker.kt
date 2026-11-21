package com.soulknight.matrixai.vision

import kotlin.math.abs

data class TrackedPlayerPosition(
    val x: Int,
    val y: Int,
    val confidence: Float,
    val timestampMillis: Long
)

/**
 * Lightweight patch tracker. The initial player-centred patch is selected by the user
 * from a captured frame; subsequent frames are searched locally around the last position.
 * This is a tracking primitive, not a trained Soul Knight character detector.
 */
class PlayerPositionTracker(
    private val patchRadius: Int = 6,
    private val searchRadius: Int = 42,
    private val minimumConfidence: Float = 0.78f
) {
    private var template: IntArray? = null
    private var lastX = -1
    private var lastY = -1

    init {
        require(patchRadius >= 2)
        require(searchRadius >= 1)
        require(minimumConfidence in 0f..1f)
    }

    fun initialize(frame: FrameSnapshot, playerCenterX: Int, playerCenterY: Int): Boolean {
        if (!containsPatch(frame, playerCenterX, playerCenterY)) return false
        template = extractPatch(frame, playerCenterX, playerCenterY)
        lastX = playerCenterX
        lastY = playerCenterY
        return true
    }

    fun track(frame: FrameSnapshot): TrackedPlayerPosition? {
        val reference = template ?: return null
        val minX = (lastX - searchRadius).coerceAtLeast(patchRadius)
        val maxX = (lastX + searchRadius).coerceAtMost(frame.width - patchRadius - 1)
        val minY = (lastY - searchRadius).coerceAtLeast(patchRadius)
        val maxY = (lastY + searchRadius).coerceAtMost(frame.height - patchRadius - 1)
        if (minX > maxX || minY > maxY) return null

        var bestX = -1
        var bestY = -1
        var bestError = Double.MAX_VALUE
        for (y in minY..maxY step SEARCH_STEP) {
            for (x in minX..maxX step SEARCH_STEP) {
                val error = patchError(frame, x, y, reference)
                if (error < bestError) {
                    bestError = error
                    bestX = x
                    bestY = y
                }
            }
        }
        if (bestX < 0) return null
        val confidence = (1.0 - bestError / MAX_CHANNEL_ERROR).toFloat().coerceIn(0f, 1f)
        if (confidence < minimumConfidence) return null
        lastX = bestX
        lastY = bestY
        return TrackedPlayerPosition(bestX, bestY, confidence, frame.timestampMillis)
    }

    fun reset() {
        template = null
        lastX = -1
        lastY = -1
    }

    private fun containsPatch(frame: FrameSnapshot, x: Int, y: Int) =
        x - patchRadius >= 0 && y - patchRadius >= 0 &&
            x + patchRadius < frame.width && y + patchRadius < frame.height

    private fun extractPatch(frame: FrameSnapshot, cx: Int, cy: Int): IntArray {
        val size = patchRadius * 2 + 1
        return IntArray(size * size) { index ->
            val x = cx + index % size - patchRadius
            val y = cy + index / size - patchRadius
            frame.pixel(x, y) ?: 0
        }
    }

    private fun patchError(frame: FrameSnapshot, cx: Int, cy: Int, reference: IntArray): Double {
        val size = patchRadius * 2 + 1
        var error = 0L
        var i = 0
        for (dy in -patchRadius..patchRadius) for (dx in -patchRadius..patchRadius) {
            val a = reference[i++]
            val b = frame.pixel(cx + dx, cy + dy) ?: return Double.MAX_VALUE
            error += abs(((a shr 16) and 255) - ((b shr 16) and 255))
            error += abs(((a shr 8) and 255) - ((b shr 8) and 255))
            error += abs((a and 255) - (b and 255))
        }
        return error.toDouble() / (size * size * 3)
    }

    companion object {
        private const val SEARCH_STEP = 2
        private const val MAX_CHANNEL_ERROR = 255.0
    }
}
