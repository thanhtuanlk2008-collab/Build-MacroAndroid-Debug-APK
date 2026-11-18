package com.macro.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.macro.app.MacroApplication
import com.macro.app.engine.MacroEngine
import com.macro.app.engine.PlaybackTelemetry
import com.macro.app.engine.SpeedEngine
import com.macro.app.model.GameCategory
import com.macro.app.model.Macro
import com.macro.app.model.MacroSettings
import com.macro.app.service.FloatingControlService
import com.macro.app.utils.PermissionManager
import com.macro.app.utils.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MacroViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MacroApplication
    private val repository = app.repository
    private val settingsStore = app.settingsDataStore

    val macros: StateFlow<List<Macro>> = repository.observeAllMacros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<MacroSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MacroSettings())

    val telemetry: StateFlow<PlaybackTelemetry> = MacroEngine.getTelemetryFlow()
        ?: MutableStateFlow(PlaybackTelemetry())

    val speedMultiplier: StateFlow<Float> = SpeedEngine.speedMultiplier
    val instantMode: StateFlow<Boolean> = SpeedEngine.instantMode
    val isAccessibilityConnected: StateFlow<Boolean> = MacroEngine.isAccessibilityConnected
    val selectedMacro: StateFlow<Macro?> = MacroEngine.selectedMacro

    private val _permissionStatus = MutableStateFlow(PermissionManager.getStatus(application))
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    private val _editingMacro = MutableStateFlow<Macro?>(null)
    val editingMacro: StateFlow<Macro?> = _editingMacro.asStateFlow()

    private val _floatingActive = MutableStateFlow(FloatingControlService.isRunning)
    val floatingActive: StateFlow<Boolean> = _floatingActive.asStateFlow()

    fun refreshStatus() {
        val ctx = getApplication<Application>()
        _permissionStatus.value = PermissionManager.getStatus(ctx)
        _floatingActive.value = FloatingControlService.isRunning
    }

    fun startEditingNewMacro() {
        val currentSettings = settings.value
        _editingMacro.value = Macro(
            name = "Macro Mới",
            gameCategory = GameCategory.OTHER,
            defaultSpeed = currentSettings.defaultSpeed,
            repeatMode = currentSettings.defaultRepeatMode,
            instantMode = currentSettings.instantModeDefault
        )
    }

    fun startEditingMacro(macro: Macro) {
        _editingMacro.value = macro
        MacroEngine.selectMacro(macro)
    }

    fun updateEditingMacro(macro: Macro) {
        _editingMacro.value = macro
    }

    fun saveMacro(macro: Macro, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveMacro(macro)
            MacroEngine.selectMacro(macro)
            _editingMacro.value = macro
            onSaved?.invoke()
        }
    }

    fun duplicateMacro(macro: Macro) {
        viewModelScope.launch {
            repository.duplicateMacro(macro)
        }
    }

    fun deleteMacro(id: String) {
        viewModelScope.launch {
            repository.deleteMacro(id)
        }
    }

    fun playMacro(macro: Macro) {
        MacroEngine.play(macro, settings.value.safeTouchRetryOnCancel)
    }

    fun pauseMacro() = MacroEngine.pause()
    fun resumeMacro() = MacroEngine.resume()
    fun stopMacro() = MacroEngine.emergencyStop()

    fun setSpeed(speed: Float) = SpeedEngine.setSpeed(speed)
    fun setInstantMode(enabled: Boolean) = SpeedEngine.setInstantMode(enabled)

    fun exportMacroJson(macro: Macro): String = repository.exportMacroToJson(macro)
    fun exportAllJson(): String = repository.exportAllToJson(macros.value)

    fun importJson(rawJson: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = repository.importFromJson(rawJson)
            onResult(result)
        }
    }

    fun updateSettings(transform: (MacroSettings) -> MacroSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun toggleFloatingControl() {
        val ctx = getApplication<Application>()
        if (FloatingControlService.isRunning) {
            FloatingControlService.stop(ctx)
            _floatingActive.value = false
        } else {
            FloatingControlService.start(ctx)
            _floatingActive.value = true
        }
    }
}
