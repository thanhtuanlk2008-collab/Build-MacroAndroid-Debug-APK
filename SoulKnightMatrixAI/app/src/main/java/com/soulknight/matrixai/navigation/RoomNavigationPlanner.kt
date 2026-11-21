package com.soulknight.matrixai.navigation

import com.soulknight.matrixai.vision.FrameSnapshot

data class RoomNavigationPlan(
    val quality: RoomScanQuality,
    val target: RoomCandidate?,
    val path: List<GridPoint>
)

/** Connects each newly scanned room map to A* and the enemy-first route policy. */
class RoomNavigationPlanner(
    private val pathfinder: AStarPathfinder = AStarPathfinder(),
    private val qualityEvaluator: RoomScanQualityEvaluator = RoomScanQualityEvaluator()
) {
    fun pathToPreferredExit(scan: RoomScanResult): List<GridPoint> {
        val start = scan.playerPosition ?: return emptyList()
        val target = scan.selectedCandidate?.doorPosition ?: return emptyList()
        return pathfinder.findPath(scan.map, start, target)
    }

    /**
     * Preferred entry point for autonomous navigation. A path is returned only when
     * the frame, player position and semantic exit detections pass the readiness gate.
     */
    fun plan(frame: FrameSnapshot, scan: RoomScanResult): RoomNavigationPlan {
        val quality = qualityEvaluator.evaluate(frame, scan)
        if (quality.readiness != ScanReadiness.READY) {
            return RoomNavigationPlan(quality, null, emptyList())
        }
        val target = scan.selectedCandidate ?: return RoomNavigationPlan(
            quality.copy(readiness = ScanReadiness.NO_RELIABLE_EXITS, score = 0f,
                reason = "Không có mục tiêu được chọn an toàn."),
            null,
            emptyList()
        )
        val path = pathToPreferredExit(scan)
        if (path.isEmpty()) {
            return RoomNavigationPlan(
                quality.copy(readiness = ScanReadiness.NO_PATH, score = 0f,
                    reason = "Không tìm thấy đường đi hợp lệ tới lối ra; không gửi thao tác di chuyển."),
                target,
                emptyList()
            )
        }
        return RoomNavigationPlan(quality, target, path)
    }
}
