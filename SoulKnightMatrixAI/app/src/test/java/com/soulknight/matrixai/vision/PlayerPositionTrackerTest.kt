package com.soulknight.matrixai.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPositionTrackerTest {
    private fun frame(playerX: Int, playerY: Int, timestamp: Long): FrameSnapshot {
        val width = 120
        val height = 90
        val pixels = IntArray(width * height) { 0xff202020.toInt() }
        for (y in playerY - 5..playerY + 5) for (x in playerX - 5..playerX + 5) {
            pixels[y * width + x] = if ((x + y) % 2 == 0) 0xff22cc66.toInt() else 0xffeeee22.toInt()
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

    @Test fun rejectsInitializationOutsideFrame() {
        val tracker = PlayerPositionTracker()
        assertTrue(!tracker.initialize(frame(40, 40, 1L), 1, 1))
    }
}
