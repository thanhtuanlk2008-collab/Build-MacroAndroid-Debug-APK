package com.soulknight.matrixai.input

import com.soulknight.matrixai.macro.MacroAction
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Serializes macro gestures. Only one gesture is dispatched at a time because
 * AccessibilityService does not guarantee independent simultaneous touch streams.
 */
class InputActionQueue(
    private val controller: InputController,
    private val onResult: (MacroAction, InputResult) -> Unit = { _, _ -> }
) {
    private data class Request(val action: MacroAction, val target: InputTarget)

    private val lock = Any()
    private val pending = ArrayDeque<Request>()
    private val stopped = AtomicBoolean(true)
    private var dispatching = false

    fun start() {
        synchronized(lock) {
            pending.clear()
            dispatching = false
            stopped.set(false)
        }
    }

    fun enqueue(action: MacroAction, target: InputTarget): Boolean {
        if (!target.isValid()) return false
        synchronized(lock) {
            if (stopped.get()) return false
            // Avoid an unbounded backlog when the configured interval is shorter
            // than the platform's gesture completion time.
            if (pending.size >= MAX_PENDING_ACTIONS) return false
            pending.addLast(Request(action, target))
            dispatchNextLocked()
            return true
        }
    }

    fun stop() {
        synchronized(lock) {
            stopped.set(true)
            pending.clear()
            dispatching = false
        }
        // AccessibilityService has no direct public API to cancel an in-flight gesture.
        // A subsequent stop prevents every queued gesture from being submitted.
    }

    fun pendingCount(): Int = synchronized(lock) { pending.size }

    private fun dispatchNextLocked() {
        if (stopped.get() || dispatching || pending.isEmpty()) return
        val request = pending.removeFirst()
        dispatching = true
        controller.tap(request.target.x, request.target.y) { result ->
            onResult(request.action, result)
            synchronized(lock) {
                dispatching = false
                dispatchNextLocked()
            }
        }
    }

    companion object {
        const val MAX_PENDING_ACTIONS = 32
    }
}
