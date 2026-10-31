package app.compasso.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.compasso.R
import app.compasso.audio.Track
import app.compasso.model.LevelAdvice
import app.compasso.model.LevelReason
import app.compasso.model.Levels
import app.compasso.model.Signal
import app.compasso.ui.components.AlertAction
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.GlassCard
import app.compasso.ui.components.IosActionSheet
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.IosSwitch
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.theme.Ios

// ---------- Trilhas ----------

@Composable
fun trackName(t: Track): String = stringResource(t.label)

/** Lista das trilhas para escolher (a atual aparece marcada). */
@Composable
fun TrackPicker(current: Track, onPick: (Track) -> Unit, onDismiss: () -> Unit) {
    IosActionSheet(
        title = stringResource(R.string.track_pick_title),
        message = stringResource(R.string.track_pick_text),
        actions = Track.entries.map { t ->
            val name = stringResource(t.label) + " · " + stringResource(t.description)
            AlertAction(if (t == current) "✓ $name" else name, { onPick(t); onDismiss() }, bold = t == current)
        },
        cancel = AlertAction(stringResource(R.string.action_cancel), onDismiss),
        onDismiss = onDismiss,
    )
}

/** Cartão da aba Ler: ler com música calma de fundo, ou não. */
@Composable
fun ReadingMusicCard(on: Boolean, track: Track, onToggle: (Boolean) -> Unit, onPickTrack: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    GlassCard(modifier.fillMaxWidth().padding(horizontal = 16.dp), contentPadding = PaddingValues(16.dp), onClick = { onToggle(!on) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(c.lavender.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Headphones, contentDescription = null, tint = c.lavender, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.read_music_title), style = Ios.type.headline, color = c.label)
                Text(
                    if (on) stringResource(R.string.read_music_on, trackName(track)) else stringResource(R.string.read_music_off),
                    style = Ios.type.subhead,
                    color = c.secondary,
                )
            }
            IosSwitch(on)
        }
        if (on) {
            Spacer(Modifier.height(10.dp))
            IosButton(
                stringResource(R.string.track_change), onPickTrack, Modifier.fillMaxWidth(),
                kind = ButtonKind.Tinted, height = 40.dp, shape = CircleShape,
            )
        }
    }
}

// ---------- Recomendações ----------

/** Por que esta leitura foi sugerida. */
@Composable
fun signalText(s: Signal): String = stringResource(
    when (s) {
        Signal.RECENT_LAPSE -> R.string.why_recent_lapse
        Signal.FAST_LOSS -> R.string.why_fast_loss
        Signal.INTERRUPTED -> R.string.why_interrupted
        Signal.NIGHT -> R.string.why_night
        Signal.BUSY -> R.string.why_busy
        Signal.LOW_CONSISTENCY -> R.string.why_low_consistency
        Signal.GOOD_STREAK -> R.string.why_good_streak
        Signal.NEW -> R.string.why_new
        Signal.GENERAL -> R.string.why_general
    },
)

/** Por que este nível foi sugerido. */
@Composable
fun levelReasonText(a: LevelAdvice): String {
    val name = levelName(a.levelId, a.levelId)
    return when (a.reason) {
        LevelReason.FIRST -> stringResource(R.string.level_why_first)
        LevelReason.STEP_DOWN -> stringResource(R.string.level_why_down, name)
        LevelReason.STAY_EASY -> stringResource(R.string.level_why_stay_easy)
        LevelReason.STEP_UP -> stringResource(R.string.level_why_up, name)
        LevelReason.TOP -> stringResource(R.string.level_why_top)
        LevelReason.KEEP -> stringResource(R.string.level_why_keep, name)
    }
}

/** Cartão "Para você" da tela Hoje: o nível sugerido e uma leitura, cada um com o motivo. */
@Composable
fun ForYouCard(
    level: LevelAdvice,
    readingTitle: String?,
    readingReason: Signal?,
    onLevel: () -> Unit,
    onRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    GlassCard(modifier.fillMaxWidth().padding(horizontal = 16.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.for_you), style = Ios.type.headline, color = c.label)
        }
        Spacer(Modifier.height(10.dp))
        AdviceRow(
            icon = { Icon(Icons.Rounded.Layers, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp)) },
            title = stringResource(R.string.for_you_level, levelName(level.levelId, level.levelId)),
            text = levelReasonText(level),
            onClick = if (Levels.presets.any { it.id == level.levelId }) onLevel else null,
        )
        if (readingTitle != null && readingReason != null) {
            Spacer(Modifier.height(10.dp))
            AdviceRow(
                icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = c.blue, modifier = Modifier.size(20.dp)) },
                title = readingTitle,
                text = signalText(readingReason),
                onClick = onRead,
            )
        }
    }
}

@Composable
private fun AdviceRow(icon: @Composable () -> Unit, title: String, text: String, onClick: (() -> Unit)?) {
    val c = Ios.colors
    val shape = RoundedCornerShape(14.dp)
    val source = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.fill.copy(alpha = c.fill.alpha * 0.6f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 1.dp)) { icon() }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Ios.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = c.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text, style = Ios.type.footnote, color = c.secondary)
        }
    }
}

private val NudgeEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** Sugestão de leitura que desce do topo ao abrir o app. */
@Composable
fun ReadingNudge(visible: Boolean, title: String?, reason: Signal?, onRead: () -> Unit, onLater: () -> Unit) {
    val c = Ios.colors
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible && title != null && reason != null,
            enter = slideInVertically(tween(360, easing = NudgeEase)) { -it * 2 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(280, easing = NudgeEase)) { -it * 2 } + fadeOut(tween(200)),
        ) {
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
                    .clip(shape)
                    .background(c.alert)
                    .border(0.75.dp, c.glassBorder, shape)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(c.blue.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = c.blue, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.nudge_label), style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = c.secondary)
                }
                Spacer(Modifier.height(8.dp))
                Text(title.orEmpty(), style = Ios.type.headline, color = c.label)
                if (reason != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(signalText(reason), style = Ios.type.subhead, color = c.secondary)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IosButton(stringResource(R.string.nudge_later), onLater, Modifier.weight(1f), kind = ButtonKind.Tinted, height = 44.dp, shape = CircleShape)
                    PrimaryButton(stringResource(R.string.nudge_read), onRead, Modifier.weight(1f), height = 44.dp)
                }
            }
        }
    }
}
