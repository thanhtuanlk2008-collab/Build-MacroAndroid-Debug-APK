package com.soulknight.matrixai.vision

/**
 * Small RGB crop used as a reference template. Templates should be cropped from
 * representative in-game screenshots at the same UI scale and display orientation.
 */
data class ImageTemplate(
    val name: String,
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    init {
        require(name.isNotBlank())
        require(width > 0 && height > 0)
        require(pixels.size == width * height)
    }

    companion object {
        fun crop(name: String, frame: FrameSnapshot, region: ScreenRegion): ImageTemplate {
            val area = region.clamp(frame.width, frame.height)
            require(area.right > area.left && area.bottom > area.top) { "Template crop is empty" }
            val width = area.right - area.left
            val height = area.bottom - area.top
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = frame.pixel(area.left + x, area.top + y)
                        ?: error("Crop exceeded frame bounds")
                }
            }
            return ImageTemplate(name, width, height, pixels)
        }
    }
}

data class ImageMatch(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val confidence: Float
)

class TemplateMatcher(private val pixelSampleStride: Int = 4) {
    init {
        require(pixelSampleStride > 0)
    }

    /**
     * Exhaustive coarse scan using normalized RGB absolute difference.
     * This is a deterministic baseline matcher, not a learned vision model.
     */
    fun findBestMatch(
        frame: FrameSnapshot,
        template: ImageTemplate,
        region: ScreenRegion = ScreenRegion(0, 0, frame.width, frame.height),
        searchStep: Int = 4
    ): ImageMatch? {
        require(searchStep > 0)
        val area = region.clamp(frame.width, frame.height)
        if (area.right - area.left < template.width ||
            area.bottom - area.top < template.height
        ) return null

        var best: ImageMatch? = null
        val lastLeft = area.right - template.width
        val lastTop = area.bottom - template.height

        var top = area.top
        while (top <= lastTop) {
            var left = area.left
            while (left <= lastLeft) {
                val confidence = similarity(frame, template, left, top)
                if (best == null || confidence > best.confidence) {
                    best = ImageMatch(left, top, template.width, template.height, confidence)
                }
                left += searchStep
            }
            top += searchStep
        }
        return best
    }

    private fun similarity(
        frame: FrameSnapshot,
        template: ImageTemplate,
        left: Int,
        top: Int
    ): Float {
        var difference = 0L
        var samples = 0
        for (y in 0 until template.height step pixelSampleStride) {
            for (x in 0 until template.width step pixelSampleStride) {
                val actual = frame.pixel(left + x, top + y) ?: continue
                val expected = template.pixels[y * template.width + x]
                difference += kotlin.math.abs(((actual shr 16) and 0xff) - ((expected shr 16) and 0xff))
                difference += kotlin.math.abs(((actual shr 8) and 0xff) - ((expected shr 8) and 0xff))
                difference += kotlin.math.abs((actual and 0xff) - (expected and 0xff))
                samples++
            }
        }
        if (samples == 0) return 0f
        return (1.0 - difference.toDouble() / (samples * 3.0 * 255.0))
            .coerceIn(0.0, 1.0).toFloat()
    }
}
