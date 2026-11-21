package com.soulknight.matrixai.movement

import android.os.Handler
import com.soulknight.matrixai.capture.FrameSnapshotStore
import com.soulknight.matrixai.input.InputController
import com.soulknight.matrixai.input.InputResult
import com.soulknight.matrixai.input.InputTarget
import com.soulknight.matrixai.input.JoystickController
import com.soulknight.matrixai.vision.FrameSnapshot
import com.soulknight.matrixai.vision.PlayerPositionTracker
import com.soulknight.matrixai.vision.TrackedPlayerPosition

/**
 * User-started, short joystick calibration. The user must choose a clear room,
 * tap the player centre in the live preview, and enter the joystick centre.
 * Gestures are bounded and sequential; this class never starts on its own.
 */
class MovementCalibrationRunner(
    private val inputController: InputController,
    private val handler: Handler,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onUpdate: (status: String, profile: MovementProfile?) -> Unit
) {
    private val tracker = PlayerPositionTracker(patchRadius = 5, searchRadius = 64, minimumConfidence = 0.72f)
    private val calibrator = MovementCalibrator(smoothing = 1f)
    private val joystick = JoystickController(inputController)
    private val trials = listOf(
        Trial(1f, 0f, MovementDirection.HORIZONTAL, "ngang"),
        Trial(0f, 1f, MovementDirection.VERTICAL, "dọc"),
        Trial(0.7071f, 0.7071f, MovementDirection.DIAGONAL, "chéo")
    )
    private var active = false
    private var trialIndex = 0
    private var profile = MovementProfile.UNCALIBRATED
    private var startPosition: TrackedPlayerPosition? = null
    private var latestPosition: TrackedPlayerPosition? = null
    private var listener: ((FrameSnapshot) -> Unit)? = null

    fun start(
        playerCenterX: Int,
        playerCenterY: Int,
        joystickCenter: InputTarget,
        joystickRadiusPixels: Float = DEFAULT_JOYSTICK_RADIUS
    ): Boolean {
        if (active || screenWidth <= 0 || screenHeight <= 0 || !joystickCenter.isValid()) return false
        val frame = FrameSnapshotStore.latestFrame ?: run {
            onUpdate("Chưa có khung hình. Hãy bật chụp màn hình trước.", null)
            return false
        }
        if (!tracker.initialize(frame, playerCenterX, playerCenterY)) {
            onUpdate("Không thể khởi tạo theo dõi: hãy chọn tâm nhân vật bên trong ảnh.", null)
            return false
        }
        if (joystickCenter.x >= screenWidth || joystickCenter.y >= screenHeight ||
            joystickRadiusPixels !in JoystickController.MIN_RADIUS..JoystickController.MAX_RADIUS
        ) {
            tracker.reset()
            onUpdate("Tọa độ joystick hoặc bán kính không hợp lệ.", null)
            return false
        }

        active = true
        trialIndex = 0
        profile = MovementProfile.UNCALIBRATED
        latestPosition = TrackedPlayerPosition(playerCenterX, playerCenterY, 1f, frame.timestampMillis)
        val frameListener: (FrameSnapshot) -> Unit = { next ->
            if (active) tracker.track(next)?.let { latestPosition = it }
        }
        listener = frameListener
        FrameSnapshotStore.addListener(frameListener)
        onUpdate("Đã bắt đầu hiệu chỉnh. Đang thử hướng ngang…", null)
        runTrial(joystickCenter, joystickRadiusPixels)
        return true
    }

    fun stop() {
        active = false
        listener?.let(FrameSnapshotStore::removeListener)
        listener = null
        tracker.reset()
        startPosition = null
        onUpdate("Đã dừng hiệu chỉnh. Cử chỉ đang chạy (nếu có) có thể hoàn tất trước khi dừng.", profile)
    }

    private fun runTrial(center: InputTarget, radius: Float) {
        if (!active) return
        if (trialIndex >= trials.size) {
            finish()
            return
        }
        val origin = latestPosition
        if (origin == null) {
            fail("Không xác định được vị trí nhân vật.")
            return
        }
        startPosition = origin
        val trial = trials[trialIndex]
        onUpdate("Đang đo tốc độ ${trial.label} (${trialIndex + 1}/${trials.size})…", null)
        val gestureId = joystick.move(
            center = center,
            directionX = trial.dx,
            directionY = trial.dy,
            radiusPixels = radius,
            durationMillis = TRIAL_DURATION_MS,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        ) { result ->
            if (!active) return@move
            if (result != InputResult.Dispatched) {
                fail("Không thực hiện được lượt thử ${trial.label}: $result")
                return@move
            }
            handler.postDelayed({
                if (!active) return@postDelayed
                val from = startPosition
                val to = latestPosition
                if (from == null || to == null || to.timestampMillis <= from.timestampMillis) {
                    fail("Không đủ khung hình để đo ${trial.label}. Hãy kiểm tra quyền chụp màn hình và thử lại.")
                    return@postDelayed
                }
                val sample = MovementSample(
                    startX = from.x.toFloat(),
                    startY = from.y.toFloat(),
                    endX = to.x.toFloat(),
                    endY = to.y.toFloat(),
                    elapsedMillis = to.timestampMillis - from.timestampMillis,
                    direction = trial.direction
                )
                val distance = kotlin.math.hypot(sample.endX - sample.startX, sample.endY - sample.startY)
                if (distance < MIN_MEASURED_DISPLACEMENT_PX) {
                    fail("Nhân vật chưa di chuyển đủ rõ ở hướng ${trial.label}. Có thể joystick sai tọa độ hoặc bị vật cản.")
                    return@postDelayed
                }
                profile = calibrator.update(profile, sample, to.timestampMillis)
                trialIndex++
                runTrial(center, radius)
            }, FRAME_SETTLE_DELAY_MS)
        }
        if (gestureId < 0L && active) fail("Không thể gửi thao tác joystick.")
    }

    private fun finish() {
        active = false
        listener?.let(FrameSnapshotStore::removeListener)
        listener = null
        tracker.reset()
        onUpdate(
            "Hiệu chỉnh hoàn tất. Tốc độ tính theo pixel của khung hình đã thu nhỏ; hãy xác minh trước khi dùng điều hướng tự động.",
            profile
        )
    }

    private fun fail(message: String) {
        active = false
        listener?.let(FrameSnapshotStore::removeListener)
        listener = null
        tracker.reset()
        onUpdate(message, profile.takeIf { it.confidence > 0f })
    }

    private data class Trial(val dx: Float, val dy: Float, val direction: MovementDirection, val label: String)

    companion object {
        const val TRIAL_DURATION_MS = 450L
        const val FRAME_SETTLE_DELAY_MS = 160L
        const val DEFAULT_JOYSTICK_RADIUS = 42f
        const val MIN_MEASURED_DISPLACEMENT_PX = 2.0f
    }
}
