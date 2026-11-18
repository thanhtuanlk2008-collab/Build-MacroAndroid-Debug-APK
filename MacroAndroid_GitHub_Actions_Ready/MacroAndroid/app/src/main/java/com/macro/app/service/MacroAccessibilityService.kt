package com.macro.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.macro.app.engine.MacroEngine

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MacroAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        MacroEngine.init(applicationContext)
        MacroEngine.setAccessibilityConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally minimal: we do not read screen content, passwords, or private UI trees.
    }

    override fun onInterrupt() {
        MacroEngine.emergencyStop("AccessibilityService bị gián đoạn bởi hệ thống.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        MacroEngine.setAccessibilityConnected(false)
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        MacroEngine.setAccessibilityConnected(false)
        instance = null
        super.onDestroy()
    }
}
