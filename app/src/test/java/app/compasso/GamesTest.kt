package app.compasso

import app.compasso.games.BlockPuzzle
import app.compasso.games.Dir
import app.compasso.games.EchoGame
import app.compasso.games.EchoStep
import app.compasso.games.MergeGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GamesTest {
    // ---------- Encaixe ----------

    @Test
    fun blocksPlaceAndRejectOverlapsOrOutside() {
        val g = BlockPuzzle(random = Random(1))
        g.setPiece(0, "XX/XX")
        assertNotNull(g.place(0, 0, 0))
        assertEquals(4, (0..1).sumOf { r -> (0..1).count { c -> g.board[r][c] != 0 } })
        assertNull(g.tray[0])
        g.setPiece(1, "X")
        assertNull(g.place(1, 1, 1)) // ocupado
        assertNull(g.place(1, 8, 0)) // fora
        assertNotNull(g.place(1, 7, 7))
    }

    @Test
    fun blocksClearRowAndColumnTogetherWithBonus() {
        val g = BlockPuzzle(random = Random(2))
        for (c in 0 until 7) g.board[0][c] = 3 // linha 0 quase cheia
        for (r in 1 until 8) g.board[r][7] = 4 // coluna 7 quase cheia
        g.setPiece(0, "X")
        val res = g.place(0, 0, 7)!!
        assertEquals(listOf(0), res.clearedRows)
        assertEquals(listOf(7), res.clearedCols)
        // 1 célula + 10 × 2² pelas duas linhas
        assertEquals(1 + 40, res.points)
        assertTrue(g.board.all { row -> row.all { it == 0 } })
        assertEquals(2, g.lines)
    }

    @Test
    fun blocksRefillWhenTrayIsEmptyAndDetectGameOver() {
        val g = BlockPuzzle(random = Random(3))
        g.setPiece(0, "X")
        g.setPiece(1, "X")
        g.setPiece(2, "X")
        g.place(0, 0, 0)
        g.place(1, 0, 2)
        g.place(2, 0, 4)
        assertTrue(g.tray.all { it != null })

        // tabuleiro em xadrez: nenhuma peça de 2 ou mais células cabe
        val over = BlockPuzzle(random = Random(4))
        for (r in 0 until 8) for (c in 0 until 8) over.board[r][c] = if ((r + c) % 2 == 0) 1 else 0
        over.setPiece(0, "XX")
        over.setPiece(1, "X/X")
        over.setPiece(2, "XX/XX")
        assertTrue(over.isGameOver())
        over.setPiece(2, "X")
        assertFalse(over.isGameOver())
    }

    // ---------- Dobra ----------

    private fun values(g: MergeGame): List<IntArray> {
        val grid = List(g.size) { IntArray(g.size) }
        g.tiles.forEach { grid[it.row][it.col] = it.value }
        return grid
    }

    @Test
    fun mergeLeftJoinsPairsOnlyOnce() {
        val g = MergeGame(random = Random(5))
        g.setTiles(listOf(intArrayOf(2, 2, 4, 0), intArrayOf(2, 2, 2, 2), intArrayOf(4, 4, 8, 0), intArrayOf(0, 0, 0, 0)))
        assertTrue(g.move(Dir.LEFT))
        val v = values(g)
        assertEquals(listOf(4, 4), v[0].take(2))
        assertEquals(listOf(4, 4), v[1].take(2))
        assertEquals(listOf(8, 8), v[2].take(2))
        assertEquals(4 + 4 + 4 + 8, g.score)
        // nasceu exatamente uma peça nova
        assertEquals(6 + 1, g.tiles.size)
    }

    @Test
    fun mergeOtherDirections() {
        val g = MergeGame(random = Random(6))
        g.setTiles(listOf(intArrayOf(2, 0, 0, 2), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(2, 0, 0, 0)))
        assertTrue(g.move(Dir.RIGHT))
        assertEquals(4, values(g)[0][3])
        val h = MergeGame(random = Random(7))
        h.setTiles(listOf(intArrayOf(2, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(2, 0, 0, 0)))
        assertTrue(h.move(Dir.DOWN))
        assertEquals(4, values(h)[3][0])
        val u = MergeGame(random = Random(8))
        u.setTiles(listOf(intArrayOf(0, 0, 0, 0), intArrayOf(0, 8, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 8, 0, 0)))
        assertTrue(u.move(Dir.UP))
        assertEquals(16, values(u)[0][1])
    }

    @Test
    fun mergeNoMoveMeansNoNewTileAndGameOverWhenStuck() {
        val g = MergeGame(random = Random(9))
        g.setTiles(listOf(intArrayOf(2, 4, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0)))
        assertFalse(g.move(Dir.LEFT))
        assertEquals(2, g.tiles.size)
        val stuck = MergeGame(random = Random(10))
        stuck.setTiles(listOf(intArrayOf(2, 4, 2, 4), intArrayOf(4, 2, 4, 2), intArrayOf(2, 4, 2, 4), intArrayOf(4, 2, 4, 2)))
        assertFalse(stuck.canMove())
        stuck.setTiles(listOf(intArrayOf(2, 2, 2, 4), intArrayOf(4, 2, 4, 2), intArrayOf(2, 4, 2, 4), intArrayOf(4, 2, 4, 2)))
        assertTrue(stuck.canMove())
    }

    // ---------- Eco ----------

    @Test
    fun echoGrowsOneStepPerRoundAndEndsOnMistake() {
        val g = EchoGame(Random(11))
        val first = g.nextRound()
        assertEquals(1, first.size)
        assertEquals(EchoStep.ROUND_DONE, g.press(first[0]))
        val second = g.nextRound()
        assertEquals(2, second.size)
        assertEquals(first[0], second[0])
        assertEquals(EchoStep.CORRECT, g.press(second[0]))
        assertEquals(EchoStep.ROUND_DONE, g.press(second[1]))
        assertEquals(1, g.score) // pontuação = rodadas completas antes da atual
        val third = g.nextRound()
        assertEquals(EchoStep.WRONG, g.press((third[0] + 1) % 4))
        assertEquals(2, g.score)
    }

    @Test
    fun echoSpeedsUpButHasAFloor() {
        val g = EchoGame(Random(12))
        g.nextRound()
        val early = g.stepMs()
        repeat(40) { g.nextRound() }
        assertTrue(g.stepMs() < early)
        assertEquals(260L, g.stepMs())
    }
}
