package com.macro.app.input

/**
 * Tracks logical touch sessions by source and pointer ID.
 *
 * IMPORTANT:
 * This is bookkeeping only. It does not create Android MotionEvent streams,
 * inject input, intercept the touchscreen, or guarantee physical/macro
 * concurrency. Android's input dispatcher still decides how actual events
 * are delivered.
 */
enum class TouchSource {
    PHYSICAL_OBSERVED,
    MACRO
}

data class TouchSessionKey(
    val source: TouchSource,
    val pointerId: Int
)

data class TouchSample(
    val x: Float,
    val y: Float,
    val eventTimeMillis: Long
)

class TouchSessionManager {
    private val sessions = mutableMapOf<TouchSessionKey, TouchSample>()

    @Synchronized
    fun begin(key: TouchSessionKey, sample: TouchSample): Boolean {
        if (sessions.containsKey(key)) return false
        sessions[key] = sample
        return true
    }

    @Synchronized
    fun update(key: TouchSessionKey, sample: TouchSample): Boolean {
        if (!sessions.containsKey(key)) return false
        sessions[key] = sample
        return true
    }

    @Synchronized
    fun end(key: TouchSessionKey): TouchSample? = sessions.remove(key)

    @Synchronized
    fun cancel(source: TouchSource) {
        sessions.keys.removeAll { it.source == source }
    }

    @Synchronized
    fun snapshot(): Map<TouchSessionKey, TouchSample> = sessions.toMap()

    @Synchronized
    fun activeCount(source: TouchSource? = null): Int =
        if (source == null) sessions.size else sessions.keys.count { it.source == source }
}
