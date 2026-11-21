package com.soulknight.matrixai.matrix

/**
 * Biomes are deliberately explicit: UNKNOWN must never be treated as an eligible biome.
 */
enum class MatrixBiome {
    GLACIER,
    RELICS,
    OTHER,
    UNKNOWN
}

enum class RouteDestination {
    SIR_VERDANT,
    NORMAL_NEXT_STAGE,
    UNKNOWN
}

data class DetectedRoute(
    val destination: RouteDestination,
    val confidence: Float
) {
    init {
        require(confidence in 0f..1f) { "Route confidence must be between 0 and 1" }
    }
}

data class BossRouteContext(
    val floorNumber: Int,
    val roomNumber: Int,
    val bossDefeated: Boolean,
    val biome: MatrixBiome,
    val routeScanComplete: Boolean,
    val detectedRoutes: List<DetectedRoute> = emptyList()
) {
    init {
        require(floorNumber >= 0) { "Floor number cannot be negative" }
        require(roomNumber >= 0) { "Room number cannot be negative" }
    }
}

enum class RouteAction {
    NOT_APPLICABLE,
    WAIT_FOR_BIOME,
    SKIP_SPECIAL_ROOM,
    WAIT_FOR_ROUTE_SCAN,
    ENTER_SIR_VERDANT,
    CONTINUE_NORMAL
}

data class RouteDecision(
    val action: RouteAction,
    val reason: String,
    val selectedRoute: DetectedRoute? = null
)

/**
 * Applies the post-boss X-5 route rule.
 *
 * This class consumes detections; it does not perform image recognition itself.
 * A visual detector must supply biome, route candidates and confidence values.
 */
class SpecialRoomRoutePolicy(
    private val sirVerdantConfidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    init {
        require(sirVerdantConfidenceThreshold in 0f..1f)
    }

    fun decide(context: BossRouteContext): RouteDecision {
        if (!context.bossDefeated || context.roomNumber != BOSS_ROOM_NUMBER) {
            return RouteDecision(
                RouteAction.NOT_APPLICABLE,
                "Chỉ kiểm tra tuyến đặc biệt sau khi boss ở phòng X-5 bị đánh bại."
            )
        }

        when (context.biome) {
            MatrixBiome.GLACIER, MatrixBiome.RELICS -> return RouteDecision(
                RouteAction.SKIP_SPECIAL_ROOM,
                "Biome Glacier/Relics: bỏ qua phòng Sir Verdant và tiếp tục tuyến bình thường."
            )
            MatrixBiome.UNKNOWN -> return RouteDecision(
                RouteAction.WAIT_FOR_BIOME,
                "Chưa xác định được biome; không tự chọn tuyến đặc biệt."
            )
            MatrixBiome.OTHER -> Unit
        }

        if (!context.routeScanComplete) {
            return RouteDecision(
                RouteAction.WAIT_FOR_ROUTE_SCAN,
                "Đang chờ nhận diện đầy đủ các lối đi sau boss."
            )
        }

        val sirVerdantRoute = context.detectedRoutes
            .asSequence()
            .filter { it.destination == RouteDestination.SIR_VERDANT }
            .filter { it.confidence >= sirVerdantConfidenceThreshold }
            .maxByOrNull { it.confidence }

        return if (sirVerdantRoute != null) {
            RouteDecision(
                RouteAction.ENTER_SIR_VERDANT,
                "Đã phát hiện lối Sir Verdant đủ độ tin cậy; ưu tiên tuyến này.",
                sirVerdantRoute
            )
        } else {
            RouteDecision(
                RouteAction.CONTINUE_NORMAL,
                "Không phát hiện lối Sir Verdant đủ độ tin cậy; tiếp tục tuyến bình thường."
            )
        }
    }

    companion object {
        const val BOSS_ROOM_NUMBER = 5
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.80f
    }
}
