package com.soulknight.matrixai.matrix

import com.soulknight.matrixai.vision.FrameSnapshot
import com.soulknight.matrixai.vision.ScreenRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixBiomeRouteCoordinatorTest {
    @Test fun glacierPaletteSkipsSirVerdantEvenWhenPortalCandidateExists() {
        val frame = FrameSnapshot(10, 10, 1L, IntArray(100) { 0xff4aaee8.toInt() })
        val result = MatrixBiomeRouteCoordinator().evaluateAfterBoss(
            floorNumber = 1, roomNumber = 5, bossDefeated = true,
            frame = frame, gameplayRegion = ScreenRegion(0, 0, 10, 10),
            routeScanComplete = true,
            detectedRoutes = listOf(DetectedRoute(RouteDestination.SIR_VERDANT, 0.99f))
        )
        assertEquals(MatrixBiome.GLACIER, result.biome.biome)
        assertEquals(RouteAction.SKIP_SPECIAL_ROOM, result.decision.action)
    }
}
