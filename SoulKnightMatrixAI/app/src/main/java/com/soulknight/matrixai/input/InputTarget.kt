package com.soulknight.matrixai.input

/** Screen coordinate configured by the user for a specific game control. */
data class InputTarget(val x: Float, val y: Float) {
    fun isValid(): Boolean = x.isFinite() && y.isFinite() && x >= 0f && y >= 0f
}

data class InputTargetConfig(
    val attack: InputTarget? = null,
    val skill: InputTarget? = null,
    val switchWeapon: InputTarget? = null
) {
    fun forAction(action: com.soulknight.matrixai.macro.MacroAction): InputTarget? =
        when (action) {
            com.soulknight.matrixai.macro.MacroAction.ATTACK -> attack
            com.soulknight.matrixai.macro.MacroAction.USE_SKILL -> skill
            com.soulknight.matrixai.macro.MacroAction.SWITCH_WEAPON -> switchWeapon
        }
}
