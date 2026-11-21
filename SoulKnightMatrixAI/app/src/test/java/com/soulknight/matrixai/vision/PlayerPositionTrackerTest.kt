package com.soulknight.matrixai.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPositionTrackerTest {
    private fun frame(playerX: Int, playerY: Int, timestamp: Long, tint: Int = 0): FrameSnapshot {
        val width = 120
        val height = 90
        val pixels = IntArray(width * height) { 0xff202020.toInt() }
        for (y in playerY - 5..playerY + 5) for (x in playerX - 5..playerX + 5) {
            val base = if ((x + y) % 2 == 0) 0xff22cc66.toInt() else 0xffeeee22.toInt()
            val red = (((base shr 16) and 255) + tint).coerceIn(0, 255)
            val green = (((base shr 8) and 255) + tint).coerceIn(0, 255)
            val blue = ((base and 255) + tint).coerceIn(0, 255)
            pixels[y * width + x] = (0xff shl 24) or (red shl 16) or (green shl 8) or blue
        }
        return FrameSnapshot(width, height, timestamp, pixels)
    }

    @Test fun followsDistinctivePatchAcrossFrames() {
        val tracker = PlayerPositionTracker(patchRadius = 4, searchRadius = 24, minimumConfidence = 0.75f)
        assertTrue(tracker.initialize(frame(40, 40, 1L), 40, 40))
        val tracked = tracker.track(frame(50, 44, 101L))
        assertNotNull(tracked)
        assertEquals(50, tracked!!.x)
        assertEquals(44, tracked.y)
    }

    @Test fun adaptsToSmallAppearanceChangesAcrossFrames() {
        val tracker = PlayerPositionTracker(
            patchRadius = 4, searchRadius = 24, minimumConfidence = 0.70f, templateAdaptation = 0.12f
        )
        assertTrue(tracker.initialize(frame(40, 40, 1L), 40, 40))
        assertNotNull(tracker.track(frame(44, 40, 101L, tint = 8)))
        assertNotNull(tracker.track(frame(48, 40, 201L, tint = 14)))
    }

    @Test fun rejectsInitializationOutsideFrame() {
        val tracker = PlayerPositionTracker()
        assertTrue(!tracker.initialize(frame(40, 40, 1L), 1, 1))
    }
}
