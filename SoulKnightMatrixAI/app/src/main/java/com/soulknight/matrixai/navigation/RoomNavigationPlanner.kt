package com.soulknight.matrixai.navigation

/** Connects each newly scanned room map to A* and the enemy-first route policy. */
class RoomNavigationPlanner(private val pathfinder: AStarPathfinder = AStarPathfinder()) {
    fun pathToPreferredExit(scan: RoomScanResult): List<GridPoint> {
        val start = scan.playerPosition ?: return emptyList()
        val target = scan.selectedCandidate?.doorPosition ?: return emptyList()
        return pathfinder.findPath(scan.map, start, target)
    }
}
