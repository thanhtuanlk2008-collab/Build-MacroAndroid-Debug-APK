package com.soulknight.matrixai.input

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import java.util.concurrent.atomic.AtomicLong

/**
 * Single gateway for app-generated touch gestures.
 *
 * Every gesture is submitted through the user-enabled MatrixAccessibilityService.
 * Android may cancel an in-flight gesture when a new gesture is dispatched; this
 * API deliberately does not promise independent simultaneous multitouch streams.
 */
class InputController internal constructor(
    private val serviceProvider: () -> MatrixAccessibilityService?
) {
    private val requestSequence = AtomicLong(0L)

    fun isAvailable(): Boolean = serviceProvider() != null

    fun tap(x: Float, y: Float, callback: (InputResult) -> Unit = {}): Long {
        return dispatch(x, y, TAP_DURATION_MS, callback)
    }

    fun press(
        x: Float,
        y: Float,
        durationMs: Long,
        callback: (InputResult) -> Unit = {}
    ): Long {
        if (durationMs !in MIN_GESTURE_DURATION_MS..MAX_GESTURE_DURATION_MS) {
            callback(InputResult.InvalidDuration)
            return -1L
        }
        return dispatch(x, y, durationMs, callback)
    }

    private fun dispatch(
        x: Float,
        y: Float,
        durationMs: Long,
        callback: (InputResult) -> Unit
    ): Long {
        if (!x.isFinite() || !y.isFinite() || x < 0f || y < 0f) {
            callback(InputResult.InvalidCoordinates)
            return -1L
        }
        val service = serviceProvider()
        if (service == null) {
            callback(InputResult.ServiceUnavailable)
            return -1L
        }

        val id = requestSequence.incrementAndGet()
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + 0.1f, y + 0.1f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
            .build()

        val accepted = service.dispatchMatrixGesture(gesture) { completed ->
            callback(if (completed) InputResult.Dispatched else InputResult.Cancelled)
        }
        if (!accepted) callback(InputResult.Failed("Android từ chối dispatchGesture"))
        return id
    }

    /** Cancels the current accessibility gesture, if one is active. */
    fun cancelCurrentGesture(): Boolean = serviceProvider()?.cancelMatrixGesture() ?: false

    companion object {
        const val TAP_DURATION_MS = 80L
        const val MIN_GESTURE_DURATION_MS = 40L
        const val MAX_GESTURE_DURATION_MS = 10_000L
    }
}
