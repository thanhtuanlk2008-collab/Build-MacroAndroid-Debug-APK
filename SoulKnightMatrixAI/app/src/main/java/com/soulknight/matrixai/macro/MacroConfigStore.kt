package com.soulknight.matrixai.macro

import android.content.Context

/**
 * Persists the legacy default configuration and named user profiles.
 * Existing installations keep their current default settings.
 */
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
        writeConfig("", config)
    }

    fun profileNames(): List<String> {
        val stored = preferences.getStringSet(PROFILE_NAMES_KEY, emptySet()).orEmpty()
        return (stored + DEFAULT_PROFILE).toList().sorted()
    }

    fun loadProfile(name: String): MacroConfig {
        val safeName = validateProfileName(name)
        if (safeName == DEFAULT_PROFILE) return load()
        val prefix = profilePrefix(safeName)
        if (!preferences.contains(prefix + "attack_enabled")) return MacroConfig()
        return readConfig(prefix)
    }

    fun saveProfile(name: String, config: MacroConfig) {
        val safeName = validateProfileName(name)
        if (safeName == DEFAULT_PROFILE) {
            save(config)
            return
        }
        writeConfig(profilePrefix(safeName), config)
        val names = preferences.getStringSet(PROFILE_NAMES_KEY, emptySet()).orEmpty().toMutableSet()
        names.add(safeName)
        preferences.edit().putStringSet(PROFILE_NAMES_KEY, names).apply()
    }

    fun deleteProfile(name: String): Boolean {
        val safeName = validateProfileName(name)
        if (safeName == DEFAULT_PROFILE) return false
        val prefix = profilePrefix(safeName)
        preferences.edit()
            .remove(prefix + "attack_enabled")
            .remove(prefix + "attack_interval")
            .remove(prefix + "skill_enabled")
            .remove(prefix + "skill_interval")
            .remove(prefix + "switch_enabled")
            .remove(prefix + "switch_interval")
            .apply()
        val names = preferences.getStringSet(PROFILE_NAMES_KEY, emptySet()).orEmpty().toMutableSet()
        val removed = names.remove(safeName)
        preferences.edit().putStringSet(PROFILE_NAMES_KEY, names).apply()
        return removed
    }

    private fun readConfig(prefix: String): MacroConfig = MacroConfig(
        attackEnabled = preferences.getBoolean(prefix + "attack_enabled", true),
        attackIntervalMs = preferences.getLong(prefix + "attack_interval", 250L),
        skillEnabled = preferences.getBoolean(prefix + "skill_enabled", true),
        skillIntervalMs = preferences.getLong(prefix + "skill_interval", 5_000L),
        switchWeaponEnabled = preferences.getBoolean(prefix + "switch_enabled", false),
        switchWeaponIntervalMs = preferences.getLong(prefix + "switch_interval", 10_000L)
    ).validated()

    private fun writeConfig(prefix: String, config: MacroConfig) {
        val safe = config.validated()
        preferences.edit()
            .putBoolean(prefix + "attack_enabled", safe.attackEnabled)
            .putLong(prefix + "attack_interval", safe.attackIntervalMs)
            .putBoolean(prefix + "skill_enabled", safe.skillEnabled)
            .putLong(prefix + "skill_interval", safe.skillIntervalMs)
            .putBoolean(prefix + "switch_enabled", safe.switchWeaponEnabled)
            .putLong(prefix + "switch_interval", safe.switchWeaponIntervalMs)
            .apply()
    }

    private fun validateProfileName(name: String): String {
        val safe = name.trim()
        require(safe.isNotEmpty()) { "Tên cấu hình không được để trống" }
        require(safe.length <= MAX_PROFILE_NAME_LENGTH) { "Tên cấu hình tối đa 32 ký tự" }
        return safe
    }

    private fun profilePrefix(name: String) = "profile:$name:"

    companion object {
        const val DEFAULT_PROFILE = "Mặc định"
        private const val PROFILE_NAMES_KEY = "profile_names"
        private const val MAX_PROFILE_NAME_LENGTH = 32
    }
}
