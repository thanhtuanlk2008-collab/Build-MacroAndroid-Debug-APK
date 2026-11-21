package com.macro.app.vision.model

import android.graphics.Bitmap

data class VisionFrame(val bitmap: Bitmap, val timestampMs: Long, val originalWidth: Int, val originalHeight: Int)
data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f
}
enum class EntityType { PLAYER, ENEMY, BOSS, PROJECTILE, WEAPON, CHEST, ITEM, EXIT, UNKNOWN }
data class Detection(val type: EntityType, val label: String, val confidence: Float, val bounds: BoundingBox, val timestampMs: Long)
data class HudState(val healthRatio: Float? = null, val energyRatio: Float? = null, val coins: Int? = null)
enum class GamePhase { UNKNOWN, MENU, EXPLORATION, COMBAT, BOSS_FIGHT, REWARD, TRANSITION, DEFEAT }
data class GameState(val phase: GamePhase = GamePhase.UNKNOWN, val detections: List<Detection> = emptyList(), val hud: HudState = HudState(), val frameTimestampMs: Long = 0L, val confidence: Float = 0f)
data class VisionResult(val state: GameState, val processingTimeMs: Long, val frameWidth: Int, val frameHeight: Int)
data class VisionTemplate(val label: String, val type: EntityType, val bitmap: Bitmap, val threshold: Float = 0.18f)
