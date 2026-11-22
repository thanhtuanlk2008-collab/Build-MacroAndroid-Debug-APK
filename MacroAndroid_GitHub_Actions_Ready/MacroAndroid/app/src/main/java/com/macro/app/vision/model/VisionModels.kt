package com.macro.app.vision.model

import android.graphics.Bitmap

data class VisionFrame(
    val bitmap: Bitmap,
    val timestampMs: Long,
    val originalWidth: Int,
    val originalHeight: Int
) {
    init {
        require(originalWidth > 0 && originalHeight > 0) { "Frame dimensions must be positive" }
        require(timestampMs >= 0L) { "Frame timestamp cannot be negative" }
    }
}

data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()) {
            "Bounding box coordinates must be finite"
        }
        require(right > left && bottom > top) { "Bounding box must have positive area" }
    }

    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f
}

enum class EntityType { PLAYER, ENEMY, BOSS, PROJECTILE, WEAPON, CHEST, ITEM, EXIT, UNKNOWN }

data class Detection(
    val type: EntityType,
    val label: String,
    val confidence: Float,
    val bounds: BoundingBox,
    val timestampMs: Long
) {
    init {
        require(label.isNotBlank()) { "Detection label cannot be blank" }
        require(confidence.isFinite() && confidence in 0f..1f) {
            "Detection confidence must be between 0 and 1"
        }
        require(timestampMs >= 0L) { "Detection timestamp cannot be negative" }
    }
}

data class HudState(
    val healthRatio: Float? = null,
    val energyRatio: Float? = null,
    val coins: Int? = null
) {
    init {
        require(healthRatio == null || (healthRatio.isFinite() && healthRatio in 0f..1f)) {
            "healthRatio must be between 0 and 1"
        }
        require(energyRatio == null || (energyRatio.isFinite() && energyRatio in 0f..1f)) {
            "energyRatio must be between 0 and 1"
        }
        require(coins == null || coins >= 0) { "coins cannot be negative" }
    }
}

enum class GamePhase { UNKNOWN, MENU, EXPLORATION, COMBAT, BOSS_FIGHT, REWARD, TRANSITION, DEFEAT }

data class GameState(
    val phase: GamePhase = GamePhase.UNKNOWN,
    val detections: List<Detection> = emptyList(),
    val hud: HudState = HudState(),
    val frameTimestampMs: Long = 0L,
    val confidence: Float = 0f
)

data class VisionResult(
    val state: GameState,
    val processingTimeMs: Long,
    val frameWidth: Int,
    val frameHeight: Int
)

data class VisionTemplate(
    val label: String,
    val type: EntityType,
    val bitmap: Bitmap,
    val threshold: Float = 0.18f
) {
    init {
        require(label.isNotBlank()) { "Template label cannot be blank" }
        require(threshold.isFinite() && threshold > 0f && threshold <= 1f) {
            "Template threshold must be greater than 0 and at most 1"
        }
    }
}
