package com.soulknight.matrixai.navigation

import com.soulknight.matrixai.vision.FrameSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomNavigationPlannerTest {
    @Test fun returnsPathOnlyWhenScanIsReadyAndExitIsReachable() {
        val frame = FrameSnapshot(40, 40, 1L, IntArray(1600) { 0xff777777.toInt() })
        val map = GridMap(3, 1)
        for (x in 0..2) map[GridPoint(x, 0)] = TileType.WALKABLE
        val candidate = RoomCandidate(RoomKind.ENEMY, GridPoint(2, 0), 0.95f)
        val scan = RoomScanResult(map, GridPoint(0, 0), listOf(candidate), candidate, 5L)

        val plan = RoomNavigationPlanner().plan(frame, scan)
        assertEquals(ScanReadiness.READY, plan.quality.readiness)
        assertEquals(candidate, plan.target)
        assertEquals(listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(2, 0)), plan.path)
    }

    @Test fun refusesToProducePathWhenNoReliableExitExists() {
        val frame = FrameSnapshot(40, 40, 1L, IntArray(1600) { 0xff777777.toInt() })
        val scan = RoomScanResult(GridMap(2, 2), GridPoint(0, 0), emptyList(), null, 5L)

        val plan = RoomNavigationPlanner().plan(frame, scan)
        assertEquals(ScanReadiness.NO_RELIABLE_EXITS, plan.quality.readiness)
        assertTrue(plan.path.isEmpty())
    }

    @Test fun refusesUnreachableTarget() {
        val frame = FrameSnapshot(40, 40, 1L, IntArray(1600) { 0xff777777.toInt() })
        val map = GridMap(3, 1)
        map[GridPoint(0, 0)] = TileType.WALKABLE
        map[GridPoint(1, 0)] = TileType.BLOCKED
        map[GridPoint(2, 0)] = TileType.WALKABLE
        val candidate = RoomCandidate(RoomKind.ENEMY, GridPoint(2, 0), 0.95f)
        val scan = RoomScanResult(map, GridPoint(0, 0), listOf(candidate), candidate, 5L)

        val plan = RoomNavigationPlanner().plan(frame, scan)
        assertEquals(ScanReadiness.NO_PATH, plan.quality.readiness)
        assertTrue(plan.path.isEmpty())
    }
}
