package com.soulknight.matrixai.input

import android.content.Context

/** Persists user-entered screen coordinates. Coordinates are not guessed automatically. */
class InputTargetStore(context: Context) {
    private val prefs = context.getSharedPreferences("matrix_input_targets", Context.MODE_PRIVATE)

    fun load(): InputTargetConfig = InputTargetConfig(
        attack = read("attack"),
        skill = read("skill"),
        switchWeapon = read("switch_weapon")
    )

    fun save(config: InputTargetConfig) {
        prefs.edit().apply {
            putTarget("attack", config.attack)
            putTarget("skill", config.skill)
            putTarget("switch_weapon", config.switchWeapon)
        }.apply()
    }

    private fun read(prefix: String): InputTarget? {
        if (!prefs.contains("${prefix}_x") || !prefs.contains("${prefix}_y")) return null
        val target = InputTarget(
            prefs.getFloat("${prefix}_x", -1f),
            prefs.getFloat("${prefix}_y", -1f)
        )
        return target.takeIf(InputTarget::isValid)
    }

    private fun android.content.SharedPreferences.Editor.putTarget(prefix: String, target: InputTarget?) {
        if (target == null || !target.isValid()) {
            remove("${prefix}_x").remove("${prefix}_y")
        } else {
            putFloat("${prefix}_x", target.x).putFloat("${prefix}_y", target.y)
        }
    }
}
