package com.soulknight.matrixai.vision

import kotlin.math.max
import kotlin.math.min

data class BiomeColorResult(
    val biome: com.soulknight.matrixai.matrix.MatrixBiome,
    val glacierScore: Float,
    val relicsScore: Float,
    val sampledPixels: Int
)

/**
 * Layout-independent biome baseline. It compares broad HSV colour proportions,
 * not room geometry or pixel-perfect map templates. Callers should crop out HUD.
 */
class BiomeColorAnalyzer(private val stride: Int = 4) {
    init { require(stride > 0) }

    fun analyze(frame: FrameSnapshot, region: ScreenRegion = ScreenRegion(0, 0, frame.width, frame.height)): BiomeColorResult {
        val area = region.clamp(frame.width, frame.height)
        var glacier = 0
        var relics = 0
        var count = 0
        for (y in area.top until area.bottom step stride) for (x in area.left until area.right step stride) {
            val pixel = frame.pixel(x, y) ?: continue
            val r = ((pixel shr 16) and 255) / 255f
            val g = ((pixel shr 8) and 255) / 255f
            val b = (pixel and 255) / 255f
            val hsv = rgbToHsv(r, g, b)
            val h = hsv.first
            val s = hsv.second
            val v = hsv.third
            if (s < 0.12f || v < 0.12f) continue
            count++
            // Cold blue/cyan colours and bright snow/ice tones.
            if ((h >= 165f && h <= 255f && s >= 0.16f) || (v >= 0.78f && s < 0.30f)) glacier++
            // Sandy yellow, ochre and warm stone colours.
            if (h in 12f..58f && s >= 0.20f && v >= 0.22f) relics++
        }
        val glacierScore = if (count == 0) 0f else glacier.toFloat() / count
        val relicsScore = if (count == 0) 0f else relics.toFloat() / count
        val biome = when {
            count == 0 -> com.soulknight.matrixai.matrix.MatrixBiome.UNKNOWN
            glacierScore >= MIN_SCORE && glacierScore > relicsScore * SCORE_MARGIN -> com.soulknight.matrixai.matrix.MatrixBiome.GLACIER
            relicsScore >= MIN_SCORE && relicsScore > glacierScore * SCORE_MARGIN -> com.soulknight.matrixai.matrix.MatrixBiome.RELICS
            max(glacierScore, relicsScore) < MIN_SCORE -> com.soulknight.matrixai.matrix.MatrixBiome.OTHER
            else -> com.soulknight.matrixai.matrix.MatrixBiome.UNKNOWN
        }
        return BiomeColorResult(biome, glacierScore, relicsScore, count)
    }

    private fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val hi = max(r, max(g, b)); val lo = min(r, min(g, b)); val d = hi - lo
        val hue = when {
            d == 0f -> 0f
            hi == r -> 60f * (((g - b) / d) % 6f)
            hi == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return Triple(hue, if (hi == 0f) 0f else d / hi, hi)
    }

    companion object { const val MIN_SCORE = 0.18f; const val SCORE_MARGIN = 1.15f }
}
