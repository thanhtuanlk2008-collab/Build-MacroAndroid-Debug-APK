package com.macro.app.engine

import android.content.Context
import com.macro.app.model.Macro
import com.macro.app.model.MacroState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MacroEngine {

    private var scheduler: MacroScheduler? = null
    val recorder = MacroRecorder()

    private val _selectedMacro = MutableStateFlow<Macro?>(null)
    val selectedMacro: StateFlow<Macro?> = _selectedMacro.asStateFlow()

    private val _isAccessibilityConnected = MutableStateFlow(false)
    val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

    fun init(context: Context) {
        if (scheduler == null) {
            val executor = GestureExecutor(context.applicationContext)
            scheduler = MacroScheduler(executor)
        }
    }

    fun getTelemetryFlow(): StateFlow<PlaybackTelemetry>? = scheduler?.telemetry

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityConnected.value = connected
        if (!connected && isPlayingOrPaused()) {
            emergencyStop("AccessibilityService đã bị tắt hoặc ngắt kết nối.")
        }
    }

    fun selectMacro(macro: Macro) {
        _selectedMacro.value = macro
    }

    fun play(macro: Macro? = _selectedMacro.value, retryOnTouchConflict: Boolean = true) {
        val target = macro ?: return
        _selectedMacro.value = target
        if (recorder.recorderState.value == MacroState.RECORDING) {
            recorder.cancelRecording()
        }
        SpeedEngine.setSpeed(target.defaultSpeed)
        SpeedEngine.setInstantMode(target.instantMode)
        scheduler?.start(target, retryOnTouchConflict)
    }

    fun pause() {
        scheduler?.pause()
    }

    fun resume() {
        scheduler?.resume()
    }

    fun emergencyStop(errorMessage: String? = null) {
        scheduler?.stop(errorMessage)
    }

    fun isPlayingOrPaused(): Boolean {
        val state = scheduler?.telemetry?.value?.state ?: return false
        return state == MacroState.PLAYING || state == MacroState.PAUSED
    }
}
