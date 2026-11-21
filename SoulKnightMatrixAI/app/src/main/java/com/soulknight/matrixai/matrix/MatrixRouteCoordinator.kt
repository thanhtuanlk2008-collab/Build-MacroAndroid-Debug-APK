package com.soulknight.matrixai.matrix

import com.soulknight.matrixai.vision.ImageTemplate
import com.soulknight.matrixai.vision.MatrixRouteVisionAnalyzer
import com.soulknight.matrixai.vision.ScreenRegion
import com.soulknight.matrixai.vision.FrameSnapshot

data class PostBossRouteEvaluation(
    val decision: RouteDecision,
    val vision: com.soulknight.matrixai.vision.MatrixRouteVisionResult
)

/**
 * Joins frame-based template detection with the post-boss route policy.
 * Movement execution remains the responsibility of a future navigation controller.
 */
class MatrixRouteCoordinator(
    private val visionAnalyzer: MatrixRouteVisionAnalyzer = MatrixRouteVisionAnalyzer(),
    private val routePolicy: SpecialRoomRoutePolicy = SpecialRoomRoutePolicy()
) {
    fun evaluateAfterBoss(
        floorNumber: Int,
        roomNumber: Int,
        bossDefeated: Boolean,
        frame: FrameSnapshot,
        biomeTemplates: Map<MatrixBiome, ImageTemplate>,
        sirVerdantEntranceTemplate: ImageTemplate,
        biomeRegion: ScreenRegion,
        routeRegion: ScreenRegion
    ): PostBossRouteEvaluation {
        val vision = visionAnalyzer.analyze(
            frame = frame,
            biomeTemplates = biomeTemplates,
            sirVerdantEntranceTemplate = sirVerdantEntranceTemplate,
            biomeRegion = biomeRegion,
            routeRegion = routeRegion
        )
        val decision = routePolicy.decide(
            BossRouteContext(
                floorNumber = floorNumber,
                roomNumber = roomNumber,
                bossDefeated = bossDefeated,
                biome = vision.biome,
                routeScanComplete = vision.routeScanComplete,
                detectedRoutes = vision.detectedRoutes
            )
        )
        return PostBossRouteEvaluation(decision, vision)
    }
}
