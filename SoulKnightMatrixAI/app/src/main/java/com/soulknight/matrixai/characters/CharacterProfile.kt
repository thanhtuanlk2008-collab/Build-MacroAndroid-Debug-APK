package com.soulknight.matrixai.characters

data class CharacterProfile(
    val id: String,
    val displayName: String,
    val movementModel: MovementModel = MovementModel.STANDARD,
    val skillCooldownMillis: Long? = null,
    val metadataConfidence: Float = 0f
)

enum class MovementModel {
    STANDARD,
    DASH,
    TELEPORT,
    UNKNOWN
}
