package com.yuranime.dinz.rpg.controls

import java.util.PriorityQueue
import kotlin.math.abs

/**
 * Clean, deterministic grid A* pathfinder for Tap-to-Move and living NPC daily schedules.
 * Guarantees obstacle avoidance with zero diagonal corner-clipping.
 */
object AStarPathfinder {

    private data class Node(
        val x: Int,
        val y: Int,
        val g: Int,
        val f: Int
    ) : Comparable<Node> {
        override fun compareTo(other: Node): Int = f.compareTo(other.f)
    }

    private val CARDINALS = arrayOf(
        1 to 0,
        -1 to 0,
        0 to 1,
        0 to -1
    )

    /**
     * Finds a walkable orthogonal path from (startX, startY) to (goalX, goalY).
     * Returns the list of waypoint tiles excluding the starting tile, or empty if unreachable.
     */
    fun findPath(
        startX: Int,
        startY: Int,
        goalX: Int,
        goalY: Int,
        mapWidth: Int,
        mapHeight: Int,
        maxVisited: Int = 1400,
        isWalkable: (Int, Int) -> Boolean
    ): List<Pair<Int, Int>> {
        if (startX == goalX && startY == goalY) return emptyList()
        if (goalX !in 0 until mapWidth || goalY !in 0 until mapHeight) return emptyList()
        if (!isWalkable(goalX, goalY)) return emptyList()

        val open = PriorityQueue<Node>()
        val cameFrom = HashMap<Long, Long>(256)
        val gScore = HashMap<Long, Int>(256)
        val closed = HashSet<Long>(256)

        val startKey = pack(startX, startY)
        val goalKey = pack(goalX, goalY)
        gScore[startKey] = 0
        open.add(Node(startX, startY, 0, heuristic(startX, startY, goalX, goalY)))

        var visitedCount = 0
        while (open.isNotEmpty() && visitedCount < maxVisited) {
            val cur = open.poll() ?: break
            val curKey = pack(cur.x, cur.y)
            if (curKey == goalKey) {
                return reconstructPath(cameFrom, startKey, goalKey)
            }
            if (!closed.add(curKey)) continue
            visitedCount++

            for ((dx, dy) in CARDINALS) {
                val nx = cur.x + dx
                val ny = cur.y + dy
                if (nx !in 0 until mapWidth || ny !in 0 until mapHeight) continue
                val nKey = pack(nx, ny)
                if (closed.contains(nKey)) continue
                if (nKey != goalKey && !isWalkable(nx, ny)) continue

                val tentativeG = cur.g + 1
                val prevG = gScore[nKey]
                if (prevG == null || tentativeG < prevG) {
                    cameFrom[nKey] = curKey
                    gScore[nKey] = tentativeG
                    open.add(Node(nx, ny, tentativeG, tentativeG + heuristic(nx, ny, goalX, goalY)))
                }
            }
        }
        return emptyList()
    }

    /**
     * Finds the shortest path from (startX, startY) to the best walkable tile adjacent to
     * a solid interactive target (tree, rock, water shore, NPC, chest, workbench, bed, etc.).
     */
    fun findPathToAdjacent(
        startX: Int,
        startY: Int,
        targetX: Int,
        targetY: Int,
        mapWidth: Int,
        mapHeight: Int,
        isWalkable: (Int, Int) -> Boolean
    ): List<Pair<Int, Int>>? {
        if (abs(startX - targetX) + abs(startY - targetY) == 1) {
            return emptyList() // Already standing right next to target!
        }
        val candidates = CARDINALS
            .map { (dx, dy) -> (targetX + dx) to (targetY + dy) }
            .filter { (cx, cy) -> cx in 0 until mapWidth && cy in 0 until mapHeight && isWalkable(cx, cy) }
            .sortedBy { (cx, cy) -> heuristic(startX, startY, cx, cy) }

        var bestPath: List<Pair<Int, Int>>? = null
        for ((cx, cy) in candidates) {
            val path = findPath(startX, startY, cx, cy, mapWidth, mapHeight, isWalkable = isWalkable)
            if (path.isNotEmpty() && (bestPath == null || path.size < bestPath.size)) {
                bestPath = path
            }
        }
        return bestPath
    }

    private fun heuristic(x1: Int, y1: Int, x2: Int, y2: Int): Int = abs(x1 - x2) + abs(y1 - y2)

    private fun pack(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)

    private fun unpackX(key: Long): Int = (key shr 32).toInt()

    private fun unpackY(key: Long): Int = key.toInt()

    private fun reconstructPath(
        cameFrom: Map<Long, Long>,
        startKey: Long,
        goalKey: Long
    ): List<Pair<Int, Int>> {
        val result = ArrayList<Pair<Int, Int>>()
        var cur = goalKey
        while (cur != startKey) {
            result.add(unpackX(cur) to unpackY(cur))
            cur = cameFrom[cur] ?: break
        }
        result.reverse()
        return result
    }
}
