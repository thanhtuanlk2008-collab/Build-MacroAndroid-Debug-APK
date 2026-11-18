package com.macro.app.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import com.macro.app.model.ActionType
import com.macro.app.model.MacroAction
import com.macro.app.service.MacroAccessibilityService
import com.macro.app.utils.CoordinateConverter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class GestureExecutionStatus {
    COMPLETED,
    CANCELLED_CONFLICT,
    SERVICE_UNAVAILABLE,
    INVALID_PARAMETERS
}

class GestureExecutor(private val context: Context) {

    suspend fun executeAction(action: MacroAction, effectiveDurationMs: Long): GestureExecutionStatus {
        val service = MacroAccessibilityService.instance
            ?: return GestureExecutionStatus.SERVICE_UNAVAILABLE

        val gesture = buildGestureDescription(action, effectiveDurationMs)
            ?: return GestureExecutionStatus.INVALID_PARAMETERS

        return dispatchSuspend(service, gesture)
    }

    private fun buildGestureDescription(
        action: MacroAction,
        durationMs: Long
    ): GestureDescription? {
        val safeDuration = durationMs.coerceIn(1L, 60_000L)
        val builder = GestureDescription.Builder()

        when (action.type) {
            ActionType.TAP, ActionType.LONG_PRESS -> {
                val pt = CoordinateConverter.toPixel(context, action.x, action.y)
                val path = Path().apply {
                    moveTo(pt.x, pt.y)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, safeDuration)
                builder.addStroke(stroke)
            }

            ActionType.SWIPE -> {
                val start = CoordinateConverter.toPixel(context, action.x, action.y)
                val end = CoordinateConverter.toPixel(context, action.endX, action.endY)
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    // Avoid zero-length swipe if start == end
                    if (start.x == end.x && start.y == end.y) {
                        lineTo(end.x + 1f, end.y + 1f)
                    } else {
                        lineTo(end.x, end.y)
                    }
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, safeDuration)
                builder.addStroke(stroke)
            }

            ActionType.WAIT, ActionType.REPEAT -> return null
        }

        return builder.build()
    }

    private suspend fun dispatchSuspend(
        service: MacroAccessibilityService,
        gesture: GestureDescription
    ): GestureExecutionStatus = suspendCancellableCoroutine { continuation ->
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (continuation.isActive) {
                    continuation.resume(GestureExecutionStatus.COMPLETED)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                // Usually triggered when human physical touch conflicts with synthetic gesture
                // or when screen state changes. We report CANCELLED_CONFLICT without crashing.
                if (continuation.isActive) {
                    continuation.resume(GestureExecutionStatus.CANCELLED_CONFLICT)
                }
            }
        }

        val dispatched = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.dispatchGesture(gesture, callback, null)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }

        if (!dispatched && continuation.isActive) {
            continuation.resume(GestureExecutionStatus.INVALID_PARAMETERS)
        }
    }
}
