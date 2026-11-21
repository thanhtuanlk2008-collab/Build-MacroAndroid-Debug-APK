package com.soulknight.matrixai.movement

import com.soulknight.matrixai.capture.FrameSnapshotStore
import com.soulknight.matrixai.vision.FrameSnapshot
import com.soulknight.matrixai.vision.PlayerPositionTracker
import com.soulknight.matrixai.vision.TrackedPlayerPosition
import kotlin.math.abs
import kotlin.math.max

/**
 * Passive, user-started observer. It does not send touch input; it measures
 * naturally occurring player movement and updates the saved profile only when
 * a sufficiently large speed change is observed.
 */
class MovementAdaptiveSession(
    initialProfile: MovementProfile,
    playerCenterX: Int,
    playerCenterY: Int,
    private val onProfileChanged: (MovementProfile, Float) -> Unit,
    private val onStatus: (String) -> Unit,
    private val monitor: MovementAdaptiveMonitor = MovementAdaptiveMonitor()
) {
    private val tracker = PlayerPositionTracker(
        patchRadius = 5,
        searchRadius = 64,
        minimumConfidence = 0.76f,
        templateAdaptation = 0.04f
    )
    private var profile = initialProfile
    private var previous: TrackedPlayerPosition? = null
    private var active = false
    private val listener: (FrameSnapshot) -> Unit = ::onFrame

    fun start(): Boolean {
        if (active) return false
        val frame = FrameSnapshotStore.latestFrame ?: run {
            onStatus("Chưa có khung hình để bắt đầu theo dõi.")
            return false
        }
        if (!tracker.initialize(frame, initialX, initialY)) {
            onStatus("Không thể khởi tạo theo dõi tại vị trí đã chọn.")
            return false
        }
        previous = TrackedPlayerPosition(initialX, initialY, 1f, frame.timestampMillis)
        active = true
        FrameSnapshotStore.addListener(listener)
        onStatus("Đang theo dõi chuyển động. Hãy chơi bình thường; AI chỉ đo khi nhân vật thực sự di chuyển.")
        return true
    }

    fun stop() {
        if (!active) return
        active = false
        FrameSnapshotStore.removeListener(listener)
        tracker.reset()
        previous = null
        onStatus("Đã dừng theo dõi tốc độ.")
    }

    private fun onFrame(frame: FrameSnapshot) {
        if (!active) return
        val current = tracker.track(frame) ?: return
        val from = previous
        previous = current
        if (from == null) return
        val elapsed = current.timestampMillis - from.timestampMillis
        if (elapsed !in MIN_SAMPLE_MILLIS..MAX_SAMPLE_MILLIS) return
        val dx = current.x - from.x
        val dy = current.y - from.y
        val absX = abs(dx).toFloat()
        val absY = abs(dy).toFloat()
        val distance = kotlin.math.hypot(absX, absY)
        if (distance < MIN_DISPLACEMENT_PIXELS) return

        val direction = when {
            absX >= absY * DIAGONAL_RATIO && absY >= absX * DIAGONAL_RATIO ->
                MovementDirection.DIAGONAL
            absX >= absY -> MovementDirection.HORIZONTAL
            else -> MovementDirection.VERTICAL
        }
        val sample = MovementSample(
            from.x.toFloat(), from.y.toFloat(),
            current.x.toFloat(), current.y.toFloat(),
            elapsed, direction
        )
        val old = profile
        val next = monitor.updateIfChanged(old, sample, current.timestampMillis)
        if (next !== old && next.updatedAtMillis != old.updatedAtMillis) {
            profile = next
            val baseline = when (direction) {
                MovementDirection.HORIZONTAL -> old.horizontalPixelsPerSecond
                MovementDirection.VERTICAL -> old.verticalPixelsPerSecond
                MovementDirection.DIAGONAL -> old.diagonalPixelsPerSecond
            }
            val measured = MovementCalibrator().measure(sample)
            val delta = if (baseline > 0f) abs(measured - baseline) / baseline else 0f
            onProfileChanged(next, delta)
            onStatus(
                if (delta >= 0.12f) "Phát hiện tốc độ thay đổi khoảng ${(delta * 100).toInt()}%. Đã cập nhật hồ sơ."
                else "Đã cập nhật hồ sơ tốc độ."
            )
        }
    }

    companion object {
        private const val MIN_SAMPLE_MILLIS = 250L
        private const val MAX_SAMPLE_MILLIS = 1800L
        private const val MIN_DISPLACEMENT_PIXELS = 3f
        private const val DIAGONAL_RATIO = 0.55f
    }

    private val initialX = playerCenterX
    private val initialY = playerCenterY
}
