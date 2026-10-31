package app.compasso.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.compasso.audio.GameSound
import app.compasso.games.BlockPuzzle
import app.compasso.games.GameKind
import app.compasso.games.Piece
import app.compasso.ui.theme.Ios
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class Drag(val slot: Int, val piece: Piece, val pointer: Offset)

private data class FlashCell(val row: Int, val col: Int, val color: Int)

@Composable
private fun pieceColors(): List<Color> {
    val c = Ios.colors
    return listOf(c.orange, c.teal, c.indigo, c.pink, c.green, c.purple, c.blue)
}

/** Encaixe: arraste as peças para o tabuleiro 8×8 e complete linhas e colunas. */
@Composable
fun BlocksScreen(
    best: Int,
    onScore: (Int) -> Unit,
    onSound: (GameSound) -> Unit,
    onPlayed: () -> Unit,
    onClose: () -> Unit,
) {
    val c = Ios.colors
    val colors = pieceColors()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var game by remember { mutableStateOf(BlockPuzzle()) }
    var version by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Drag?>(null) }
    var flash by remember { mutableStateOf<List<FlashCell>>(emptyList()) }
    val flashAnim = remember { Animatable(0f) }
    var gain by remember { mutableStateOf<String?>(null) }
    val gainAnim = remember { Animatable(0f) }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var boardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val slotCoords = remember { arrayOfNulls<LayoutCoordinates>(3) }
    val liftPx = with(density) { 56.dp.toPx() }
    val saveScore by rememberUpdatedState(onScore)

    DisposableEffect(Unit) { onDispose { saveScore(game.score) } }

    /** Tamanho de uma célula do tabuleiro, em pixels (medido na tela). */
    fun cellPx(): Float = (boardCoords?.size?.width ?: 0) / game.size.toFloat()

    /** Canto da peça arrastada (em coordenadas da tela) e a célula onde ela cairia. */
    fun target(d: Drag): Pair<Int, Int>? {
        val b = boardCoords ?: return null
        val cellPx = cellPx()
        if (cellPx <= 0f) return null
        val w = d.piece.cols * cellPx
        val h = d.piece.rows * cellPx
        val topLeft = d.pointer - Offset(w / 2f, h + liftPx)
        val origin = b.positionInRoot()
        val col = ((topLeft.x - origin.x) / cellPx).roundToInt()
        val row = ((topLeft.y - origin.y) / cellPx).roundToInt()
        return if (game.canPlace(d.piece, row, col)) row to col else null
    }

    fun drop() {
        val d = drag ?: return
        drag = null
        val t = target(d) ?: return
        val after = Array(game.size) { r -> game.board[r].clone() }
        d.piece.cells.forEach { (r, cc) -> after[t.first + r][t.second + cc] = d.piece.color }
        val res = game.place(d.slot, t.first, t.second) ?: return
        if (res.lines > 0) {
            val cells = LinkedHashSet<Pair<Int, Int>>()
            res.clearedRows.forEach { r -> for (cc in 0 until game.size) cells += r to cc }
            res.clearedCols.forEach { cc -> for (r in 0 until game.size) cells += r to cc }
            flash = cells.map { (r, cc) -> FlashCell(r, cc, after[r][cc]) }
            scope.launch {
                flashAnim.snapTo(1f)
                flashAnim.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
                flash = emptyList()
            }
            onSound(GameSound.CLEAR)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            onSound(GameSound.PLACE)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        gain = "+${res.points}"
        scope.launch {
            gainAnim.snapTo(1f)
            gainAnim.animateTo(0f, tween(900))
        }
        version++
        if (game.isGameOver()) {
            over = true
            onScore(game.score)
        }
    }

    fun restart() {
        game = BlockPuzzle()
        version++
        over = false
        flash = emptyList()
    }

    GameScaffold(GameKind.BLOCKS, game.score, best, onClose, onPlayed) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { rootCoords = it },
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, 380.dp)
                    val ghost = drag?.let { d -> target(d)?.let { t -> d.piece to t } }
                    Box {
                        Board(
                            game = game,
                            version = version,
                            colors = colors,
                            ghost = ghost,
                            flash = flash,
                            flashAlpha = flashAnim.value,
                            modifier = Modifier
                                .size(side)
                                .onGloballyPositioned { boardCoords = it }
                                .semantics { contentDescription = "Tabuleiro do Encaixe" },
                        )
                        val g = gain
                        if (g != null && gainAnim.value > 0f) {
                            Text(
                                g,
                                style = Ios.type.title2,
                                color = c.label,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        alpha = gainAnim.value
                                        translationY = -40f * (1f - gainAnim.value)
                                    },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().height(132.dp).padding(horizontal = 16.dp)) {
                    for (i in 0 until 3) {
                        val piece = game.tray[i]
                        val dragging = drag?.slot == i
                        val fits = piece != null && game.fitsSomewhere(piece)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { slotCoords[i] = it }
                                .pointerInput(i, version, game) {
                                    detectDragGestures(
                                        onDragStart = { pos ->
                                            val p = game.tray[i] ?: return@detectDragGestures
                                            val coords = slotCoords[i] ?: return@detectDragGestures
                                            if (over) return@detectDragGestures
                                            drag = Drag(i, p, coords.localToRoot(pos))
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            drag = drag?.let { it.copy(pointer = it.pointer + amount) }
                                        },
                                        onDragEnd = { drop() },
                                        onDragCancel = { drag = null },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (piece != null && !dragging) {
                                val cell = cellPx().takeIf { it > 0f } ?: with(density) { 40.dp.toPx() }
                                val small = with(density) { (cell * 0.52f).toDp() }
                                PieceView(piece, colors, small, alpha = if (fits) 1f else 0.35f)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // peça sendo arrastada, um pouco acima do dedo (para dar pra ver onde cai)
            val d = drag
            val root = rootCoords
            val cellPx = cellPx()
            if (d != null && root != null && cellPx > 0f) {
                val w = d.piece.cols * cellPx
                val h = d.piece.rows * cellPx
                val topLeft = d.pointer - Offset(w / 2f, h + liftPx) - root.positionInRoot()
                Box(Modifier.offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }) {
                    PieceView(d.piece, colors, with(density) { cellPx.toDp() }, alpha = 0.92f)
                }
            }

            GameOverCard(
                visible = over,
                title = "Sem espaço",
                score = game.score,
                newRecord = game.score > best,
                onAgain = ::restart,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun Board(
    game: BlockPuzzle,
    version: Int,
    colors: List<Color>,
    ghost: Pair<Piece, Pair<Int, Int>>?,
    flash: List<FlashCell>,
    flashAlpha: Float,
    modifier: Modifier,
) {
    val c = Ios.colors
    val empty = c.fill
    val boardBg = c.card
    Canvas(modifier) {
        @Suppress("UNUSED_EXPRESSION") version
        val n = game.size
        val cell = size.width / n
        val gap = cell * 0.08f
        drawRoundRect(boardBg, cornerRadius = CornerRadius(cell * 0.3f))
        // linhas/colunas que a peça fantasma completaria
        val highlight = HashSet<Pair<Int, Int>>()
        if (ghost != null) {
            val (piece, at) = ghost
            val sim = Array(n) { r -> game.board[r].clone() }
            piece.cells.forEach { (r, cc) -> sim[at.first + r][at.second + cc] = piece.color }
            for (r in 0 until n) if (sim[r].all { it != 0 }) for (cc in 0 until n) highlight += r to cc
            for (cc in 0 until n) if ((0 until n).all { sim[it][cc] != 0 }) for (r in 0 until n) highlight += r to cc
        }
        for (r in 0 until n) for (cc in 0 until n) {
            val v = game.board[r][cc]
            val base = if (v == 0) empty else colors[(v - 1) % colors.size]
            drawCell(r, cc, cell, gap, base)
            if ((r to cc) in highlight && ghost != null) {
                drawCell(r, cc, cell, gap, colors[(ghost.first.color - 1) % colors.size].copy(alpha = 0.35f))
            }
        }
        if (ghost != null) {
            val (piece, at) = ghost
            piece.cells.forEach { (r, cc) ->
                drawCell(at.first + r, at.second + cc, cell, gap, colors[(piece.color - 1) % colors.size].copy(alpha = 0.45f))
            }
        }
        if (flashAlpha > 0f) {
            flash.forEach { f ->
                val col = if (f.color == 0) Color.White else colors[(f.color - 1) % colors.size]
                val grow = (1f - flashAlpha) * cell * 0.25f
                drawRoundRect(
                    col.copy(alpha = flashAlpha),
                    topLeft = Offset(f.col * cell + gap / 2 - grow / 2, f.row * cell + gap / 2 - grow / 2),
                    size = Size(cell - gap + grow, cell - gap + grow),
                    cornerRadius = CornerRadius(cell * 0.22f),
                )
            }
        }
    }
}

private fun DrawScope.drawCell(r: Int, c: Int, cell: Float, gap: Float, color: Color) {
    drawRoundRect(
        color,
        topLeft = Offset(c * cell + gap / 2, r * cell + gap / 2),
        size = Size(cell - gap, cell - gap),
        cornerRadius = CornerRadius(cell * 0.22f),
    )
}

@Composable
private fun PieceView(piece: Piece, colors: List<Color>, cell: androidx.compose.ui.unit.Dp, alpha: Float) {
    val color = colors[(piece.color - 1) % colors.size]
    Canvas(Modifier.size(cell * piece.cols, cell * piece.rows).graphicsLayer { this.alpha = alpha }) {
        val px = size.width / piece.cols
        val gap = px * 0.08f
        piece.cells.forEach { (r, cc) -> drawCell(r, cc, px, gap, color) }
    }
}
