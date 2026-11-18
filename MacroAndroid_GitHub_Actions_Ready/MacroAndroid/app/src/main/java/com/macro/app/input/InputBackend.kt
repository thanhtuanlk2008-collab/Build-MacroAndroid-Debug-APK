package com.macro.app.input

/**
 * Contract for a future input injector.
 *
 * An implementation may claim INDEPENDENT_TOUCH_STREAM only after it has been
 * validated on the target Android build while a real finger gesture is active.
 * Accessibility dispatchGesture must never claim that capability.
 */
enum class InputCapability {
    ACCESSIBILITY_GESTURE,
    INDEPENDENT_TOUCH_STREAM
}

enum class InputDispatchResult {
    ACCEPTED,
    REJECTED,
    UNSUPPORTED,
    CANCELLED,
    FAILED
}

data class MacroTouchCommand(
    val pointerId: Int,
    val x: Float,
    val y: Float,
    val durationMs: Long,
    val isRelease: Boolean = false
)

data class InputBackendReport(
    val backendId: String,
    val available: Boolean,
    val capabilities: Set<InputCapability>,
    val explanation: String
)

interface MacroInputBackend {
    val backendId: String
    suspend fun probe(): InputBackendReport
    suspend fun dispatch(command: MacroTouchCommand): InputDispatchResult
}
