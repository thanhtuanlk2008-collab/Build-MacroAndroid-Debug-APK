package com.soulknight.matrixai.movement

import android.content.Context

/** Persists the last measured movement profile locally on this device. */
class MovementProfileStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(profile: MovementProfile) {
        preferences.edit()
            .putFloat(KEY_HORIZONTAL, profile.horizontalPixelsPerSecond)
            .putFloat(KEY_VERTICAL, profile.verticalPixelsPerSecond)
            .putFloat(KEY_DIAGONAL, profile.diagonalPixelsPerSecond)
            .putLong(KEY_RESPONSE, profile.responseDelayMillis)
            .putLong(KEY_STOP, profile.stopDelayMillis)
            .putFloat(KEY_CONFIDENCE, profile.confidence)
            .putLong(KEY_UPDATED, profile.updatedAtMillis)
            .apply()
    }

    fun load(): MovementProfile {
        if (!preferences.contains(KEY_UPDATED)) return MovementProfile.UNCALIBRATED
        return MovementProfile(
            horizontalPixelsPerSecond = preferences.getFloat(KEY_HORIZONTAL, 0f),
            verticalPixelsPerSecond = preferences.getFloat(KEY_VERTICAL, 0f),
            diagonalPixelsPerSecond = preferences.getFloat(KEY_DIAGONAL, 0f),
            responseDelayMillis = preferences.getLong(KEY_RESPONSE, 0L),
            stopDelayMillis = preferences.getLong(KEY_STOP, 0L),
            confidence = preferences.getFloat(KEY_CONFIDENCE, 0f),
            updatedAtMillis = preferences.getLong(KEY_UPDATED, 0L)
        )
    }

    fun clear() { preferences.edit().clear().apply() }

    companion object {
        private const val PREFERENCES = "matrix_movement_profile"
        private const val KEY_HORIZONTAL = "horizontal_px_s"
        private const val KEY_VERTICAL = "vertical_px_s"
        private const val KEY_DIAGONAL = "diagonal_px_s"
        private const val KEY_RESPONSE = "response_ms"
        private const val KEY_STOP = "stop_ms"
        private const val KEY_CONFIDENCE = "confidence"
        private const val KEY_UPDATED = "updated_at"
    }
}
