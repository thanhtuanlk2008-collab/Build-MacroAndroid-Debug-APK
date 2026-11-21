package com.macro.app.vision

import android.graphics.Bitmap
import android.os.SystemClock
import com.macro.app.vision.core.FramePreprocessor
import com.macro.app.vision.detection.EntityDetector
import com.macro.app.vision.model.HudState
import com.macro.app.vision.model.VisionResult
import com.macro.app.vision.state.GameStateEstimator

/** Processes a caller-owned frame. Capture permissions and Bitmap lifecycle stay with the caller. */
class MatrixVisionPipeline(
    private val preprocessor: FramePreprocessor,
    private val detector: EntityDetector,
    private val stateEstimator: GameStateEstimator
) {
    fun process(bitmap: Bitmap, timestampMs: Long = SystemClock.uptimeMillis(),
                 hudState: HudState = HudState()): VisionResult {
        val started = SystemClock.uptimeMillis()
        val frame = preprocessor.process(bitmap, timestampMs)
        val detections = detector.detect(frame)
        val state = stateEstimator.estimate(detections, hudState, timestampMs)
        return VisionResult(state, (SystemClock.uptimeMillis() - started).coerceAtLeast(0L),
            frame.originalWidth, frame.originalHeight)
    }
}
