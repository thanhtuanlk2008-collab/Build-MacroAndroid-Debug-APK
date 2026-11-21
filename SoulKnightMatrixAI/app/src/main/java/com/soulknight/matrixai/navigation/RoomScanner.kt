package com.soulknight.matrixai.navigation

import com.soulknight.matrixai.vision.FrameSnapshot
import kotlin.math.abs

enum class RoomKind { ENEMY, BOSS, SPECIAL, UNKNOWN }

data class RoomObjectDetection(val kind: RoomKind, val gridPosition: GridPoint, val confidence: Float) {
    init { require(confidence in 0f..1f) }
}

data class RoomCandidate(val kind: RoomKind, val doorPosition: GridPoint, val confidence: Float)

data class RoomScanResult(
    val map: GridMap,
    val playerPosition: GridPoint?,
    val candidates: List<RoomCandidate>,
    val selectedCandidate: RoomCandidate?,
    val roomFingerprint: Long
)

/**
 * Creates a fresh local map for each room. Semantic detections (player, doors,
 * enemies and special-room markers) are injected by a detector; this class never
 * pretends colour alone can identify game objects.
 */
class RoomScanner {
    fun scan(
        frame: FrameSnapshot,
        gridWidth: Int,
        gridHeight: Int,
        walkableCells: Set<GridPoint>,
        playerPosition: GridPoint?,
        detections: List<RoomObjectDetection>,
        doorPositions: List<GridPoint>
    ): RoomScanResult {
        require(gridWidth > 0 && gridHeight > 0)
        val map = GridMap(gridWidth, gridHeight)
        for (y in 0 until gridHeight) for (x in 0 until gridWidth) {
            val point = GridPoint(x, y)
            map[point] = if (point in walkableCells) TileType.WALKABLE else TileType.BLOCKED
        }
        val candidates = detections.asSequence()
            .filter { it.confidence >= MIN_DETECTION_CONFIDENCE }
            .filter { it.kind != RoomKind.SPECIAL && it.kind != RoomKind.UNKNOWN }
            .flatMap { detection ->
                doorPositions.filter { map.contains(it) }.map {
                    RoomCandidate(detection.kind, it, detection.confidence)
                }
            }.distinctBy { it.doorPosition to it.kind }.toList()
        val selected = candidates.sortedWith(
            compareBy<RoomCandidate> { priority(it.kind) }
                .thenByDescending { it.confidence }
                .thenBy { distance(playerPosition, it.doorPosition) }
        ).firstOrNull()
        return RoomScanResult(map, playerPosition, candidates, selected, fingerprint(frame))
    }

    private fun priority(kind: RoomKind) = when (kind) {
        RoomKind.ENEMY -> 0
        RoomKind.BOSS -> 1
        RoomKind.SPECIAL -> 2
        RoomKind.UNKNOWN -> 3
    }

    private fun distance(a: GridPoint?, b: GridPoint): Int =
        if (a == null) Int.MAX_VALUE else abs(a.x - b.x) + abs(a.y - b.y)

    private fun fingerprint(frame: FrameSnapshot): Long {
        var hash = 1125899906842597L
        val stepX = (frame.width / 12).coerceAtLeast(1)
        val stepY = (frame.height / 12).coerceAtLeast(1)
        for (y in 0 until frame.height step stepY) for (x in 0 until frame.width step stepX) {
            hash = hash * 31 + ((frame.pixel(x, y) ?: 0) ushr 4)
        }
        return hash
    }

    companion object { const val MIN_DETECTION_CONFIDENCE = 0.70f }
}
