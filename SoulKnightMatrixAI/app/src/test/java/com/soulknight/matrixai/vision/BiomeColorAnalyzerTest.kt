package com.soulknight.matrixai.vision

import com.soulknight.matrixai.matrix.MatrixBiome
import org.junit.Assert.assertEquals
import org.junit.Test

class BiomeColorAnalyzerTest {
    @Test fun detectsColdBluePaletteWithoutMatchingRoomLayout() {
        val pixels = IntArray(100) { 0xff4aaee8.toInt() }
        val result = BiomeColorAnalyzer(1).analyze(FrameSnapshot(10, 10, 1L, pixels))
        assertEquals(MatrixBiome.GLACIER, result.biome)
    }

    @Test fun detectsWarmSandPalette() {
        val pixels = IntArray(100) { 0xffc99a42.toInt() }
        val result = BiomeColorAnalyzer(1).analyze(FrameSnapshot(10, 10, 1L, pixels))
        assertEquals(MatrixBiome.RELICS, result.biome)
    }
}
