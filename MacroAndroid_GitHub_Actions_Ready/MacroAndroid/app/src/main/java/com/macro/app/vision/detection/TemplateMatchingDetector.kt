package com.macro.app.vision.detection

import android.graphics.Bitmap
import com.macro.app.vision.model.BoundingBox
import com.macro.app.vision.model.Detection
import com.macro.app.vision.model.VisionFrame
import com.macro.app.vision.model.VisionTemplate
import java.util.PriorityQueue
import kotlin.math.abs

interface EntityDetector { fun detect(frame: VisionFrame): List<Detection> }

/**
 * Lightweight grayscale matcher for small, user-supplied reference crops.
 * This is a baseline detector, not a trained or scale-invariant game detector.
 */
class TemplateMatchingDetector(
    private val templates: List<VisionTemplate>,
    private val stride: Int = 4,
    private val maxDetectionsPerTemplate: Int = 8
) : EntityDetector {
    init {
        require(stride in 1..32) { "stride must be between 1 and 32" }
        require(maxDetectionsPerTemplate in 1..100) {
            "maxDetectionsPerTemplate must be between 1 and 100"
        }
    }

    private data class Descriptor(val values: FloatArray, val width: Int, val height: Int)
    private data class Candidate(val detection: Detection, val error: Float)

    override fun detect(frame: VisionFrame): List<Detection> {
        val source = frame.bitmap
        if (source.isRecycled || templates.isEmpty() || source.width < 8 || source.height < 8) {
            return emptyList()
        }

        val output = mutableListOf<Detection>()
        val candidateLimit = maxDetectionsPerTemplate * CANDIDATE_MULTIPLIER

        for (template in templates) {
            val reference = template.bitmap
            if (reference.isRecycled || reference.width < 1 || reference.height < 1) continue

            val descriptor = describe(reference)
            if (descriptor.width > source.width || descriptor.height > source.height) continue

            // Keep only the best candidates while scanning; do not retain every match
            // from a large frame, which could otherwise create substantial memory pressure.
            val candidates = PriorityQueue<Candidate>(candidateLimit, compareByDescending { it.error })
            var y = 0
            while (y + descriptor.height <= source.height) {
                var x = 0
                while (x + descriptor.width <= source.width) {
                    val error = compare(source, x, y, descriptor)
                    if (error <= template.threshold) {
                        val sx = frame.originalWidth.toFloat() / source.width
                        val sy = frame.originalHeight.toFloat() / source.height
                        val candidate = Candidate(
                            Detection(
                                template.type,
                                template.label,
                                (1f - error / template.threshold).coerceIn(0f, 1f),
                                BoundingBox(
                                    x * sx,
                                    y * sy,
                                    (x + descriptor.width) * sx,
                                    (y + descriptor.height) * sy
                                ),
                                frame.timestampMs
                            ),
                            error
                        )
                        if (candidates.size < candidateLimit) {
                            candidates.add(candidate)
                        } else if (error < candidates.peek().error) {
                            candidates.poll()
                            candidates.add(candidate)
                        }
                    }
                    x += stride
                }
                y += stride
            }

            val kept = mutableListOf<Candidate>()
            for (candidate in candidates.toList().sortedBy { it.error }) {
                if (kept.none { iou(it.detection.bounds, candidate.detection.bounds) > NMS_IOU_THRESHOLD }) {
                    kept += candidate
                }
                if (kept.size >= maxDetectionsPerTemplate) break
            }
            output += kept.map { it.detection }
        }
        return output.sortedByDescending { it.confidence }
    }

    private fun describe(bitmap: Bitmap): Descriptor {
        val values = FloatArray(GRID * GRID)
        for (gy in 0 until GRID) for (gx in 0 until GRID) {
            val x = ((gx + 0.5f) * bitmap.width / GRID).toInt().coerceIn(0, bitmap.width - 1)
            val y = ((gy + 0.5f) * bitmap.height / GRID).toInt().coerceIn(0, bitmap.height - 1)
            values[gy * GRID + gx] = luminance(bitmap.getPixel(x, y))
        }
        return Descriptor(values, bitmap.width, bitmap.height)
    }

    private fun compare(source: Bitmap, left: Int, top: Int, template: Descriptor): Float {
        var total = 0f
        for (gy in 0 until GRID) for (gx in 0 until GRID) {
            val x = left + ((gx + 0.5f) * template.width / GRID).toInt().coerceIn(0, template.width - 1)
            val y = top + ((gy + 0.5f) * template.height / GRID).toInt().coerceIn(0, template.height - 1)
            total += abs(luminance(source.getPixel(x, y)) - template.values[gy * GRID + gx])
        }
        return total / (GRID * GRID)
    }

    private fun luminance(color: Int): Float {
        val r = (color shr 16 and 255) / 255f
        val g = (color shr 8 and 255) / 255f
        val b = (color and 255) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)
        val intersection = maxOf(0f, right - left) * maxOf(0f, bottom - top)
        val union = a.width * a.height + b.width * b.height - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    companion object {
        private const val GRID = 8
        private const val CANDIDATE_MULTIPLIER = 16
        private const val NMS_IOU_THRESHOLD = 0.35f
    }
}
