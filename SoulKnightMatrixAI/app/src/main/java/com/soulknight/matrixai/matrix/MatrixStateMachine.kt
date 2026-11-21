package com.soulknight.matrixai.matrix

class MatrixStateMachine(initial: MatrixState = MatrixState.IDLE) {
    var current: MatrixState = initial
        private set

    fun transitionTo(next: MatrixState, confidence: Float): Boolean {
        if (confidence !in 0f..1f) return false
        if (confidence < MIN_TRANSITION_CONFIDENCE && next != MatrixState.UNKNOWN && next != MatrixState.STOPPED) {
            current = MatrixState.UNKNOWN
            return false
        }
        if (next == current) return true
        if (next !in allowed[current].orEmpty()) return false
        current = next
        return true
    }

    companion object {
        private const val MIN_TRANSITION_CONFIDENCE = 0.70f
        private val allowed = mapOf(
            MatrixState.IDLE to setOf(MatrixState.CAPTURING, MatrixState.STOPPED),
            MatrixState.CAPTURING to setOf(MatrixState.OBSERVING, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.OBSERVING to setOf(MatrixState.EXPLORING, MatrixState.COMBAT, MatrixState.ROOM_CLEAR, MatrixState.TRANSITION, MatrixState.BOSS, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.EXPLORING to setOf(MatrixState.OBSERVING, MatrixState.COMBAT, MatrixState.ROOM_CLEAR, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.COMBAT to setOf(MatrixState.OBSERVING, MatrixState.ROOM_CLEAR, MatrixState.BOSS, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.ROOM_CLEAR to setOf(MatrixState.REWARD, MatrixState.TRANSITION, MatrixState.OBSERVING, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.REWARD to setOf(MatrixState.TRANSITION, MatrixState.OBSERVING, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.TRANSITION to setOf(MatrixState.OBSERVING, MatrixState.BOSS, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.BOSS to setOf(MatrixState.COMBAT, MatrixState.ROOM_CLEAR, MatrixState.UNKNOWN, MatrixState.STOPPED),
            MatrixState.UNKNOWN to setOf(MatrixState.OBSERVING, MatrixState.STOPPED),
            MatrixState.STOPPED to setOf(MatrixState.IDLE)
        )
    }
}
