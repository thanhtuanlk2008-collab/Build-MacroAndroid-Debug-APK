package com.soulknight.matrixai.matrix

enum class MatrixState {
    IDLE,
    CAPTURING,
    OBSERVING,
    EXPLORING,
    COMBAT,
    ROOM_CLEAR,
    REWARD,
    TRANSITION,
    BOSS,
    UNKNOWN,
    STOPPED
}

data class MatrixSnapshot(
    val state: MatrixState = MatrixState.IDLE,
    val floor: Int? = null,
    val roomIndex: Int? = null,
    val confidence: Float = 0f,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
