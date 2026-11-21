package com.soulknight.matrixai.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent

/**
 * Explicitly user-enabled accessibility bridge.
 * It does not inspect UI trees, click arbitrary nodes, or bypass Android consent.
 */
class MatrixAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        cancelMatrixGesture()
    }

    override fun onDestroy() {
        cancelMatrixGesture()
        if (instance === this) instance = null
        super.onDestroy()
    }

    internal fun dispatchMatrixGesture(
        gesture: GestureDescription,
        onFinished: (Boolean) -> Unit
    ): Boolean {
        return dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    onFinished(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    onFinished(false)
                }
            },
            null
        )
    }

    /**
     * Android AccessibilityService exposes no dedicated cancel-current-gesture API.
     * Returning false is intentional; never emulate cancellation with BACK/HOME.
     * Dispatching a subsequent gesture may cause Android to cancel the prior one.
     */
    internal fun cancelMatrixGesture(): Boolean = false

    companion object {
        @Volatile
        private var instance: MatrixAccessibilityService? = null

        fun isConnected(): Boolean = instance != null

        fun inputController(): InputController = InputController { instance }
    }
}
