package com.macro.app.engine

import com.macro.app.model.ActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToLong

object SpeedEngine {

    val PRESET_SPEEDS = listOf(
        0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 5.0f, 8.0f, 10.0f, 16.0f
    )

    const val MIN_SPEED = 0.25f
    const val MAX_SPEED = 32.0f

    // Practical Android AccessibilityService dispatchGesture() safety floors
    const val MIN_TAP_DURATION_MS = 25L
    const val MIN_LONG_PRESS_DURATION_MS = 80L
    const val MIN_SWIPE_DURATION_MS = 40L
    const val DEFAULT_MIN_INTERVAL_MS = 15L

    private val _speedMultiplier = MutableStateFlow(1.0f)
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    private val _instantMode = MutableStateFlow(false)
    val instantMode: StateFlow<Boolean> = _instantMode.asStateFlow()

    private val _minGestureIntervalMs = MutableStateFlow(DEFAULT_MIN_INTERVAL_MS)
    val minGestureIntervalMs: StateFlow<Long> = _minGestureIntervalMs.asStateFlow()

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        _speedMultiplier.value = clamped
    }

    fun stepSpeedUp() {
        val current = _speedMultiplier.value
        val next = PRESET_SPEEDS.firstOrNull { it > current + 0.01f } ?: PRESET_SPEEDS.last()
        _speedMultiplier.value = next
    }

    fun stepSpeedDown() {
        val current = _speedMultiplier.value
        val prev = PRESET_SPEEDS.lastOrNull { it < current - 0.01f } ?: PRESET_SPEEDS.first()
        _speedMultiplier.value = prev
    }

    fun setInstantMode(enabled: Boolean) {
        _instantMode.value = enabled
    }

    fun setMinGestureInterval(intervalMs: Long) {
        _minGestureIntervalMs.value = intervalMs.coerceIn(0L, 500L)
    }

    /**
     * Calculates effective gesture duration using:
     * newTime = originalTime / speedMultiplier
     * While respecting minimum Android dispatchGesture() hardware/input floors so gestures are not dropped.
     */
    fun computeGestureDuration(type: ActionType, originalDurationMs: Long): Long {
        val speed = _speedMultiplier.value.coerceAtLeast(MIN_SPEED)
        val scaled = (originalDurationMs / speed).roundToLong()

        return when (type) {
            ActionType.TAP -> scaled.coerceAtLeast(MIN_TAP_DURATION_MS)
            ActionType.LONG_PRESS -> scaled.coerceAtLeast(MIN_LONG_PRESS_DURATION_MS)
            ActionType.SWIPE -> scaled.coerceAtLeast(MIN_SWIPE_DURATION_MS)
            ActionType.WAIT -> {
                if (_instantMode.value) 0L else scaled.coerceAtLeast(0L)
            }
            ActionType.REPEAT -> 0L
        }
    }

    /**
     * Calculates effective wait delay after an action.
     * In Instant Mode, unnecessary WAIT delays are skipped while keeping a tiny safe interval
     * to prevent overlapping dispatchGesture() calls.
     */
    fun computeWaitDelay(originalWaitMs: Long): Long {
        if (originalWaitMs <= 0L) return _minGestureIntervalMs.value
        if (_instantMode.value) return _minGestureIntervalMs.value

        val speed = _speedMultiplier.value.coerceAtLeast(MIN_SPEED)
        val scaled = (originalWaitMs / speed).roundToLong()
        return scaled.coerceAtLeast(_minGestureIntervalMs.value)
    }
}
