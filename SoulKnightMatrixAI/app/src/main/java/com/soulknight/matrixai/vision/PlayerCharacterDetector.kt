package com.soulknight.matrixai.vision

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class PlayerDetectionStatus { WARMING_UP, DETECTED, AMBIGUOUS, NOT_FOUND }

data class SpriteCandidate(
    val centerX: Int,
    val centerY: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    /** Visual candidate score, not a calibrated probability of being the player. */
    val score: Float,
    val changedSamples: Int
)

data class PlayerDetectionResult(
    val status: PlayerDetectionStatus,
    val candidate: SpriteCandidate? = null,
    val alternatives: List<SpriteCandidate> = emptyList(),
    val timestampMillis: Long = 0L,
    val message: String
)

/**
 * Finds moving sprite-like regions without a user-supplied screen coordinate.
 *
 * This is a first-stage, image-only candidate detector. Frame differencing and
 * appearance/shape features cannot semantically distinguish the controlled hero
 * from enemies or animated effects in every scene. It therefore reports AMBIGUOUS
 * rather than silently selecting a weak candidate.
 */
class PlayerCharacterDetector(
    private val sampleStep: Int = 2,
    private val pixelDifferenceThreshold: Int = 34,
    private val minComponentSamples: Int = 5,
    private val maxCandidates: Int = 8
) {
    private var previous: FrameSnapshot? = null
    private var lastCenterX: Int? = null
    private var lastCenterY: Int? = null

    init {
        require(sampleStep >= 1)
        require(pixelDifferenceThreshold in 1..765)
        require(minComponentSamples >= 1)
        require(maxCandidates >= 1)
    }

    @Synchronized
    fun analyze(frame: FrameSnapshot): PlayerDetectionResult {
        val old = previous
        previous = frame
        if (old == null || old.width != frame.width || old.height != frame.height) {
            lastCenterX = null
            lastCenterY = null
            return PlayerDetectionResult(
                PlayerDetectionStatus.WARMING_UP,
                timestampMillis = frame.timestampMillis,
                message = "Đang lấy khung hình nền; cần thêm một khung hình để so sánh chuyển động."
            )
        }

        val gridWidth = (frame.width + sampleStep - 1) / sampleStep
        val gridHeight = (frame.height + sampleStep - 1) / sampleStep
        val changed = BooleanArray(gridWidth * gridHeight)
        for (gy in 0 until gridHeight) {
            val y = (gy * sampleStep).coerceAtMost(frame.height - 1)
            for (gx in 0 until gridWidth) {
                val x = (gx * sampleStep).coerceAtMost(frame.width - 1)
                val a = old.pixel(x, y) ?: continue
                val b = frame.pixel(x, y) ?: continue
                val difference =
                    abs(((a shr 16) and 255) - ((b shr 16) and 255)) +
                    abs(((a shr 8) and 255) - ((b shr 8) and 255)) +
                    abs((a and 255) - (b and 255))
                changed[gy * gridWidth + gx] = difference >= pixelDifferenceThreshold
            }
        }

        val visited = BooleanArray(changed.size)
        val queue = IntArray(changed.size)
        val found = ArrayList<SpriteCandidate>()
        for (index in changed.indices) {
            if (!changed[index] || visited[index]) continue
            var head = 0
            var tail = 0
            queue[tail++] = index
            visited[index] = true
            var count = 0
            var minGx = gridWidth
            var minGy = gridHeight
            var maxGx = 0
            var maxGy = 0
            while (head < tail) {
                val current = queue[head++]
                val gx = current % gridWidth
                val gy = current / gridWidth
                count++
                minGx = min(minGx, gx)
                minGy = min(minGy, gy)
                maxGx = max(maxGx, gx)
                maxGy = max(maxGy, gy)
                for (dy in -1..1) for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = gx + dx
                    val ny = gy + dy
                    if (nx !in 0 until gridWidth || ny !in 0 until gridHeight) continue
                    val ni = ny * gridWidth + nx
                    if (changed[ni] && !visited[ni]) {
                        visited[ni] = true
                        queue[tail++] = ni
                    }
                }
            }
            if (count < minComponentSamples) continue
            val left = minGx * sampleStep
            val top = minGy * sampleStep
            val right = min(frame.width, (maxGx + 1) * sampleStep)
            val bottom = min(frame.height, (maxGy + 1) * sampleStep)
            val boxWidth = right - left
            val boxHeight = bottom - top
            if (boxWidth !in MIN_SPRITE_SIZE..MAX_SPRITE_SIZE ||
                boxHeight !in MIN_SPRITE_SIZE..MAX_SPRITE_SIZE) continue

            val boxArea = max(1, (maxGx - minGx + 1) * (maxGy - minGy + 1))
            val compactness = (count.toFloat() / boxArea).coerceIn(0f, 1f)
            val colorfulness = measureColorfulness(frame, left, top, right, bottom)
            val motionStrength = (count / 28f).coerceIn(0f, 1f)
            val shapePlausibility = (1f - abs(boxWidth - boxHeight).toFloat() /
                max(boxWidth, boxHeight).toFloat()).coerceIn(0f, 1f)
            var score = 0.45f * motionStrength + 0.25f * compactness +
                0.20f * colorfulness + 0.10f * shapePlausibility

            val priorX = lastCenterX
            val priorY = lastCenterY
            if (priorX != null && priorY != null) {
                val cx = (left + right) / 2
                val cy = (top + bottom) / 2
                val distance = kotlin.math.hypot((cx - priorX).toDouble(), (cy - priorY).toDouble())
                score *= (1f - (distance / 360.0).toFloat().coerceIn(0f, 0.30f))
            }
            found += SpriteCandidate(
                centerX = (left + right) / 2,
                centerY = (top + bottom) / 2,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                score = score.coerceIn(0f, 1f),
                changedSamples = count
            )
        }

        val ranked = found.sortedByDescending { it.score }.take(maxCandidates)
        if (ranked.isEmpty()) {
            return PlayerDetectionResult(
                PlayerDetectionStatus.NOT_FOUND,
                timestampMillis = frame.timestampMillis,
                message = "Chưa tìm thấy vùng chuyển động có hình dạng phù hợp."
            )
        }
        val best = ranked.first()
        val second = ranked.getOrNull(1)
        if (second != null && second.score >= best.score * AMBIGUITY_RATIO) {
            return PlayerDetectionResult(
                PlayerDetectionStatus.AMBIGUOUS,
                alternatives = ranked,
                timestampMillis = frame.timestampMillis,
                message = "Có nhiều vùng giống nhân vật; chưa đủ căn cứ xác định người chơi."
            )
        }

        lastCenterX = best.centerX
        lastCenterY = best.centerY
        return PlayerDetectionResult(
            PlayerDetectionStatus.DETECTED,
            candidate = best,
            alternatives = ranked.drop(1),
            timestampMillis = frame.timestampMillis,
            message = "Đã tìm thấy một ứng viên hình ảnh; cần kiểm chứng trên gameplay thực tế."
        )
    }

    @Synchronized
    fun reset() {
        previous = null
        lastCenterX = null
        lastCenterY = null
    }

    private fun measureColorfulness(
        frame: FrameSnapshot, left: Int, top: Int, right: Int, bottom: Int
    ): Float {
        var saturationTotal = 0L
        var samples = 0
        val step = max(1, sampleStep)
        for (y in top until bottom step step) for (x in left until right step step) {
            val color = frame.pixel(x, y) ?: continue
            val r = (color shr 16) and 255
            val g = (color shr 8) and 255
            val b = color and 255
            val high = max(r, max(g, b))
            val low = min(r, min(g, b))
            saturationTotal += if (high == 0) 0 else ((high - low) * 255 / high)
            samples++
        }
        return if (samples == 0) 0f else
            (saturationTotal.toFloat() / samples / 255f).coerceIn(0f, 1f)
    }

    companion object {
        private const val MIN_SPRITE_SIZE = 4
        private const val MAX_SPRITE_SIZE = 52
        private const val AMBIGUITY_RATIO = 0.88f
    }
}
