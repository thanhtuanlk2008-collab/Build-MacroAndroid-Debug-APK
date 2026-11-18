package com.macro.app.engine

import com.macro.app.model.ActionType
import com.macro.app.model.Macro
import com.macro.app.model.MacroAction
import com.macro.app.model.MacroState
import com.macro.app.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackTelemetry(
    val state: MacroState = MacroState.IDLE,
    val activeMacroId: String? = null,
    val activeMacroName: String = "",
    val currentActionIndex: Int = 0,
    val totalActions: Int = 0,
    val currentLoop: Int = 0,
    val targetLoops: Int = 1,
    val elapsedMs: Long = 0L,
    val executedGestures: Long = 0L,
    val cancelledGestures: Long = 0L,
    val lastError: String? = null
)

class MacroScheduler(
    private val gestureExecutor: GestureExecutor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null

    private val _isPaused = MutableStateFlow(false)

    private val _telemetry = MutableStateFlow(PlaybackTelemetry())
    val telemetry: StateFlow<PlaybackTelemetry> = _telemetry.asStateFlow()

    fun start(macro: Macro, retryOnTouchConflict: Boolean = true) {
        if (macro.actions.isEmpty()) {
            _telemetry.update {
                it.copy(
                    state = MacroState.ERROR,
                    activeMacroName = macro.name,
                    lastError = "Macro không có hành động nào để chạy."
                )
            }
            return
        }

        // Prevent duplicate concurrent execution
        stopInternal(resetToIdle = false)

        _isPaused.value = false
        val targetLoops = resolveTargetLoops(macro)
        val expandedActions = expandActionsWithRepeatGroups(macro.actions.sortedBy { it.orderIndex })

        _telemetry.value = PlaybackTelemetry(
            state = MacroState.PLAYING,
            activeMacroId = macro.id,
            activeMacroName = macro.name,
            currentActionIndex = 0,
            totalActions = expandedActions.size,
            currentLoop = 1,
            targetLoops = targetLoops,
            elapsedMs = 0L,
            executedGestures = 0L,
            cancelledGestures = 0L,
            lastError = null
        )

        val startTimeMs = System.currentTimeMillis()

        playbackJob = scope.launch {
            try {
                var loop = 1
                while (isActive && (targetLoops == -1 || loop <= targetLoops)) {
                    _telemetry.update { it.copy(currentLoop = loop) }

                    for ((index, action) in expandedActions.withIndex()) {
                        if (!isActive) break

                        awaitIfPaused()
                        if (!isActive) break

                        _telemetry.update {
                            it.copy(
                                currentActionIndex = index + 1,
                                elapsedMs = System.currentTimeMillis() - startTimeMs
                            )
                        }

                        if (action.type == ActionType.WAIT) {
                            val waitDuration = SpeedEngine.computeGestureDuration(ActionType.WAIT, action.durationMs)
                            if (waitDuration > 0L) {
                                delayInterruptible(waitDuration)
                            }
                            continue
                        }

                        if (action.type == ActionType.REPEAT) {
                            continue
                        }

                        val effectiveDuration = SpeedEngine.computeGestureDuration(action.type, action.durationMs)
                        val result = gestureExecutor.executeAction(action, effectiveDuration)

                        when (result) {
                            GestureExecutionStatus.COMPLETED -> {
                                _telemetry.update {
                                    it.copy(
                                        executedGestures = it.executedGestures + 1,
                                        elapsedMs = System.currentTimeMillis() - startTimeMs
                                    )
                                }
                            }
                            GestureExecutionStatus.CANCELLED_CONFLICT -> {
                                _telemetry.update {
                                    it.copy(
                                        cancelledGestures = it.cancelledGestures + 1,
                                        elapsedMs = System.currentTimeMillis() - startTimeMs
                                    )
                                }
                                if (!retryOnTouchConflict) {
                                    // Continue gracefully without blocking user's manual touch
                                }
                            }
                            GestureExecutionStatus.SERVICE_UNAVAILABLE -> {
                                _telemetry.update {
                                    it.copy(
                                        state = MacroState.ERROR,
                                        lastError = "AccessibilityService chưa được bật hoặc đã bị ngắt kết nối."
                                    )
                                }
                                return@launch
                            }
                            GestureExecutionStatus.INVALID_PARAMETERS -> {
                                _telemetry.update {
                                    it.copy(
                                        state = MacroState.ERROR,
                                        lastError = "Thao tác không hợp lệ tại bước #${index + 1}."
                                    )
                                }
                                return@launch
                            }
                        }

                        val postDelay = SpeedEngine.computeWaitDelay(action.waitAfterMs)
                        if (postDelay > 0L && isActive) {
                            delayInterruptible(postDelay)
                        }
                    }

                    loop++
                }

                if (isActive) {
                    _telemetry.update {
                        it.copy(
                            state = MacroState.STOPPED,
                            elapsedMs = System.currentTimeMillis() - startTimeMs
                        )
                    }
                    delay(300)
                    _telemetry.update { it.copy(state = MacroState.IDLE) }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Expected on stop()
            } catch (e: Exception) {
                _telemetry.update {
                    it.copy(
                        state = MacroState.ERROR,
                        lastError = e.message ?: "Lỗi không xác định khi chạy Macro"
                    )
                }
            }
        }
    }

    fun pause() {
        if (_telemetry.value.state == MacroState.PLAYING) {
            _isPaused.value = true
            _telemetry.update { it.copy(state = MacroState.PAUSED) }
        }
    }

    fun resume() {
        if (_telemetry.value.state == MacroState.PAUSED) {
            _isPaused.value = false
            _telemetry.update { it.copy(state = MacroState.PLAYING) }
        }
    }

    fun stop(errorMessage: String? = null) {
        stopInternal(resetToIdle = errorMessage == null, errorMessage = errorMessage)
    }

    private fun stopInternal(resetToIdle: Boolean, errorMessage: String? = null) {
        playbackJob?.cancel()
        playbackJob = null
        _isPaused.value = false

        _telemetry.update {
            it.copy(
                state = if (errorMessage != null) MacroState.ERROR else if (resetToIdle) MacroState.IDLE else MacroState.STOPPED,
                lastError = errorMessage ?: it.lastError
            )
        }
    }

    private suspend fun awaitIfPaused() {
        if (_isPaused.value) {
            _isPaused.first { !it }
        }
    }

    private suspend fun delayInterruptible(totalMs: Long) {
        var remaining = totalMs
        val chunk = 50L
        while (remaining > 0L) {
            awaitIfPaused()
            val step = minOf(remaining, chunk)
            delay(step)
            remaining -= step
        }
    }

    private fun resolveTargetLoops(macro: Macro): Int {
        return when (macro.repeatMode) {
            RepeatMode.ONCE -> 1
            RepeatMode.FIVE_TIMES -> 5
            RepeatMode.TEN_TIMES -> 10
            RepeatMode.CUSTOM -> macro.customRepeatCount.coerceAtLeast(1)
            RepeatMode.INFINITE -> -1
        }
    }

    /**
     * Expands grouped REPEAT actions or standalone REPEAT blocks into flat execution sequence.
     */
    private fun expandActionsWithRepeatGroups(actions: List<MacroAction>): List<MacroAction> {
        val result = mutableListOf<MacroAction>()
        var i = 0
        while (i < actions.size) {
            val current = actions[i]
            if (current.groupId != null) {
                val gid = current.groupId
                val groupActions = mutableListOf<MacroAction>()
                var repeatTimes = current.repeatCount.coerceAtLeast(1)
                while (i < actions.size && actions[i].groupId == gid) {
                    val item = actions[i]
                    if (item.type == ActionType.REPEAT) {
                        repeatTimes = item.repeatCount.coerceAtLeast(1)
                    } else {
                        groupActions.add(item)
                    }
                    i++
                }
                repeat(repeatTimes) {
                    result.addAll(groupActions)
                }
            } else if (current.type == ActionType.REPEAT) {
                // Standalone REPEAT repeats the preceding action(s) if no groupId is set
                val repeatTimes = (current.repeatCount - 1).coerceAtLeast(0)
                if (result.isNotEmpty() && repeatTimes > 0) {
                    val last = result.last()
                    repeat(repeatTimes) { result.add(last) }
                }
                i++
            } else {
                result.add(current)
                i++
            }
        }
        return result
    }
}
