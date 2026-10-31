package app.compasso.games

import kotlin.random.Random

/** Peça do Encaixe: lista de células (linha, coluna) relativas ao canto de cima à esquerda. */
data class Piece(val id: Long, val cells: List<Pair<Int, Int>>, val color: Int) {
    val rows: Int = cells.maxOf { it.first } + 1
    val cols: Int = cells.maxOf { it.second } + 1
}

data class PlaceResult(val clearedRows: List<Int>, val clearedCols: List<Int>, val points: Int) {
    val lines: Int get() = clearedRows.size + clearedCols.size
}

/**
 * Encaixe: tabuleiro 8×8, três peças por vez. Linhas e colunas completas somem.
 * O jogo acaba quando nenhuma das peças que sobraram cabe no tabuleiro.
 */
class BlockPuzzle(val size: Int = 8, private val random: Random = Random.Default) {
    /** 0 = vazio; 1..7 = cor da peça que ocupou. */
    val board: Array<IntArray> = Array(size) { IntArray(size) }
    val tray: Array<Piece?> = arrayOfNulls(3)
    var score: Int = 0
        private set
    var lines: Int = 0
        private set
    private var combo = 0
    private var nextId = 1L

    init {
        refill()
    }

    fun canPlace(piece: Piece, row: Int, col: Int): Boolean = piece.cells.all { (r, c) ->
        val rr = row + r
        val cc = col + c
        rr in 0 until size && cc in 0 until size && board[rr][cc] == 0
    }

    /** Coloca a peça do espaço [slot] com o canto em (row, col). Nulo se não couber. */
    fun place(slot: Int, row: Int, col: Int): PlaceResult? {
        val piece = tray.getOrNull(slot) ?: return null
        if (!canPlace(piece, row, col)) return null
        piece.cells.forEach { (r, c) -> board[row + r][col + c] = piece.color }
        tray[slot] = null

        val fullRows = (0 until size).filter { r -> board[r].all { it != 0 } }
        val fullCols = (0 until size).filter { c -> (0 until size).all { r -> board[r][c] != 0 } }
        fullRows.forEach { r -> for (c in 0 until size) board[r][c] = 0 }
        fullCols.forEach { c -> for (r in 0 until size) board[r][c] = 0 }

        val n = fullRows.size + fullCols.size
        combo = if (n > 0) combo + 1 else 0
        val points = piece.cells.size + 10 * n * n + if (n > 0) 10 * (combo - 1) else 0
        score += points
        lines += n
        if (tray.all { it == null }) refill()
        return PlaceResult(fullRows, fullCols, points)
    }

    /** Para testes: coloca uma peça escolhida num espaço da bandeja. */
    fun setPiece(slot: Int, shape: String, color: Int = 1) {
        val cells = shape.split("/").flatMapIndexed { r, line ->
            line.mapIndexedNotNull { c, ch -> if (ch == 'X') r to c else null }
        }
        tray[slot] = Piece(nextId++, cells, color)
    }

    /** Acabou quando nenhuma peça restante cabe em lugar nenhum. */
    fun isGameOver(): Boolean = tray.filterNotNull().none { fitsSomewhere(it) }

    fun fitsSomewhere(piece: Piece): Boolean =
        (0..size - piece.rows).any { r -> (0..size - piece.cols).any { c -> canPlace(piece, r, c) } }

    private fun refill() {
        for (i in tray.indices) tray[i] = newPiece()
    }

    private fun newPiece(): Piece {
        val shape = SHAPES[random.nextInt(SHAPES.size)]
        return Piece(nextId++, shape, 1 + random.nextInt(7))
    }

    companion object {
        /** Formas em texto: "X" ocupa, "." vazio, "/" troca de linha. */
        private val SHAPE_TEXT = listOf(
            "X",
            "XX", "X/X",
            "XXX", "X/X/X",
            "XXXX", "X/X/X/X",
            "XXXXX", "X/X/X/X/X",
            "XX/XX",
            "XXX/XXX/XXX",
            "XX/X.", "XX/.X", "X./XX", ".X/XX",
            "XXX/X..", "XXX/..X", "X../XXX", "..X/XXX",
            "XXX/.X.", ".X./XXX", "X./XX/X.", ".X/XX/.X",
            "XX./.XX", ".XX/XX.", "X./XX/.X", ".X/XX/X.",
            "XXX/X../X..", "XXX/..X/..X", "X../X../XXX", "..X/..X/XXX",
        )

        val SHAPES: List<List<Pair<Int, Int>>> = SHAPE_TEXT.map { text ->
            text.split("/").flatMapIndexed { r, line ->
                line.mapIndexedNotNull { c, ch -> if (ch == 'X') r to c else null }
            }
        }
    }
}
