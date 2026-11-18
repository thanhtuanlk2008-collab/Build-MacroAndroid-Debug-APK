package com.macro.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccentTheme(val displayName: String) {
    REDMAGIC_CRIMSON("Crimson Red"),
    CYBER_BLUE("Cyber Blue")
}

@Serializable
data class MacroSettings(
    val language: String = "vi",
    val accentTheme: AccentTheme = AccentTheme.REDMAGIC_CRIMSON,
    val defaultSpeed: Float = 1.0f,
    val defaultRepeatMode: RepeatMode = RepeatMode.ONCE,
    val instantModeDefault: Boolean = false,
    val floatingAlpha: Float = 0.88f,
    val floatingScale: Float = 1.0f,
    val floatingPosX: Int = 24,
    val floatingPosY: Int = 240,
    val safeTouchRetryOnCancel: Boolean = true,
    val minGestureIntervalMs: Long = 20L
)
