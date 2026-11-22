package com.macro.app.biome

import android.graphics.Bitmap

/**
 * Small RGB histogram descriptor for an on-device baseline.
 * It is intentionally simple and must not be treated as a production classifier.
 */
class BiomeFeatureExtractor(private val binsPerChannel: Int = 8) {
    init {
        require(binsPerChannel == 8) { "The current feature format requires 8 bins per channel" }
    }

    fun extract(bitmap: Bitmap): BiomeFeatureVector {
        require(!bitmap.isRecycled) { "Bitmap has been recycled" }
        require(bitmap.width > 0 && bitmap.height > 0) { "Bitmap dimensions must be positive" }

        val histogram = FloatArray(BiomeFeatureVector.FEATURE_SIZE)
        val sampleStep = maxOf(1, maxOf(bitmap.width, bitmap.height) / 128)
        var samples = 0

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                histogram[((color shr 16 and 0xff) * 8) / 256] += 1f
                histogram[8 + ((color shr 8 and 0xff) * 8) / 256] += 1f
                histogram[16 + ((color and 0xff) * 8) / 256] += 1f
                samples++
                x += sampleStep
            }
            y += sampleStep
        }

        if (samples > 0) {
            for (channel in 0..2) {
                val offset = channel * 8
                for (bin in 0 until 8) histogram[offset + bin] /= samples.toFloat()
            }
        }
        return BiomeFeatureVector(histogram.toList())
    }
}
