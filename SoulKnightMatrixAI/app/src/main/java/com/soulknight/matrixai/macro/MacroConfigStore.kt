package com.soulknight.matrixai.macro

import android.content.Context

class MacroConfigStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "matrix_macro_settings", Context.MODE_PRIVATE
    )

    fun load(): MacroConfig = MacroConfig(
        attackEnabled = preferences.getBoolean("attack_enabled", true),
        attackIntervalMs = preferences.getLong("attack_interval", 250L),
        skillEnabled = preferences.getBoolean("skill_enabled", true),
        skillIntervalMs = preferences.getLong("skill_interval", 5_000L),
        switchWeaponEnabled = preferences.getBoolean("switch_enabled", false),
        switchWeaponIntervalMs = preferences.getLong("switch_interval", 10_000L)
    ).validated()

    fun save(config: MacroConfig) {
        val safe = config.validated()
        preferences.edit()
            .putBoolean("attack_enabled", safe.attackEnabled)
            .putLong("attack_interval", safe.attackIntervalMs)
            .putBoolean("skill_enabled", safe.skillEnabled)
            .putLong("skill_interval", safe.skillIntervalMs)
            .putBoolean("switch_enabled", safe.switchWeaponEnabled)
            .putLong("switch_interval", safe.switchWeaponIntervalMs)
            .apply()
    }
}
