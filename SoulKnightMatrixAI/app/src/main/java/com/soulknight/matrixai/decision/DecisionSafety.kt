package com.soulknight.matrixai.decision

data class DecisionSafety(
    val minimumConfidence: Float = 0.75f,
    val stopOnUnknownState: Boolean = true,
    val maximumActionDurationMillis: Long = 800L
) {
    fun canAct(confidence: Float, stateKnown: Boolean): Boolean =
        confidence >= minimumConfidence && (stateKnown || !stopOnUnknownState)
}
