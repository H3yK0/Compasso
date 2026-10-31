package app.compasso.ui.games

import androidx.compose.ui.res.stringResource
import app.compasso.R

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.compasso.audio.GameSound
import app.compasso.games.EchoGame
import app.compasso.games.EchoStep
import app.compasso.games.GameKind
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.theme.Ios
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class EchoPhase { READY, SHOWING, INPUT, OVER }

private val padSounds = listOf(GameSound.PAD1, GameSound.PAD2, GameSound.PAD3, GameSound.PAD4)

/** Eco: observe a sequência de luzes e sons e repita. */
@Composable
fun EchoScreen(
    best: Int,
    onScore: (Int) -> Unit,
    onSound: (GameSound) -> Unit,
    onPlayed: () -> Unit,
    onClose: () -> Unit,
) {
    val c = Ios.colors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var game by remember { mutableStateOf(EchoGame()) }
    var phase by remember { mutableStateOf(EchoPhase.READY) }
    var lit by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }
    val padColors = listOf(c.teal, c.orange, c.pink, c.indigo)
    val saveScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) { onDispose { saveScore(score) } }

    fun show(sequence: List<Int>) {
        job?.cancel()
        job = scope.launch {
            phase = EchoPhase.SHOWING
            delay(600)
            val step = game.stepMs()
            for (pad in sequence) {
                lit = pad
                onSound(padSounds[pad])
                delay(step * 7 / 10)
                lit = -1
                delay(step * 3 / 10)
            }
            phase = EchoPhase.INPUT
        }
    }

    fun start() {
        game = EchoGame()
        score = 0
        show(game.nextRound())
    }

    fun press(pad: Int) {
        if (phase != EchoPhase.INPUT) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when (game.press(pad)) {
            EchoStep.CORRECT -> {
                onSound(padSounds[pad])
                flash(scope, pad) { lit = it }
            }
            EchoStep.ROUND_DONE -> {
                onSound(padSounds[pad])
                flash(scope, pad) { lit = it }
                score = game.round
                show(game.nextRound())
            }
            EchoStep.WRONG -> {
                onSound(GameSound.WRONG)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                score = game.score
                phase = EchoPhase.OVER
                onScore(score)
            }
        }
    }

    GameScaffold(GameKind.ECHO, score, best, onClose, onPlayed) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(
                        when (phase) {
                            EchoPhase.READY -> R.string.game_echo_ready
                            EchoPhase.SHOWING -> R.string.game_echo_watch
                            EchoPhase.INPUT -> R.string.game_echo_your_turn
                            EchoPhase.OVER -> R.string.game_over
                        },
                    ),
                    style = Ios.type.title3,
                    color = c.label,
                )
                Text(
                    if (game.round > 0) stringResource(R.string.game_echo_round, game.round) else stringResource(R.string.game_echo_grows),
                    style = Ios.type.subhead,
                    color = c.secondary,
                )
                Spacer(Modifier.height(18.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, 340.dp)
                    Column(Modifier.width(side), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (row in 0..1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                for (col in 0..1) {
                                    val pad = row * 2 + col
                                    Pad(
                                        color = padColors[pad],
                                        lit = lit == pad,
                                        enabled = phase == EchoPhase.INPUT,
                                        label = stringResource(R.string.game_echo_pad_cd, pad + 1),
                                        modifier = Modifier.weight(1f),
                                        onPress = { press(pad) },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (phase == EchoPhase.READY) {
                    PrimaryButton(stringResource(R.string.action_start), ::start, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                } else {
                    Text(
                        stringResource(R.string.game_echo_rule),
                        style = Ios.type.footnote,
                        color = c.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
            GameOverCard(
                visible = phase == EchoPhase.OVER,
                title = stringResource(R.string.game_over),
                score = score,
                newRecord = score > best,
                onAgain = ::start,
                onClose = onClose,
            )
        }
    }
}

private fun flash(scope: kotlinx.coroutines.CoroutineScope, pad: Int, set: (Int) -> Unit) {
    scope.launch {
        set(pad)
        delay(220)
        set(-1)
    }
}

@Composable
private fun Pad(color: Color, lit: Boolean, enabled: Boolean, label: String, modifier: Modifier, onPress: () -> Unit) {
    val c = Ios.colors
    val bg by animateColorAsState(if (lit) color else color.copy(alpha = if (c.dark) 0.32f else 0.42f), tween(90), label = "cor")
    val scale by animateFloatAsState(if (lit) 1.04f else 1f, tween(90), label = "escala")
    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .border(if (lit) 3.dp else 0.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                onClick = onPress,
            ),
    ) {
        Box(Modifier.align(Alignment.Center).size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = if (lit) 0.9f else 0.35f)))
    }
}
