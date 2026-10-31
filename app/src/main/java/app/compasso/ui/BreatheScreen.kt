package app.compasso.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import app.compasso.R
import app.compasso.ui.components.Astro
import app.compasso.ui.components.GlassButton
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.SkyBackground
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.compasso.model.Breath
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.GlassCircleButton
import app.compasso.ui.components.IosButton
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.PhaseColors

private const val BLOCK_MS = 2 * 60_000L

/**
 * Respiração avulsa para quando a vontade bate (inspira 4 s, solta 6 s), com trilha calma que
 * acompanha o ar. Cada bloco de 2 minutos completo rende pontos.
 */
@Composable
fun BreatheScreen(
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    onStart: () -> Unit,
    onLevel: (Float) -> Unit,
    onBlockDone: () -> Unit,
    onStop: () -> Unit,
    onCue: (inhale: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var duration by remember { mutableLongStateOf(BLOCK_MS) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val start by rememberUpdatedState(onStart)
    val stop by rememberUpdatedState(onStop)
    DisposableEffect(Unit) {
        start()
        onDispose { stop() }
    }
    LaunchedEffect(duration) {
        var last = withFrameMillis { it }
        var step = Breath.step(elapsed)
        while (elapsed < duration) {
            val now = withFrameMillis { it }
            elapsed = (elapsed + (now - last).coerceIn(0L, 250L)).coerceAtMost(duration)
            last = now
            onLevel(Breath.level(elapsed))
            val s = Breath.step(elapsed)
            if (s != step) {
                step = s
                if (elapsed < duration) onCue(Breath.isInhale(elapsed))
            }
        }
        onLevel(0f)
        onBlockDone()
    }
    KeepScreenOn()
    BackHandler(onBack = onClose)
    BreatheContent(elapsed, duration, onClose = onClose, onMore = { duration += BLOCK_MS }, sound = sound, onSound = onSound)
}

@Composable
fun BreatheContent(
    elapsedMs: Long,
    durationMs: Long,
    onClose: () -> Unit,
    onMore: () -> Unit,
    sound: Boolean = true,
    onSound: (Boolean) -> Unit = {},
) {
    val c = Ios.colors
    val done = elapsedMs >= durationMs
    val level by animateFloatAsState(
        targetValue = if (done) 0f else Breath.level(elapsedMs),
        animationSpec = if (done) tween(900) else snap(),
        label = "nivel",
    )
    val tint = c.lavender
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var orb by remember { mutableStateOf<LayoutCoordinates?>(null) }
    SkyBackground(Modifier.fillMaxSize().onGloballyPositioned { root = it }, glow = tint, glowStrength = 1.5f) {
        // ondas: saem do astro e, no fim da inspiração, chegam à borda da tela
        Canvas(Modifier.fillMaxSize()) {
            val r = root
            val o = orb
            val center = if (r != null && o != null && r.isAttached && o.isAttached) {
                r.localPositionOf(o, Offset(o.size.width / 2f, o.size.height / 2f))
            } else {
                Offset(size.width / 2f, size.height * 0.40f)
            }
            drawBreathWaves(center, 96.dp.toPx(), level)
        }
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.breathe_title), style = Ios.type.headline, color = c.label)
                    Text(stringResource(R.string.breathe_pattern), style = Ios.type.subhead, color = c.secondary)
                }
                GlassCircleButton(
                    if (sound) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    stringResource(if (sound) R.string.breathe_sound_off else R.string.breathe_sound_on),
                    { onSound(!sound) },
                    size = 40.dp,
                    iconSize = 20.dp,
                )
                Spacer(Modifier.width(10.dp))
                GlassCircleButton(Icons.Rounded.Close, stringResource(R.string.action_close), onClose, size = 40.dp, iconSize = 20.dp)
            }
            Spacer(Modifier.weight(1f))
            // o astro respira junto: cresce na inspiração e o anel mostra o tempo do bloco
            Box(
                Modifier
                    .size(260.dp)
                    .onGloballyPositioned { orb = it }
                    .graphicsLayer {
                        val s = 0.84f + 0.16f * level
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center,
            ) {
                Astro(
                    progress = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f),
                    tint = tint,
                    size = 260.dp,
                    level = level,
                )
            }
            Spacer(Modifier.height(18.dp))
            AnimatedContent(
                targetState = stringResource(
                    when {
                        done -> R.string.breathe_done
                        Breath.isInhale(elapsedMs) -> R.string.breath_in
                        else -> R.string.breath_out
                    },
                ),
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                label = "dica",
            ) { cue ->
                Text(cue, style = Ios.type.display.copy(fontSize = 42.sp, lineHeight = 48.sp), color = c.label)
            }
            Text(
                stringResource(if (done) R.string.breathe_done_text else R.string.breathe_text),
                style = Ios.type.callout,
                color = c.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            if (!done) {
                // linha do tempo, como num player
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatClock(elapsedMs), style = Ios.type.caption1.copy(fontFeatureSettings = "tnum"), color = c.secondary)
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(c.label.copy(alpha = 0.14f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.7f), tint))),
                        )
                    }
                    Text("−" + formatClock(durationMs - elapsedMs + 999), style = Ios.type.caption1.copy(fontFeatureSettings = "tnum"), color = c.secondary)
                }
                Spacer(Modifier.height(18.dp))
                GlassButton(stringResource(R.string.action_end), onClose, Modifier.fillMaxWidth(), height = 56.dp)
            } else {
                PrimaryButton(stringResource(R.string.breathe_more), onMore, Modifier.fillMaxWidth(), color = tint)
                Spacer(Modifier.height(10.dp))
                GlassButton(stringResource(R.string.nav_back), onClose, Modifier.fillMaxWidth(), height = 56.dp)
            }
        }
    }
}
