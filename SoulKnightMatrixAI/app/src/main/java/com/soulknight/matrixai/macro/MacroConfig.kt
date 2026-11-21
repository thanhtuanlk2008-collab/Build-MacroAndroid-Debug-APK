package com.soulknight.matrixai.macro

data class MacroConfig(
    val attackEnabled: Boolean = true,
    val attackIntervalMs: Long = 250L,
    val skillEnabled: Boolean = true,
    val skillIntervalMs: Long = 5_000L,
    val switchWeaponEnabled: Boolean = false,
    val switchWeaponIntervalMs: Long = 10_000L
) {
    fun intervalFor(action: MacroAction): Long = when (action) {
        MacroAction.ATTACK -> attackIntervalMs
        MacroAction.USE_SKILL -> skillIntervalMs
        MacroAction.SWITCH_WEAPON -> switchWeaponIntervalMs
    }

    fun isEnabled(action: MacroAction): Boolean = when (action) {
        MacroAction.ATTACK -> attackEnabled
        MacroAction.USE_SKILL -> skillEnabled
        MacroAction.SWITCH_WEAPON -> switchWeaponEnabled
    }

    fun validated(): MacroConfig = copy(
        attackIntervalMs = attackIntervalMs.coerceIn(100L, 5_000L),
        skillIntervalMs = skillIntervalMs.coerceIn(500L, 60_000L),
        switchWeaponIntervalMs = switchWeaponIntervalMs.coerceIn(500L, 60_000L)
    )
}
