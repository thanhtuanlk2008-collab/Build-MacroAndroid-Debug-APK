package com.soulknight.matrixai.input

/** Result returned by the permission-gated input gateway. */
sealed interface InputResult {
    data object Dispatched : InputResult
    data object ServiceUnavailable : InputResult
    data object InvalidCoordinates : InputResult
    data object InvalidDuration : InputResult
    data object Cancelled : InputResult
    data class Failed(val reason: String) : InputResult
}
