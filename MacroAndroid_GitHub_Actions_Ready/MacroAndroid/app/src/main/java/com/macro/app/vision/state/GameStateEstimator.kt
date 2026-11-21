package com.macro.app.vision.state

import com.macro.app.vision.model.*

class GameStateEstimator {
    fun estimate(detections: List<Detection>, hud: HudState = HudState(), timestampMs: Long): GameState {
        val hasBoss = detections.any { it.type == EntityType.BOSS }
        val enemyCount = detections.count { it.type == EntityType.ENEMY }
        val hasExit = detections.any { it.type == EntityType.EXIT }
        val hasReward = detections.any { it.type == EntityType.CHEST || it.type == EntityType.ITEM }
        val phase = when {
            hasBoss -> GamePhase.BOSS_FIGHT
            enemyCount > 0 -> GamePhase.COMBAT
            hasReward -> GamePhase.REWARD
            hasExit -> GamePhase.EXPLORATION
            else -> GamePhase.UNKNOWN
        }
        val relevant = when (phase) {
            GamePhase.BOSS_FIGHT -> detections.filter { it.type == EntityType.BOSS }
            GamePhase.COMBAT -> detections.filter { it.type == EntityType.ENEMY }
            else -> detections
        }
        return GameState(phase, detections, hud, timestampMs,
            relevant.maxOfOrNull { it.confidence } ?: 0f)
    }
}
