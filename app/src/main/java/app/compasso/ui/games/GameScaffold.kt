package app.compasso.ui.games

import androidx.compose.ui.res.stringResource
import app.compasso.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.SkyBackground
import app.compasso.ui.gameCover
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import app.compasso.games.GameKind
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.NavIconButton
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.formatScore
import app.compasso.ui.theme.Ios
import kotlinx.coroutines.delay

/** Segundos jogados para render pontos (e para a vontade baixar). */
const val PLAY_GOAL_S = 180

/**
 * Moldura comum dos jogos: barra com Fechar, título e ajuda; placar com pontos e recorde;
 * e um anel discreto que se completa em 3 minutos de jogo.
 */
@Composable
fun GameScaffold(
    kind: GameKind,
    score: Int,
    best: Int,
    onClose: () -> Unit,
    onPlayedEnough: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ios.colors
    var help by remember { mutableIntStateOf(0) }
    var played by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val done by rememberUpdatedState(onPlayedEnough)
    LaunchedEffect(Unit) {
        while (played < PLAY_GOAL_S) {
            delay(1_000)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) played++
        }
        done()
    }
    SkyBackground(Modifier.fillMaxSize(), glow = gameCover(kind).first.first(), glowStrength = 0.8f) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(Modifier.fillMaxWidth().height(44.dp)) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) { NavTextButton(stringResource(R.string.action_close), onClose) }
                Text(stringResource(kind.title), style = Ios.type.headline, color = c.label, modifier = Modifier.align(Alignment.Center))
                Row(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayRing(played)
                    NavIconButton(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.game_how_to), { help++ })
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ScoreTile(stringResource(R.string.game_score), formatScore(score), Modifier.weight(1f))
                ScoreTile(stringResource(R.string.game_best), formatScore(maxOf(best, score)), Modifier.weight(1f), highlight = score > best && best > 0)
            }
            content()
        }
        if (help > 0) {
            HelpOverlay(kind) { help = 0 }
        }
    }
}

/** Anel que enche em 3 minutos; completo, vira um check. */
@Composable
private fun PlayRing(seconds: Int) {
    val c = Ios.colors
    val p = (seconds.toFloat() / PLAY_GOAL_S).coerceIn(0f, 1f)
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 3.dp.toPx()
            val r = size.minDimension / 2f - stroke / 2f
            drawCircle(c.fill, radius = r, style = Stroke(stroke))
            drawArc(
                c.green,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        if (p >= 1f) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.game_played_cd), tint = c.green, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ScoreTile(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    val c = Ios.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.glass)
            .border(0.75.dp, if (highlight) c.amber.copy(alpha = 0.6f) else c.glassBorder, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = Ios.type.caption1.copy(fontWeight = FontWeight.SemiBold), color = if (highlight) c.amber else c.secondary)
        Text(value, style = Ios.type.title3, color = c.label)
    }
}

@Composable
private fun HelpOverlay(kind: GameKind, onClose: () -> Unit) {
    val c = Ios.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.alert)
                .border(0.75.dp, c.glassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.game_how_to), style = Ios.type.headline, color = c.label)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(kind.howTo), style = Ios.type.subhead, color = c.label, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.game_play_goal),
                style = Ios.type.footnote,
                color = c.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(stringResource(R.string.action_got_it), onClose, Modifier.fillMaxWidth(), height = 48.dp)
        }
    }
}

/** Cartão de fim de jogo por cima do tabuleiro. */
@Composable
fun BoxScope.GameOverCard(visible: Boolean, title: String, score: Int, newRecord: Boolean, onAgain: () -> Unit, onClose: () -> Unit) {
    val c = Ios.colors
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center),
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(c.alert)
                .border(0.75.dp, c.glassBorder, RoundedCornerShape(26.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Image) { }
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = Ios.type.title1, color = c.label)
            Spacer(Modifier.height(4.dp))
            Text(
                if (newRecord) stringResource(R.string.game_new_record, formatScore(score)) else stringResource(R.string.game_final_score, formatScore(score)),
                style = Ios.type.body,
                color = if (newRecord) c.amber else c.secondary,
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(stringResource(R.string.game_again), onAgain, Modifier.fillMaxWidth(), height = 50.dp)
            Spacer(Modifier.height(4.dp))
            NavTextButton(stringResource(R.string.action_close), onClose)
        }
    }
}

/** Espaço vertical flexível usado pelos tabuleiros. */
@Composable
fun ColumnScope.Flex(weight: Float = 1f) = Spacer(Modifier.weight(weight))

@Composable
fun HSpace(w: Int) = Spacer(Modifier.width(w.dp))

/** Círculo pequeno decorativo. */
@Composable
fun Dot(color: Color, size: Int) = Box(Modifier.size(size.dp).clip(CircleShape).background(color))
