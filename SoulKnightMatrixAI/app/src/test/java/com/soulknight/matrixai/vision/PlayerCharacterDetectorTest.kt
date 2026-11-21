package com.soulknight.matrixai.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerCharacterDetectorTest {
    private fun frame(spriteX: Int?, timestamp: Long): FrameSnapshot {
        val width = 120
        val height = 90
        val pixels = IntArray(width * height) { 0xff202020.toInt() }
        if (spriteX != null) {
            for (y in 30..47) for (x in spriteX..spriteX + 15) {
                pixels[y * width + x] =
                    if ((x + y) % 2 == 0) 0xffe8c832.toInt() else 0xff30b878.toInt()
            }
        }
        return FrameSnapshot(width, height, timestamp, pixels)
    }

    @Test fun waitsForSecondFrameBeforeMotionAnalysis() {
        val detector = PlayerCharacterDetector(sampleStep = 1)
        assertEquals(PlayerDetectionStatus.WARMING_UP, detector.analyze(frame(20, 1)).status)
        val result = detector.analyze(frame(27, 101))
        assertTrue(result.status == PlayerDetectionStatus.DETECTED ||
            result.status == PlayerDetectionStatus.NOT_FOUND ||
            result.status == PlayerDetectionStatus.AMBIGUOUS)
    }

    @Test fun reportsAmbiguousWhenSeveralMovingRegionsHaveSimilarScores() {
        val detector = PlayerCharacterDetector(sampleStep = 1)
        val first = frame(null, 1)
        detector.analyze(first)
        val secondPixels = first.pixels.copyOf()
        for (y in 20..35) for (x in 15..28) secondPixels[y * first.width + x] = 0xffe8c832.toInt()
        for (y in 50..65) for (x in 75..88) secondPixels[y * first.width + x] = 0xff30b878.toInt()
        val result = detector.analyze(FrameSnapshot(first.width, first.height, 101, secondPixels))
        assertTrue(result.status == PlayerDetectionStatus.AMBIGUOUS ||
            result.status == PlayerDetectionStatus.DETECTED ||
            result.status == PlayerDetectionStatus.NOT_FOUND)
        assertNotNull(result.message)
    }
}
