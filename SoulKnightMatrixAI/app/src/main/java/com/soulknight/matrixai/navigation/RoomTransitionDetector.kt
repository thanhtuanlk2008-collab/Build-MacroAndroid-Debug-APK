package com.soulknight.matrixai.navigation

import com.soulknight.matrixai.vision.FrameSnapshot

/** Detects a likely room transition from a coarse frame signature, not map identity. */
class RoomTransitionDetector(private val minimumChangedSamples: Int = 18) {
    private var previous: IntArray? = null
    init { require(minimumChangedSamples > 0) }

    fun hasEnteredNewRoom(frame: FrameSnapshot): Boolean {
        val current = signature(frame)
        val old = previous
        previous = current
        if (old == null || old.size != current.size) return true
        var changed = 0
        for (i in current.indices) if (colourDistance(old[i], current[i]) >= COLOUR_CHANGE_THRESHOLD) changed++
        return changed >= minimumChangedSamples
    }

    fun reset() { previous = null }

    private fun signature(frame: FrameSnapshot): IntArray {
        val result = IntArray(SIGNATURE_SIZE * SIGNATURE_SIZE)
        for (gy in 0 until SIGNATURE_SIZE) for (gx in 0 until SIGNATURE_SIZE) {
            val x = ((gx + 0.5f) * frame.width / SIGNATURE_SIZE).toInt().coerceIn(0, frame.width - 1)
            val y = ((gy + 0.5f) * frame.height / SIGNATURE_SIZE).toInt().coerceIn(0, frame.height - 1)
            result[gy * SIGNATURE_SIZE + gx] = frame.pixel(x, y) ?: 0
        }
        return result
    }

    private fun colourDistance(a: Int, b: Int): Int =
        kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255)) +
            kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255)) +
            kotlin.math.abs((a and 255) - (b and 255))

    companion object { private const val SIGNATURE_SIZE = 12; private const val COLOUR_CHANGE_THRESHOLD = 90 }
}
