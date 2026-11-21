package com.soulknight.matrixai.navigation

/**
 * Remembers rooms discovered during the current Matrix run.
 *
 * Room IDs are visual fingerprints, not floor/layout identifiers. The graph is rebuilt
 * from observed transitions so different generated floor layouts are supported.
 */
data class RememberedRoom(
    val fingerprint: Long,
    val kind: RoomKind,
    val confidence: Float,
    val cleared: Boolean = false
) {
    init { require(confidence in 0f..1f) }
}

class RoomExplorationMemory {
    private val rooms = linkedMapOf<Long, RememberedRoom>()
    private val edges = linkedMapOf<Long, MutableSet<Long>>()
    private var previousFingerprint: Long? = null

    @Synchronized
    fun observe(room: RememberedRoom) {
        val old = rooms[room.fingerprint]
        rooms[room.fingerprint] = when {
            old == null -> room
            room.confidence >= old.confidence -> room.copy(cleared = old.cleared || room.cleared)
            else -> old.copy(cleared = old.cleared || room.cleared)
        }
        edges.getOrPut(room.fingerprint) { linkedSetOf() }
        previousFingerprint?.let { previous ->
            if (previous != room.fingerprint) {
                edges.getOrPut(previous) { linkedSetOf() }.add(room.fingerprint)
                edges.getOrPut(room.fingerprint) { linkedSetOf() }.add(previous)
            }
        }
        previousFingerprint = room.fingerprint
    }

    /** Adds a door/exit edge discovered by the current room scan. */
    @Synchronized
    fun connectRooms(fromFingerprint: Long, toFingerprint: Long) {
        require(fromFingerprint != toFingerprint)
        if (fromFingerprint !in rooms || toFingerprint !in rooms) return
        edges.getOrPut(fromFingerprint) { linkedSetOf() }.add(toFingerprint)
        edges.getOrPut(toFingerprint) { linkedSetOf() }.add(fromFingerprint)
    }

    @Synchronized
    fun markCleared(fingerprint: Long) {
        rooms[fingerprint]?.let { rooms[fingerprint] = it.copy(cleared = true) }
    }

    /** Returns only directly discovered, uncleared non-special destinations. */
    @Synchronized
    fun nextRoom(fromFingerprint: Long): RememberedRoom? =
        edges[fromFingerprint].orEmpty()
            .mapNotNull(rooms::get)
            .filter { !it.cleared && it.kind != RoomKind.SPECIAL }
            .sortedWith(
                compareBy<RememberedRoom> { priority(it.kind) }
                    .thenByDescending { it.confidence }
                    .thenBy { it.fingerprint }
            )
            .firstOrNull()

    @Synchronized
    fun snapshot(): List<RememberedRoom> = rooms.values.toList()

    @Synchronized
    fun reset() {
        rooms.clear()
        edges.clear()
        previousFingerprint = null
    }

    private fun priority(kind: RoomKind): Int = when (kind) {
        RoomKind.ENEMY -> 0
        RoomKind.BOSS -> 1
        RoomKind.UNKNOWN -> 2
        RoomKind.SPECIAL -> 3
    }
}
