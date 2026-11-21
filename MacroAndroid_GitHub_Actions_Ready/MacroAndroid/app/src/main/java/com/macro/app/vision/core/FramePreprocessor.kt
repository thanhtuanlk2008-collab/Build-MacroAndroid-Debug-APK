package com.macro.app.vision.core

import android.graphics.Bitmap
import android.graphics.Matrix
import com.macro.app.vision.model.VisionFrame

class FramePreprocessor(private val maxDimension: Int = 640) {
    init { require(maxDimension >= 160) }

    fun process(source: Bitmap, timestampMs: Long): VisionFrame {
        require(!source.isRecycled) { "Source bitmap has been recycled" }
        val width = source.width
        val height = source.height
        require(width > 0 && height > 0) { "Invalid frame size" }
        val largest = maxOf(width, height)
        val scale = if (largest > maxDimension) maxDimension.toFloat() / largest else 1f
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        val output = if (scale == 1f) source else Bitmap.createBitmap(
            source, 0, 0, width, height,
            Matrix().apply { setScale(scale, scale) }, true
        )
        return VisionFrame(output, timestampMs, width, height)
    }
}
