package com.soulknight.matrixai.matrix

import com.soulknight.matrixai.vision.BiomeColorAnalyzer
import com.soulknight.matrixai.vision.BiomeColorResult
import com.soulknight.matrixai.vision.FrameSnapshot
import com.soulknight.matrixai.vision.ScreenRegion

data class PaletteRouteEvaluation(val biome: BiomeColorResult, val decision: RouteDecision)

/** Uses layout-independent HSV palette classification as the biome input to the X-5 policy. */
class MatrixBiomeRouteCoordinator(
    private val biomeAnalyzer: BiomeColorAnalyzer = BiomeColorAnalyzer(),
    private val policy: SpecialRoomRoutePolicy = SpecialRoomRoutePolicy()
) {
    fun evaluateAfterBoss(
        floorNumber: Int,
        roomNumber: Int,
        bossDefeated: Boolean,
        frame: FrameSnapshot,
        gameplayRegion: ScreenRegion,
        routeScanComplete: Boolean,
        detectedRoutes: List<DetectedRoute> = emptyList()
    ): PaletteRouteEvaluation {
        val biome = biomeAnalyzer.analyze(frame, gameplayRegion)
        val decision = policy.decide(
            BossRouteContext(
                floorNumber = floorNumber,
                roomNumber = roomNumber,
                bossDefeated = bossDefeated,
                biome = biome.biome,
                routeScanComplete = routeScanComplete,
                detectedRoutes = detectedRoutes
            )
        )
        return PaletteRouteEvaluation(biome, decision)
    }
}
