package com.soulknight.matrixai.vision

/**
 * Lightweight deterministic frame statistics used to validate the perception pipeline.
 * This is not an enemy/player detector; semantic recognition is a later milestone.
 */
data class FrameStatistics(
    val averageRed: Double,
    val averageGreen: Double,
    val averageBlue: Double,
    val sampledPixels: Int
)

class FrameAnalyzer(private val sampleStride: Int = 12) {
    init { require(sampleStride > 0) }

    fun analyze(frame: FrameSnapshot, region: ScreenRegion? = null): FrameStatistics {
        val area = (region ?: ScreenRegion(0, 0, frame.width, frame.height))
            .clamp(frame.width, frame.height)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0
        for (y in area.top until area.bottom step sampleStride) {
            for (x in area.left until area.right step sampleStride) {
                val color = frame.pixel(x, y) ?: continue
                red += (color shr 16) and 0xff
                green += (color shr 8) and 0xff
                blue += color and 0xff
                count++
            }
        }
        if (count == 0) return FrameStatistics(0.0, 0.0, 0.0, 0)
        return FrameStatistics(red.toDouble() / count, green.toDouble() / count, blue.toDouble() / count, count)
    }
}
