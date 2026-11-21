package com.soulknight.matrixai.navigation

import java.util.PriorityQueue
import kotlin.math.abs

class AStarPathfinder {
    private data class Node(val point: GridPoint, val score: Int)

    fun findPath(map: GridMap, start: GridPoint, goal: GridPoint): List<GridPoint> {
        if (!map.contains(start) || !map.contains(goal)) return emptyList()
        if (start == goal) return listOf(start)
        if (map[start] != TileType.WALKABLE || map[goal] != TileType.WALKABLE) return emptyList()

        val open = PriorityQueue(compareBy<Node> { it.score })
        val cameFrom = mutableMapOf<GridPoint, GridPoint>()
        val gScore = mutableMapOf(start to 0)
        open += Node(start, heuristic(start, goal))
        val closed = mutableSetOf<GridPoint>()

        while (open.isNotEmpty()) {
            val current = open.remove().point
            if (!closed.add(current)) continue
            if (current == goal) return reconstruct(cameFrom, current)
            val currentCost = gScore[current] ?: continue

            for (next in map.neighbors(current)) {
                val tentative = currentCost + 1
                if (tentative < (gScore[next] ?: Int.MAX_VALUE)) {
                    cameFrom[next] = current
                    gScore[next] = tentative
                    open += Node(next, tentative + heuristic(next, goal))
                }
            }
        }
        return emptyList()
    }

    private fun heuristic(a: GridPoint, b: GridPoint) = abs(a.x - b.x) + abs(a.y - b.y)

    private fun reconstruct(cameFrom: Map<GridPoint, GridPoint>, end: GridPoint): List<GridPoint> {
        val path = mutableListOf(end)
        var current = end
        while (cameFrom[current] != null) {
            current = cameFrom.getValue(current)
            path += current
        }
        return path.asReversed()
    }
}
