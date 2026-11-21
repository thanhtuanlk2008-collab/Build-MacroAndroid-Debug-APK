package com.soulknight.matrixai.weapons

data class WeaponProfile(
    val id: String,
    val displayName: String,
    val attackModel: AttackModel,
    val usesEnergy: Boolean,
    val effectiveRangeTiles: Float? = null,
    val recognitionConfidence: Float = 0f
)

enum class AttackModel {
    MELEE,
    PROJECTILE,
    CONTINUOUS,
    CHARGED,
    AREA,
    UNKNOWN
}
