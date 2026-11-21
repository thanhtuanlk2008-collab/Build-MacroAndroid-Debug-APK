package com.soulknight.matrixai.vision

data class FrameSnapshot(
    val width: Int,
    val height: Int,
    val timestampMillis: Long,
    val pixels: IntArray
) {
    init {
        require(width > 0 && height > 0)
        require(pixels.size == width * height)
    }

    fun pixel(x: Int, y: Int): Int? =
        if (x in 0 until width && y in 0 until height) pixels[y * width + x] else null
}

data class ScreenRegion(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun clamp(width: Int, height: Int): ScreenRegion = ScreenRegion(
        left.coerceIn(0, width), top.coerceIn(0, height),
        right.coerceIn(0, width), bottom.coerceIn(0, height)
    )
}
