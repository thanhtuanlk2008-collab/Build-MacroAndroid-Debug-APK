package com.macro.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ActionType {
    TAP,
    LONG_PRESS,
    SWIPE,
    WAIT,
    REPEAT
}

@Serializable
data class MacroAction(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    /** Normalized X coordinate in [0.0, 1.0] relative to screen width */
    val x: Float = 0.5f,
    /** Normalized Y coordinate in [0.0, 1.0] relative to screen height */
    val y: Float = 0.5f,
    /** Normalized end X coordinate for SWIPE in [0.0, 1.0] */
    val endX: Float = 0.5f,
    /** Normalized end Y coordinate for SWIPE in [0.0, 1.0] */
    val endY: Float = 0.5f,
    /** Duration of the gesture itself (TAP ~40ms, LONG_PRESS e.g. 1000ms, SWIPE e.g. 300ms, WAIT e.g. 500ms) */
    val durationMs: Long = 50L,
    /** Delay after this action completes before the next action starts */
    val waitAfterMs: Long = 100L,
    /** Used when type == REPEAT or for repeating a group */
    val repeatCount: Int = 1,
    /** Optional group identifier for grouping actions */
    val groupId: String? = null,
    /** Group label if part of a named group */
    val groupName: String? = null,
    /** Execution order index */
    val orderIndex: Int = 0,
    /** Optional user label/note */
    val label: String = ""
)
