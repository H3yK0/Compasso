package app.compasso.ui.games

import androidx.compose.ui.res.stringResource
import app.compasso.R

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.compasso.audio.GameSound
import app.compasso.games.Dir
import app.compasso.games.GameKind
import app.compasso.games.MergeGame
import app.compasso.games.Tile
import app.compasso.ui.theme.Ios
import kotlin.math.abs

private data class TileLook(val bg: Color, val fg: Color)

@Composable
private fun tileLook(value: Int): TileLook {
    val c = Ios.colors
    val lightText = Color.White
    return when (value) {
        2 -> if (c.dark) TileLook(Color(0xFF2C3A3D), Color(0xFFE6F2F3)) else TileLook(Color(0xFFE3F1F3), Color(0xFF1C3A40))
        4 -> if (c.dark) TileLook(Color(0xFF2A4A52), Color(0xFFE6F2F3)) else TileLook(Color(0xFFC9E6EA), Color(0xFF1C3A40))
        8 -> TileLook(Color(0xFF30B0C7), lightText)
        16 -> TileLook(Color(0xFF2F80ED), lightText)
        32 -> TileLook(Color(0xFF5E5CE6), lightText)
        64 -> TileLook(Color(0xFF9B51E0), lightText)
        128 -> TileLook(Color(0xFFE0457B), lightText)
        256 -> TileLook(Color(0xFFF2994A), lightText)
        512 -> TileLook(Color(0xFFEB5757), lightText)
        1024 -> TileLook(Color(0xFFE2B33C), lightText)
        2048 -> TileLook(Color(0xFF27AE60), lightText)
        else -> TileLook(Color(0xFF1F2A44), Color(0xFFF2C94C))
    }
}

/** Dobra: deslize para juntar números iguais. */
@Composable
fun MergeScreen(
    best: Int,
    onScore: (Int) -> Unit,
    onSound: (GameSound) -> Unit,
    onPlayed: () -> Unit,
    onClose: () -> Unit,
) {
    val c = Ios.colors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var game by remember { mutableStateOf(MergeGame()) }
    var tiles by remember { mutableStateOf(game.tiles) }
    var score by remember { mutableStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var showWon by remember { mutableStateOf(false) }
    val saveScore by rememberUpdatedState(onScore)
    val boardLabel = stringResource(R.string.game_merge_board_cd)
    DisposableEffect(Unit) { onDispose { saveScore(game.score) } }

    fun move(dir: Dir) {
        if (over) return
        val before = game.score
        if (!game.move(dir)) return
        tiles = game.tiles
        score = game.score
        if (game.score > before) {
            onSound(GameSound.MERGE)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (!won && game.maxValue >= 2048) {
            won = true
            showWon = true
        }
        if (!game.canMove()) {
            over = true
            onScore(game.score)
        }
    }

    fun restart() {
        game = MergeGame()
        tiles = game.tiles
        score = 0
        over = false
        won = false
        showWon = false
    }

    GameScaffold(GameKind.MERGE, score, best, onClose, onPlayed) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(20.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, 380.dp)
                    val n = game.size
                    val gap = 8.dp
                    val cell = (side - gap * (n + 1)) / n
                    Box(
                        Modifier
                            .size(side)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.card)
                            .semantics { contentDescription = boardLabel }
                            .pointerInput(game) {
                                var total = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { total = Offset.Zero },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        total += amount
                                    },
                                    onDragEnd = {
                                        val min = 24.dp.toPx()
                                        if (abs(total.x) < min && abs(total.y) < min) return@detectDragGestures
                                        move(
                                            if (abs(total.x) > abs(total.y)) {
                                                if (total.x > 0) Dir.RIGHT else Dir.LEFT
                                            } else {
                                                if (total.y > 0) Dir.DOWN else Dir.UP
                                            },
                                        )
                                    },
                                )
                            },
                    ) {
                        val empty = c.fill
                        Canvas(Modifier.fillMaxSize()) {
                            val g = gap.toPx()
                            val cp = cell.toPx()
                            for (r in 0 until n) for (col in 0 until n) {
                                drawRoundRect(
                                    empty,
                                    topLeft = Offset(g + col * (cp + g), g + r * (cp + g)),
                                    size = Size(cp, cp),
                                    cornerRadius = CornerRadius(cp * 0.16f),
                                )
                            }
                        }
                        tiles.forEach { t ->
                            key(t.id) { TileView(t, cell, gap) }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(if (showWon) R.string.game_merge_won else R.string.game_merge_hint),
                    style = Ios.type.subhead,
                    color = if (showWon) c.green else c.secondary,
                )
            }
            GameOverCard(
                visible = over,
                title = stringResource(R.string.game_merge_over),
                score = score,
                newRecord = score > best,
                onAgain = ::restart,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun TileView(t: Tile, cell: Dp, gap: Dp) {
    val density = LocalDensity.current
    val target = with(density) {
        IntOffset(
            (gap.toPx() + t.col * (cell + gap).toPx()).toInt(),
            (gap.toPx() + t.row * (cell + gap).toPx()).toInt(),
        )
    }
    val pos by animateIntOffsetAsState(target, tween(110), label = "posicao")
    val scale = remember { Animatable(if (t.fresh) 0f else 1f) }
    LaunchedEffect(t.value, t.merged, t.fresh) {
        when {
            t.fresh -> scale.animateTo(1f, tween(durationMillis = 200, delayMillis = 90, easing = FastOutSlowInEasing))
            t.merged -> {
                scale.snapTo(1.14f)
                scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
            }
        }
    }
    val look = tileLook(t.value)
    val digits = t.value.toString().length
    Box(
        Modifier
            .offset { pos }
            .size(cell)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(cell * 0.16f))
            .background(look.bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            t.value.toString(),
            color = look.fg,
            style = Ios.type.figure.copy(
                fontWeight = FontWeight.Bold,
                fontSize = when (digits) {
                    1, 2 -> 32.sp
                    3 -> 27.sp
                    else -> 21.sp
                },
            ),
        )
    }
}
