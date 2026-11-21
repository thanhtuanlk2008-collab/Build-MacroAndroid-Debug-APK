package com.soulknight.matrixai.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomExplorationMemoryTest {
    @Test fun prioritizesEnemyRoomsAndSkipsSpecialRooms() {
        val memory = RoomExplorationMemory()
        memory.observe(RememberedRoom(1L, RoomKind.UNKNOWN, 0.95f))
        memory.observe(RememberedRoom(2L, RoomKind.SPECIAL, 0.99f))
        memory.observe(RememberedRoom(3L, RoomKind.BOSS, 0.90f))
        memory.observe(RememberedRoom(4L, RoomKind.ENEMY, 0.72f))

        assertEquals(4L, memory.nextRoom(1L)?.fingerprint)
    }

    @Test fun doesNotRevisitClearedRoomsAndReturnsNullWhenOnlySpecialExitExists() {
        val memory = RoomExplorationMemory()
        memory.observe(RememberedRoom(10L, RoomKind.ENEMY, 0.9f))
        memory.observe(RememberedRoom(11L, RoomKind.SPECIAL, 0.99f))
        memory.markCleared(10L)

        assertNull(memory.nextRoom(10L))
    }

    @Test fun resetClearsRunHistory() {
        val memory = RoomExplorationMemory()
        memory.observe(RememberedRoom(1L, RoomKind.ENEMY, 0.9f))
        memory.observe(RememberedRoom(2L, RoomKind.ENEMY, 0.9f))
        memory.reset()

        assertEquals(0, memory.snapshot().size)
        assertNull(memory.nextRoom(1L))
    }
}
