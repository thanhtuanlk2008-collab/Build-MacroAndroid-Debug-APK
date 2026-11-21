package com.soulknight.matrixai.input

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import java.util.concurrent.atomic.AtomicLong

/**
 * Single gateway for app-generated touch gestures.
 * Gestures are submitted only through the user-enabled AccessibilityService.
 */
class InputController internal constructor(
    private val serviceProvider: () -> MatrixAccessibilityService?
) {
    private val requestSequence = AtomicLong(0L)

    fun isAvailable(): Boolean = serviceProvider() != null

    fun tap(x: Float, y: Float, callback: (InputResult) -> Unit = {}): Long =
        dispatchPath(x, y, x + 0.1f, y + 0.1f, TAP_DURATION_MS, callback)

    fun press(x: Float, y: Float, durationMs: Long, callback: (InputResult) -> Unit = {}): Long =
        dispatchPath(x, y, x + 0.1f, y + 0.1f, durationMs, callback)

    /** Performs one bounded swipe; callers must validate target bounds for the device. */
    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long,
        callback: (InputResult) -> Unit = {}
    ): Long = dispatchPath(startX, startY, endX, endY, durationMs, callback)

    private fun dispatchPath(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long,
        callback: (InputResult) -> Unit
    ): Long {
        if (durationMs !in MIN_GESTURE_DURATION_MS..MAX_GESTURE_DURATION_MS) {
            callback(InputResult.InvalidDuration)
            return -1L
        }
        if (listOf(startX, startY, endX, endY).any { !it.isFinite() || it < 0f }) {
            callback(InputResult.InvalidCoordinates)
            return -1L
        }
        val service = serviceProvider()
        if (service == null) {
            callback(InputResult.ServiceUnavailable)
            return -1L
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
            .build()
        val id = requestSequence.incrementAndGet()
        val accepted = service.dispatchMatrixGesture(gesture) { completed ->
            callback(if (completed) InputResult.Dispatched else InputResult.Cancelled)
        }
        if (!accepted) callback(InputResult.Failed("Android từ chối dispatchGesture"))
        return id
    }

    /** Android AccessibilityService has no public API to cancel an in-flight gesture. */
    fun cancelCurrentGesture(): Boolean = serviceProvider()?.cancelMatrixGesture() ?: false

    companion object {
        const val TAP_DURATION_MS = 80L
        const val MIN_GESTURE_DURATION_MS = 40L
        const val MAX_GESTURE_DURATION_MS = 10_000L
    }
}
