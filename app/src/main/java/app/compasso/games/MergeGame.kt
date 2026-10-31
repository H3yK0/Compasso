package app.compasso.games

import kotlin.random.Random

data class Tile(val id: Long, val value: Int, val row: Int, val col: Int, val merged: Boolean = false, val fresh: Boolean = false)

enum class Dir { UP, DOWN, LEFT, RIGHT }

/**
 * Dobra: deslize para juntar peças iguais (2 + 2 = 4, 4 + 4 = 8…). Cada jogada
 * acrescenta uma peça nova. Acaba quando não há mais movimento possível.
 */
class MergeGame(val size: Int = 4, private val random: Random = Random.Default) {
    var tiles: List<Tile> = emptyList()
        private set
    var score: Int = 0
        private set
    private var nextId = 1L

    init {
        spawn()
        spawn()
    }

    val maxValue: Int get() = tiles.maxOfOrNull { it.value } ?: 0

    /** Faz a jogada. Falso se nada se mexeu (aí não nasce peça nova). */
    fun move(dir: Dir): Boolean {
        val grid = Array(size) { arrayOfNulls<Tile>(size) }
        tiles.forEach { grid[it.row][it.col] = it.copy(merged = false, fresh = false) }
        val result = ArrayList<Tile>()
        var moved = false
        var gained = 0

        for (line in 0 until size) {
            // posições da linha na ordem em que as peças "caem" na direção do movimento
            val positions = (0 until size).map { i ->
                when (dir) {
                    Dir.LEFT -> line to i
                    Dir.RIGHT -> line to (size - 1 - i)
                    Dir.UP -> i to line
                    Dir.DOWN -> (size - 1 - i) to line
                }
            }
            val items = positions.mapNotNull { (r, c) -> grid[r][c] }
            var target = 0
            var i = 0
            while (i < items.size) {
                val a = items[i]
                val b = items.getOrNull(i + 1)
                val (r, c) = positions[target]
                if (b != null && b.value == a.value) {
                    val value = a.value * 2
                    result += Tile(a.id, value, r, c, merged = true)
                    gained += value
                    moved = true
                    i += 2
                } else {
                    if (a.row != r || a.col != c) moved = true
                    result += a.copy(row = r, col = c)
                    i += 1
                }
                target++
            }
        }
        if (!moved) return false
        tiles = result
        score += gained
        spawn()
        return true
    }

    fun canMove(): Boolean {
        if (tiles.size < size * size) return true
        val grid = Array(size) { IntArray(size) }
        tiles.forEach { grid[it.row][it.col] = it.value }
        for (r in 0 until size) for (c in 0 until size) {
            if (c + 1 < size && grid[r][c] == grid[r][c + 1]) return true
            if (r + 1 < size && grid[r][c] == grid[r + 1][c]) return true
        }
        return false
    }

    /** Para testes: começa com um tabuleiro definido. */
    fun setTiles(values: List<IntArray>) {
        nextId = 1L
        tiles = values.flatMapIndexed { r, row ->
            row.toList().mapIndexedNotNull { c, v -> if (v > 0) Tile(nextId++, v, r, c) else null }
        }
    }

    private fun spawn() {
        val taken = tiles.map { it.row to it.col }.toSet()
        val free = (0 until size).flatMap { r -> (0 until size).map { c -> r to c } }.filter { it !in taken }
        if (free.isEmpty()) return
        val (r, c) = free[random.nextInt(free.size)]
        val value = if (random.nextInt(10) == 0) 4 else 2
        tiles = tiles + Tile(nextId++, value, r, c, fresh = true)
    }
}
