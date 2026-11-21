package com.soulknight.matrixai.vision

import com.soulknight.matrixai.matrix.DetectedRoute
import com.soulknight.matrixai.matrix.MatrixBiome
import com.soulknight.matrixai.matrix.RouteDestination

data class MatrixRouteVisionResult(
    val biome: MatrixBiome,
    val biomeConfidence: Float,
    val routeScanComplete: Boolean,
    val detectedRoutes: List<DetectedRoute>,
    val sirVerdantMatch: ImageMatch?
)

/**
 * Template-based visual baseline. Supply reference crops from the actual game
 * for each biome and for the Sir Verdant entrance. No reference assets are bundled.
 */
class MatrixRouteVisionAnalyzer(
    private val matcher: TemplateMatcher = TemplateMatcher()
) {
    fun analyze(
        frame: FrameSnapshot,
        biomeTemplates: Map<MatrixBiome, ImageTemplate>,
        sirVerdantEntranceTemplate: ImageTemplate,
        biomeRegion: ScreenRegion,
        routeRegion: ScreenRegion,
        biomeConfidenceThreshold: Float = DEFAULT_BIOME_THRESHOLD,
        routeConfidenceThreshold: Float = DEFAULT_ROUTE_THRESHOLD,
        searchStep: Int = DEFAULT_SEARCH_STEP
    ): MatrixRouteVisionResult {
        require(biomeConfidenceThreshold in 0f..1f)
        require(routeConfidenceThreshold in 0f..1f)
        require(biomeTemplates.keys.none { it == MatrixBiome.UNKNOWN })

        val biomeMatches = biomeTemplates.mapNotNull { (biome, template) ->
            matcher.findBestMatch(frame, template, biomeRegion, searchStep)
                ?.let { biome to it }
        }
        val bestBiome = biomeMatches.maxByOrNull { it.second.confidence }
        val biomeConfidence = bestBiome?.second?.confidence ?: 0f
        val biome = if (bestBiome != null && biomeConfidence >= biomeConfidenceThreshold) {
            bestBiome.first
        } else {
            MatrixBiome.UNKNOWN
        }

        val boundedRouteRegion = routeRegion.clamp(frame.width, frame.height)
        val routeRegionValid = boundedRouteRegion.right > boundedRouteRegion.left &&
            boundedRouteRegion.bottom > boundedRouteRegion.top &&
            boundedRouteRegion.right - boundedRouteRegion.left >= sirVerdantEntranceTemplate.width &&
            boundedRouteRegion.bottom - boundedRouteRegion.top >= sirVerdantEntranceTemplate.height

        val portalMatch = if (routeRegionValid) {
            matcher.findBestMatch(frame, sirVerdantEntranceTemplate, boundedRouteRegion, searchStep)
        } else {
            null
        }
        val routes = if (portalMatch != null && portalMatch.confidence >= routeConfidenceThreshold) {
            listOf(DetectedRoute(RouteDestination.SIR_VERDANT, portalMatch.confidence))
        } else {
            emptyList()
        }

        return MatrixRouteVisionResult(
            biome = biome,
            biomeConfidence = biomeConfidence,
            routeScanComplete = routeRegionValid,
            detectedRoutes = routes,
            sirVerdantMatch = portalMatch
        )
    }

    companion object {
        const val DEFAULT_BIOME_THRESHOLD = 0.88f
        const val DEFAULT_ROUTE_THRESHOLD = 0.90f
        const val DEFAULT_SEARCH_STEP = 4
    }
}
