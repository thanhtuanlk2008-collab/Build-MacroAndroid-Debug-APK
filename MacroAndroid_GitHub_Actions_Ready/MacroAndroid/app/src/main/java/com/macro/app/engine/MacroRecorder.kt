package com.macro.app.engine

import com.macro.app.model.ActionType
import com.macro.app.model.GameCategory
import com.macro.app.model.Macro
import com.macro.app.model.MacroAction
import com.macro.app.model.MacroState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MacroRecorder {

    private val _recorderState = MutableStateFlow(MacroState.IDLE)
    val recorderState: StateFlow<MacroState> = _recorderState.asStateFlow()

    private val _recordedActions = MutableStateFlow<List<MacroAction>>(emptyList())
    val recordedActions: StateFlow<List<MacroAction>> = _recordedActions.asStateFlow()

    private var lastEventTimestampMs: Long = 0L

    fun startRecording() {
        _recordedActions.value = emptyList()
        lastEventTimestampMs = System.currentTimeMillis()
        _recorderState.value = MacroState.RECORDING
    }

    fun pauseRecording() {
        if (_recorderState.value == MacroState.RECORDING) {
            _recorderState.value = MacroState.PAUSED
        }
    }

    fun resumeRecording() {
        if (_recorderState.value == MacroState.PAUSED) {
            lastEventTimestampMs = System.currentTimeMillis()
            _recorderState.value = MacroState.RECORDING
        }
    }

    fun cancelRecording() {
        _recordedActions.value = emptyList()
        _recorderState.value = MacroState.IDLE
    }

    fun recordTap(normX: Float, normY: Float, durationMs: Long = 45L, label: String = "") {
        if (_recorderState.value != MacroState.RECORDING) return
        appendWaitIfNeeded()
        addAction(
            MacroAction(
                type = ActionType.TAP,
                x = normX.coerceIn(0f, 1f),
                y = normY.coerceIn(0f, 1f),
                durationMs = durationMs.coerceAtLeast(25L),
                waitAfterMs = 120L,
                orderIndex = _recordedActions.value.size,
                label = label
            )
        )
    }

    fun recordLongPress(normX: Float, normY: Float, durationMs: Long = 800L, label: String = "") {
        if (_recorderState.value != MacroState.RECORDING) return
        appendWaitIfNeeded()
        addAction(
            MacroAction(
                type = ActionType.LONG_PRESS,
                x = normX.coerceIn(0f, 1f),
                y = normY.coerceIn(0f, 1f),
                durationMs = durationMs.coerceAtLeast(150L),
                waitAfterMs = 150L,
                orderIndex = _recordedActions.value.size,
                label = label
            )
        )
    }

    fun recordSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L,
        label: String = ""
    ) {
        if (_recorderState.value != MacroState.RECORDING) return
        appendWaitIfNeeded()
        addAction(
            MacroAction(
                type = ActionType.SWIPE,
                x = startX.coerceIn(0f, 1f),
                y = startY.coerceIn(0f, 1f),
                endX = endX.coerceIn(0f, 1f),
                endY = endY.coerceIn(0f, 1f),
                durationMs = durationMs.coerceAtLeast(50L),
                waitAfterMs = 150L,
                orderIndex = _recordedActions.value.size,
                label = label
            )
        )
    }

    fun recordWait(durationMs: Long = 500L) {
        if (_recorderState.value != MacroState.RECORDING) return
        addAction(
            MacroAction(
                type = ActionType.WAIT,
                durationMs = durationMs.coerceAtLeast(50L),
                waitAfterMs = 0L,
                orderIndex = _recordedActions.value.size
            )
        )
        lastEventTimestampMs = System.currentTimeMillis()
    }

    fun removeLastAction() {
        _recordedActions.update { list ->
            if (list.isEmpty()) list else list.dropLast(1)
        }
    }

    fun finishAndBuildMacro(
        name: String,
        gameCategory: GameCategory = GameCategory.OTHER
    ): Macro {
        val actions = _recordedActions.value.mapIndexed { index, action ->
            action.copy(orderIndex = index)
        }
        _recorderState.value = MacroState.IDLE
        _recordedActions.value = emptyList()

        return Macro(
            name = name.ifBlank { "Macro ${System.currentTimeMillis() % 10000}" },
            gameCategory = gameCategory,
            defaultSpeed = SpeedEngine.speedMultiplier.value,
            instantMode = SpeedEngine.instantMode.value,
            actions = actions
        )
    }

    private fun appendWaitIfNeeded() {
        val now = System.currentTimeMillis()
        val gap = now - lastEventTimestampMs
        if (_recordedActions.value.isNotEmpty() && gap > 200L) {
            addAction(
                MacroAction(
                    type = ActionType.WAIT,
                    durationMs = gap.coerceAtMost(10_000L),
                    waitAfterMs = 0L,
                    orderIndex = _recordedActions.value.size
                )
            )
        }
        lastEventTimestampMs = now
    }

    private fun addAction(action: MacroAction) {
        _recordedActions.update { it + action }
    }
}
