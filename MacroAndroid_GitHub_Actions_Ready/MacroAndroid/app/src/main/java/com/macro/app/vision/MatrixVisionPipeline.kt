package com.macro.app.vision

import android.graphics.Bitmap
import android.os.SystemClock
import com.macro.app.vision.core.FramePreprocessor
import com.macro.app.vision.detection.EntityDetector
import com.macro.app.vision.model.HudState
import com.macro.app.vision.model.VisionResult
import com.macro.app.vision.state.GameStateEstimator

/**
 * Processes a caller-owned frame.
 * Capture permissions and the input Bitmap lifecycle stay with the caller.
 * Any downscaled intermediate Bitmap created by the preprocessor is released here.
 */
class MatrixVisionPipeline(
    private val preprocessor: FramePreprocessor,
    private val detector: EntityDetector,
    private val stateEstimator: GameStateEstimator
) {
    fun process(
        bitmap: Bitmap,
        timestampMs: Long = SystemClock.uptimeMillis(),
        hudState: HudState = HudState()
    ): VisionResult {
        val started = SystemClock.uptimeMillis()
        val frame = preprocessor.process(bitmap, timestampMs)
        try {
            val detections = detector.detect(frame)
            val state = stateEstimator.estimate(detections, hudState, timestampMs)
            return VisionResult(
                state = state,
                processingTimeMs = (SystemClock.uptimeMillis() - started).coerceAtLeast(0L),
                frameWidth = frame.originalWidth,
                frameHeight = frame.originalHeight
            )
        } finally {
            // Never recycle the caller's original Bitmap.
            if (frame.bitmap !== bitmap && !frame.bitmap.isRecycled) {
                frame.bitmap.recycle()
            }
        }
    }
}
