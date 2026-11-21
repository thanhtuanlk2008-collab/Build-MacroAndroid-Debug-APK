package com.soulknight.matrixai.navigation

import com.soulknight.matrixai.vision.FrameSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomScanQualityTest {
    @Test fun refusesToProceedWhenPlayerOrReliableExitIsMissing() {
        val frame = FrameSnapshot(40, 40, 1L, IntArray(1600))
        val map = GridMap(2, 2)
        val noPlayer = RoomScanResult(map, null, emptyList(), null, 1L)
        assertEquals(ScanReadiness.PLAYER_NOT_FOUND, RoomScanQualityEvaluator().evaluate(frame, noPlayer).readiness)

        val noExit = RoomScanResult(map, GridPoint(0, 0), emptyList(), null, 1L)
        assertEquals(ScanReadiness.NO_RELIABLE_EXITS, RoomScanQualityEvaluator().evaluate(frame, noExit).readiness)
    }

    @Test fun rejectsTinyFrames() {
        val frame = FrameSnapshot(10, 10, 1L, IntArray(100))
        val scan = RoomScanResult(GridMap(1, 1), GridPoint(0, 0), emptyList(), null, 1L)
        assertEquals(ScanReadiness.INSUFFICIENT_FRAME, RoomScanQualityEvaluator().evaluate(frame, scan).readiness)
    }
}
