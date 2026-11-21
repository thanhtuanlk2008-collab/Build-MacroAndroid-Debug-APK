package com.soulknight.matrixai.navigation

data class GridPoint(val x: Int, val y: Int)

enum class TileType { UNKNOWN, WALKABLE, BLOCKED, HAZARD }

class GridMap(val width: Int, val height: Int, private val cells: Array<TileType> = Array(width * height) { TileType.UNKNOWN }) {
    init {
        require(width > 0 && height > 0)
        require(cells.size == width * height)
    }

    operator fun get(point: GridPoint): TileType? =
        if (contains(point)) cells[point.y * width + point.x] else null

    operator fun set(point: GridPoint, type: TileType) {
        require(contains(point))
        cells[point.y * width + point.x] = type
    }

    fun contains(point: GridPoint) = point.x in 0 until width && point.y in 0 until height

    fun neighbors(point: GridPoint): List<GridPoint> =
        listOf(GridPoint(point.x + 1, point.y), GridPoint(point.x - 1, point.y),
            GridPoint(point.x, point.y + 1), GridPoint(point.x, point.y - 1))
            .filter { contains(it) && get(it) == TileType.WALKABLE }
}
