package com.macro.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.macro.app.model.AccentTheme
import com.macro.app.model.MacroSettings
import com.macro.app.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "macro_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val DEFAULT_REPEAT = stringPreferencesKey("default_repeat")
        val INSTANT_DEFAULT = booleanPreferencesKey("instant_default")
        val FLOATING_ALPHA = floatPreferencesKey("floating_alpha")
        val FLOATING_SCALE = floatPreferencesKey("floating_scale")
        val FLOATING_POS_X = intPreferencesKey("floating_pos_x")
        val FLOATING_POS_Y = intPreferencesKey("floating_pos_y")
        val SAFE_TOUCH_RETRY = booleanPreferencesKey("safe_touch_retry")
        val MIN_GESTURE_INTERVAL = longPreferencesKey("min_gesture_interval")
    }

    val settingsFlow: Flow<MacroSettings> = context.dataStore.data.map { prefs ->
        MacroSettings(
            language = prefs[Keys.LANGUAGE] ?: "vi",
            accentTheme = runCatching {
                AccentTheme.valueOf(prefs[Keys.ACCENT_THEME] ?: AccentTheme.REDMAGIC_CRIMSON.name)
            }.getOrDefault(AccentTheme.REDMAGIC_CRIMSON),
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            defaultRepeatMode = runCatching {
                RepeatMode.valueOf(prefs[Keys.DEFAULT_REPEAT] ?: RepeatMode.ONCE.name)
            }.getOrDefault(RepeatMode.ONCE),
            instantModeDefault = prefs[Keys.INSTANT_DEFAULT] ?: false,
            floatingAlpha = prefs[Keys.FLOATING_ALPHA] ?: 0.88f,
            floatingScale = prefs[Keys.FLOATING_SCALE] ?: 1.0f,
            floatingPosX = prefs[Keys.FLOATING_POS_X] ?: 24,
            floatingPosY = prefs[Keys.FLOATING_POS_Y] ?: 240,
            safeTouchRetryOnCancel = prefs[Keys.SAFE_TOUCH_RETRY] ?: true,
            minGestureIntervalMs = prefs[Keys.MIN_GESTURE_INTERVAL] ?: 20L
        )
    }

    suspend fun updateSettings(transform: (MacroSettings) -> MacroSettings) {
        context.dataStore.edit { prefs ->
            val current = MacroSettings(
                language = prefs[Keys.LANGUAGE] ?: "vi",
                accentTheme = runCatching {
                    AccentTheme.valueOf(prefs[Keys.ACCENT_THEME] ?: AccentTheme.REDMAGIC_CRIMSON.name)
                }.getOrDefault(AccentTheme.REDMAGIC_CRIMSON),
                defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
                defaultRepeatMode = runCatching {
                    RepeatMode.valueOf(prefs[Keys.DEFAULT_REPEAT] ?: RepeatMode.ONCE.name)
                }.getOrDefault(RepeatMode.ONCE),
                instantModeDefault = prefs[Keys.INSTANT_DEFAULT] ?: false,
                floatingAlpha = prefs[Keys.FLOATING_ALPHA] ?: 0.88f,
                floatingScale = prefs[Keys.FLOATING_SCALE] ?: 1.0f,
                floatingPosX = prefs[Keys.FLOATING_POS_X] ?: 24,
                floatingPosY = prefs[Keys.FLOATING_POS_Y] ?: 240,
                safeTouchRetryOnCancel = prefs[Keys.SAFE_TOUCH_RETRY] ?: true,
                minGestureIntervalMs = prefs[Keys.MIN_GESTURE_INTERVAL] ?: 20L
            )
            val updated = transform(current)
            prefs[Keys.LANGUAGE] = updated.language
            prefs[Keys.ACCENT_THEME] = updated.accentTheme.name
            prefs[Keys.DEFAULT_SPEED] = updated.defaultSpeed
            prefs[Keys.DEFAULT_REPEAT] = updated.defaultRepeatMode.name
            prefs[Keys.INSTANT_DEFAULT] = updated.instantModeDefault
            prefs[Keys.FLOATING_ALPHA] = updated.floatingAlpha
            prefs[Keys.FLOATING_SCALE] = updated.floatingScale
            prefs[Keys.FLOATING_POS_X] = updated.floatingPosX
            prefs[Keys.FLOATING_POS_Y] = updated.floatingPosY
            prefs[Keys.SAFE_TOUCH_RETRY] = updated.safeTouchRetryOnCancel
            prefs[Keys.MIN_GESTURE_INTERVAL] = updated.minGestureIntervalMs
        }
    }

    suspend fun updateFloatingPosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FLOATING_POS_X] = x
            prefs[Keys.FLOATING_POS_Y] = y
        }
    }
}
