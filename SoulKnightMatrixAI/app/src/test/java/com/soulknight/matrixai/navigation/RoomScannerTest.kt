package com.soulknight.matrixai.navigation

import com.soulknight.matrixai.vision.FrameSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoomScannerTest {
    @Test fun selectsEnemyRoomBeforeSpecialRoomAndKeepsBossAsFallback() {
        val scanner = RoomScanner()
        val frame = FrameSnapshot(4, 4, 1L, IntArray(16) { 0xff777777.toInt() })
        val result = scanner.scan(
            frame, 4, 4,
            (0..3).flatMap { y -> (0..3).map { x -> GridPoint(x, y) } }.toSet(),
            GridPoint(0, 0),
            listOf(
                RoomObjectDetection(RoomKind.SPECIAL, GridPoint(2, 0), 0.99f),
                RoomObjectDetection(RoomKind.BOSS, GridPoint(3, 0), 0.99f),
                RoomObjectDetection(RoomKind.ENEMY, GridPoint(1, 0), 0.75f)
            ),
            listOf(GridPoint(1, 1), GridPoint(3, 1))
        )
        assertNotNull(result.selectedCandidate)
        assertEquals(RoomKind.ENEMY, result.selectedCandidate!!.kind)
        assertEquals(TileType.WALKABLE, result.map[GridPoint(0, 0)])
    }
}
