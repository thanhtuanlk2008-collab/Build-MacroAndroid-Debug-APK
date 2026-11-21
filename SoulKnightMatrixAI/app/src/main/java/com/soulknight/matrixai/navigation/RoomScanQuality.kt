package com.soulknight.matrixai.navigation

import com.soulknight.matrixai.vision.FrameSnapshot

enum class ScanReadiness { READY, INSUFFICIENT_FRAME, PLAYER_NOT_FOUND, NO_RELIABLE_EXITS, NO_PATH }

/** Explicit readiness gate: low-information scans must not trigger automatic movement. */
data class RoomScanQuality(
    val readiness: ScanReadiness,
    val score: Float,
    val reason: String
)

class RoomScanQualityEvaluator {
    fun evaluate(frame: FrameSnapshot, scan: RoomScanResult): RoomScanQuality {
        val sampled = minOf(frame.width * frame.height, 4096)
        if (sampled < MIN_SAMPLED_PIXELS || frame.width < MIN_DIMENSION || frame.height < MIN_DIMENSION) {
            return RoomScanQuality(ScanReadiness.INSUFFICIENT_FRAME, 0f, "Ảnh quá nhỏ để phân tích an toàn.")
        }
        if (scan.playerPosition == null || !scan.map.contains(scan.playerPosition)) {
            return RoomScanQuality(ScanReadiness.PLAYER_NOT_FOUND, 0f, "Chưa xác định được vị trí người chơi.")
        }
        val candidates = scan.candidates.filter { it.confidence >= MIN_EXIT_CONFIDENCE }
        if (candidates.isEmpty()) {
            return RoomScanQuality(ScanReadiness.NO_RELIABLE_EXITS, 0f, "Chưa có lối ra/phòng đích đủ tin cậy.")
        }
        val score = candidates.maxOf { it.confidence }
        return RoomScanQuality(ScanReadiness.READY, score, "Đã có vị trí người chơi và ít nhất một mục tiêu đủ tin cậy.")
    }

    companion object {
        private const val MIN_SAMPLED_PIXELS = 1024
        private const val MIN_DIMENSION = 32
        private const val MIN_EXIT_CONFIDENCE = 0.70f
    }
}
