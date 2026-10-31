package app.compasso.ui

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.compasso.R
import app.compasso.model.Breath
import app.compasso.model.PhaseType
import app.compasso.model.ReleaseMode
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.GlassCircleButton
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.drawStarField
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.PhaseColors

fun phaseGradient(phase: PhaseType): List<Color> = when (phase) {
    PhaseType.COUNTDOWN -> PhaseColors.countdown
    PhaseType.GO -> PhaseColors.go
    PhaseType.STOP -> PhaseColors.stop
    PhaseType.RELEASE -> PhaseColors.release
    PhaseType.DENIED -> PhaseColors.denied
}

/**
 * Tela da sessão: fundo forte por fase (dá pra perceber de canto de olho), contagem grande,
 * círculo que pulsa na batida e, nas pausas, ondas de respiração que saem do timer até a borda.
 */
@Composable
fun SessionScreen(
    ui: SessionUi,
    breathGuide: Boolean,
    onClose: () -> Unit,
    onTogglePause: () -> Unit,
    onRequestEnd: () -> Unit,
    onFinish: () -> Unit,
    /** "Não aguentei": encerra ali mesmo e registra com honestidade. */
    onLost: () -> Unit = {},
) {
    val gradient = phaseGradient(ui.phase)
    val top by animateColorAsState(gradient[0], tween(450), label = "topo")
    val bottom by animateColorAsState(gradient[1], tween(450), label = "base")
    val breathing = breathGuide && ui.phase == PhaseType.STOP
    val breathLevel by animateFloatAsState(
        targetValue = if (breathing) Breath.level(ui.phaseElapsedMs) else 0f,
        animationSpec = if (breathing) snap() else tween(700),
        label = "respiracao",
    )
    val wavesAlpha by animateFloatAsState(if (breathing) 1f else 0f, tween(700), label = "ondas")
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dialCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val blurred = ui.paused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .onGloballyPositioned { rootCoords = it },
    ) {
        // o mesmo céu do app, na cor da fase
        Canvas(Modifier.fillMaxSize()) { drawStarField(0.8f) }
        if (wavesAlpha > 0.005f) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = wavesAlpha }) {
                val r = rootCoords
                val d = dialCoords
                val center = if (r != null && d != null && r.isAttached && d.isAttached) {
                    r.localPositionOf(d, Offset(d.size.width / 2f, d.size.height / 2f))
                } else {
                    Offset(size.width / 2f, size.height * 0.45f)
                }
                drawBreathWaves(center, baseRadius = 122.dp.toPx(), level = breathLevel)
            }
        }
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (blurred) Modifier.blur(20.dp) else Modifier)
                    .safeDrawingPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SessionTopBar(ui, onRequestEnd)
                Spacer(Modifier.weight(1f))
                PhaseDial(ui, Modifier.onGloballyPositioned { dialCoords = it })
                Spacer(Modifier.height(28.dp))
                PhaseHint(ui, breathing)
                Spacer(Modifier.weight(1f))
                RoundDots(ui)
                Spacer(Modifier.height(22.dp))
                SessionControls(ui, bottom, onClose, onTogglePause, onFinish, onLost)
            }
            if (ui.paused) PausedOverlay(blurred, ui.notice, ui.blocked, onResume = onTogglePause, onEnd = onRequestEnd, onLost = onLost)
        }
    }
}

@Composable
private fun SessionTopBar(ui: SessionUi, onRequestEnd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(levelName(ui.level), style = Ios.type.headline)
            val base = when (ui.phase) {
                PhaseType.COUNTDOWN -> pluralStringResource(R.plurals.session_rounds_ahead, ui.totalRounds, ui.totalRounds)
                PhaseType.RELEASE, PhaseType.DENIED -> stringResource(R.string.session_training_time, formatClock(ui.elapsedMs))
                else -> stringResource(R.string.session_round_of, ui.round, ui.totalRounds, formatClock(ui.elapsedMs))
            }
            Text(
                if (ui.controlOnly) stringResource(R.string.session_control_only_suffix, base) else base,
                style = Ios.type.subhead,
                color = LocalContentColor.current.copy(alpha = 0.75f),
            )
        }
        GlassCircleButton(Icons.Rounded.Close, stringResource(R.string.session_end_cd), onRequestEnd, size = 36.dp, iconSize = 18.dp)
    }
}

@Composable
private fun PhaseDial(ui: SessionUi, modifier: Modifier = Modifier) {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(ui.beatCount) {
        if (ui.phase == PhaseType.GO && ui.beatCount > 0) {
            val beatMs = if (ui.bpm > 0) 60_000 / ui.bpm else 600
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween((beatMs * 0.85f).toInt().coerceIn(120, 800), easing = FastOutSlowInEasing))
        }
    }
    val timed = ui.phase == PhaseType.COUNTDOWN || ui.phase == PhaseType.GO || ui.phase == PhaseType.STOP
    val progress = if (ui.phaseDurationMs > 0) (ui.phaseRemainingMs.toFloat() / ui.phaseDurationMs).coerceIn(0f, 1f) else 1f
    val isGo = ui.phase == PhaseType.GO

    val tint = phaseGradient(ui.phase)[0]
    Box(modifier.size(276.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val r = size.minDimension / 2f - 10.dp.toPx()
            val p = if (isGo) pulse.value else 0f
            // halo e esfera da cor da fase (o "astro" da sessão), que pulsa na batida
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.10f + 0.08f * p), Color.Transparent), center = center, radius = r * 1.05f),
                radius = r * 1.05f,
            )
            val sr = r * (0.80f + 0.04f * p)
            drawCircle(
                Brush.radialGradient(
                    0f to lerp(tint, Color.White, 0.45f),
                    0.5f to lerp(tint, Color.White, 0.12f),
                    1f to lerp(tint, Color(0xFF050817), 0.45f),
                    center = Offset(center.x - sr * 0.35f, center.y - sr * 0.4f),
                    radius = sr * 1.5f,
                ),
                radius = sr,
            )
            drawCircle(Color.White.copy(alpha = 0.22f), radius = sr, style = Stroke(1.dp.toPx()))
            if (isGo && p > 0.01f) {
                drawCircle(Color.White.copy(alpha = 0.45f * p), radius = sr + (r - sr) * (1f - p), style = Stroke(2.dp.toPx()))
            }
            // anel do tempo restante, com um ponto de luz na ponta
            drawCircle(Color.White.copy(alpha = 0.16f), radius = r, style = Stroke(stroke))
            if (timed && progress > 0.001f) {
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                val ang = Math.toRadians((-90f + 360f * progress).toDouble())
                val dot = Offset(center.x + (kotlin.math.cos(ang) * r).toFloat(), center.y + (kotlin.math.sin(ang) * r).toFloat())
                drawCircle(Color.White.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = dot)
                drawCircle(Color.White, radius = 4.dp.toPx(), center = dot)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                phaseWord(ui.phase),
                style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 4.dp),
            )
            if (timed) {
                Text("${ceilSeconds(ui.phaseRemainingMs)}", style = Ios.type.timer)
                Text(stringResource(R.string.session_seconds), style = Ios.type.footnote, color = LocalContentColor.current.copy(alpha = 0.7f))
            } else {
                Spacer(Modifier.height(12.dp))
                Icon(
                    if (ui.phase == PhaseType.RELEASE) Icons.Rounded.Check else Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(76.dp),
                )
            }
        }
    }
}

@Composable
private fun phaseWord(phase: PhaseType): String = stringResource(
    when (phase) {
        PhaseType.COUNTDOWN -> R.string.phase_countdown
        PhaseType.GO -> R.string.phase_go
        PhaseType.STOP -> R.string.phase_stop
        PhaseType.RELEASE -> R.string.phase_release
        PhaseType.DENIED -> R.string.phase_denied
    },
)

@Composable
private fun PhaseHint(ui: SessionUi, breathing: Boolean) {
    val (main, sub) = when (ui.phase) {
        PhaseType.COUNTDOWN -> if (ui.controlOnly) {
            stringResource(R.string.hint_control_title) to stringResource(R.string.hint_control_text)
        } else {
            stringResource(R.string.hint_ready_title) to stringResource(R.string.hint_ready_text)
        }
        PhaseType.GO -> stringResource(R.string.hint_go_title, speedName(ui.bpm), ui.bpm) to stringResource(R.string.hint_go_text)
        PhaseType.STOP -> {
            val next = ui.nextBpm
            val after = if (next != null) stringResource(R.string.hint_after, speedName(next), next) else stringResource(R.string.hint_after_final)
            if (breathing) {
                stringResource(if (Breath.isInhale(ui.phaseElapsedMs)) R.string.breath_in else R.string.breath_out) to
                    stringResource(R.string.hint_hands_off_after, after)
            } else {
                stringResource(R.string.hint_hands_off) to after
            }
        }
        PhaseType.RELEASE -> stringResource(R.string.hint_release_title) to stringResource(R.string.hint_release_text)
        PhaseType.DENIED -> stringResource(
            when {
                ui.controlOnly -> R.string.outcome_denied_control_title
                ui.level.release == ReleaseMode.CHANCE -> R.string.hint_denied_chance
                else -> R.string.hint_denied_level
            },
        ) to stringResource(R.string.hint_denied_text)
    }
    AnimatedContent(
        targetState = main,
        transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(450)) },
        label = "dica",
    ) { text ->
        Text(text, style = Ios.type.title2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(4.dp))
    Text(
        sub,
        style = Ios.type.callout,
        color = LocalContentColor.current.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RoundDots(ui: SessionUi) {
    val n = ui.totalRounds
    val done = when (ui.phase) {
        PhaseType.COUNTDOWN -> 0
        PhaseType.RELEASE, PhaseType.DENIED -> n
        PhaseType.STOP -> ui.round
        PhaseType.GO -> ui.round - 1
    }
    if (n > 24) {
        Text(stringResource(R.string.session_rounds_done, done, n), style = Ios.type.footnote)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..n) {
            val current = ui.phase == PhaseType.GO && i == ui.round
            val alpha = if (i <= done || current) 1f else 0.28f
            Box(
                Modifier
                    .size(if (current) 11.dp else 7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun SessionControls(
    ui: SessionUi,
    accent: Color,
    onClose: () -> Unit,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onLost: () -> Unit,
) {
    when (ui.phase) {
        PhaseType.RELEASE, PhaseType.DENIED -> IosButton(
            stringResource(R.string.action_finish), onFinish, Modifier.fillMaxWidth(),
            kind = ButtonKind.White, contentColor = accent, height = 58.dp, shape = CircleShape,
        )
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IosButton(
                stringResource(if (ui.phase == PhaseType.STOP) R.string.session_still_close else R.string.session_close),
                onClose,
                Modifier.weight(1f),
                kind = ButtonKind.White,
                contentColor = accent,
                enabled = ui.phase != PhaseType.COUNTDOWN && !ui.paused,
                height = 60.dp,
                shape = CircleShape,
            )
            GlassCircleButton(
                if (ui.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                stringResource(if (ui.paused) R.string.action_continue else R.string.session_pause),
                onTogglePause,
                size = 60.dp,
                iconSize = 26.dp,
            )
        }
        // sempre à mão, sem passar pelo menu de encerrar
        if (ui.phase != PhaseType.COUNTDOWN) {
            Spacer(Modifier.height(4.dp))
            IosButton(stringResource(R.string.session_lost), onLost, kind = ButtonKind.Plain, color = Color.White.copy(alpha = 0.85f), height = 40.dp)
        }
        }
    }
}

@Composable
private fun PausedOverlay(
    blurred: Boolean,
    notice: SessionNotice?,
    blocked: Boolean,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onLost: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF050817).copy(alpha = if (blurred) 0.5f else 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.session_paused), style = Ios.type.title1, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.session_paused_text), style = Ios.type.body, color = Color.White.copy(alpha = 0.8f))
            if (notice != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    noticeText(notice),
                    style = Ios.type.subhead,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            IosButton(
                stringResource(if (blocked) R.string.session_back_fullscreen else R.string.action_continue), onResume, Modifier.width(240.dp),
                kind = ButtonKind.White, contentColor = Color(0xFF0B1234), height = 52.dp, shape = CircleShape,
                enabled = !blocked,
            )
            Spacer(Modifier.height(6.dp))
            IosButton(stringResource(R.string.session_lost), onLost, kind = ButtonKind.Plain, color = Color.White, height = 44.dp)
            IosButton(stringResource(R.string.session_end_cd), onEnd, kind = ButtonKind.Plain, color = Color.White.copy(alpha = 0.8f), height = 44.dp)
        }
    }
}

@Composable
private fun noticeText(n: SessionNotice): String = when (n) {
    is SessionNotice.LeftApp -> stringResource(R.string.notice_left_app) + " " +
        if (n.exitsLeft == 1) stringResource(R.string.notice_last_exit) else pluralStringResource(R.plurals.notice_exits_left, n.exitsLeft, n.exitsLeft)
    SessionNotice.SplitScreen -> stringResource(R.string.notice_split)
    SessionNotice.Unpinned -> stringResource(R.string.notice_unpinned)
}
