package app.compasso.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.compasso.R
import app.compasso.audio.Track
import app.compasso.model.Kegel
import app.compasso.model.KegelHold
import app.compasso.model.KegelLength
import app.compasso.model.KegelPhase
import app.compasso.model.KegelPlan
import app.compasso.model.KegelState
import app.compasso.model.Tip
import app.compasso.notify.Reminders
import app.compasso.ui.components.AlertAction
import app.compasso.ui.components.Astro
import app.compasso.ui.components.GlassButton
import app.compasso.ui.components.GlassCircleButton
import app.compasso.ui.components.IosAlert
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.SkyBackground
import app.compasso.ui.components.SwitchRow
import app.compasso.ui.components.TipCard
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Id do artigo sobre Kegel (aberto pela tela do Kegel). */
const val KEGEL_ARTICLE = "kegel"

/** O cartão "Kegel de hoje" na tela Hoje. [hint]: sessões recentes terminando cedo. */
data class KegelCardUi(val today: Int, val hold: KegelHold, val length: KegelLength, val hint: Boolean = false)

/** Cor do Kegel: um verde calmo, diferente do lilás do Respirar. */
@Composable
fun kegelTint(): Color = Ios.colors.green

/** Bolinhas do dia: feitos de [total]. */
@Composable
fun KegelDots(done: Int, total: Int = Kegel.PER_DAY, color: Color = kegelTint(), size: Dp = 10.dp, empty: Color = Color.White.copy(alpha = 0.28f)) {
    Row(horizontalArrangement = Arrangement.spacedBy(size * 0.6f)) {
        repeat(total) { i ->
            Box(Modifier.size(size).clip(CircleShape).background(if (i < done) color else empty))
        }
    }
}

/** Horário em minutos desde a meia-noite, no formato do idioma (9:00, 9:00 AM…). */
@Composable
fun formatMinutes(minutes: Int): String {
    val t = LocalTime.of((minutes / 60) % 24, minutes % 60)
    return t.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()))
}

/** Tudo o que a tela do Kegel mostra. */
data class KegelUi(
    val today: Int,
    val week: List<Int>,
    val hold: KegelHold,
    val length: KegelLength,
    val unlocked: KegelHold,
    /** Treinos que ainda faltam para liberar a próxima permanência. */
    val toUnlock: Int,
    val vibrate: Boolean,
    val nudges: Boolean,
    val reminders: Boolean,
    val times: List<Int>,
)

/** Kegel: o que já foi feito hoje, níveis de permanência e tempo, como fazer e os lembretes. */
@Composable
fun KegelScreen(
    ui: KegelUi,
    tip: Tip?,
    onTipClose: (Tip) -> Unit,
    onHold: (KegelHold) -> Unit,
    onLength: (KegelLength) -> Unit,
    onStart: () -> Unit,
    onRead: (() -> Unit)?,
    onVibrate: (Boolean) -> Unit,
    onNudges: (Boolean) -> Unit,
    onReminders: (Boolean) -> Unit,
    onTime: (index: Int, minutes: Int) -> Unit,
    onBack: () -> Unit,
    remindersAllowed: () -> Boolean = { true },
) {
    val c = Ios.colors
    val tint = kegelTint()
    val context = LocalContext.current
    var blocked by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && remindersAllowed()) onReminders(true) else blocked = true
    }
    fun turnOnReminders() {
        when {
            Reminders.needsPermission(context) && Build.VERSION.SDK_INT >= 33 -> askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            !remindersAllowed() -> blocked = true
            else -> onReminders(true)
        }
    }
    BackHandler(onBack = onBack)
    LargeTitleList(stringResource(R.string.kegel_title), onBack = onBack, backLabel = stringResource(R.string.tab_refuge), glow = tint) {
        item(key = "intro") {
            Text(
                stringResource(R.string.kegel_intro, Kegel.PER_DAY),
                style = Ios.type.callout,
                color = c.secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            if (tip != null) {
                Spacer(Modifier.height(10.dp))
                TipCard(stringResource(tip.title), stringResource(tip.message), Icons.Rounded.SelfImprovement, onClose = { onTipClose(tip) })
            }
        }
        section(header = { stringResource(R.string.kegel_today) }, key = "hoje") {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.kegel_today_count, ui.today, Kegel.PER_DAY), style = Ios.type.headline, color = c.label)
                    Spacer(Modifier.height(6.dp))
                    KegelDots(ui.today, color = tint, size = 12.dp, empty = c.label.copy(alpha = 0.14f))
                }
                WeekBars(ui.week, tint)
            }
            RowDivider()
            Text(
                pluralStringResource(R.plurals.kegel_week_days, ui.week.count { it > 0 }, ui.week.count { it > 0 }) + " · " +
                    stringResource(R.string.kegel_week),
                style = Ios.type.footnote,
                color = c.secondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        section(
            header = { stringResource(R.string.kegel_hold_header) },
            footer = {
                val next = ui.unlocked.next
                if (next == null) {
                    stringResource(R.string.kegel_hold_all)
                } else {
                    pluralStringResource(R.plurals.kegel_to_unlock, ui.toUnlock, ui.toUnlock, ui.unlocked.seconds, next.seconds)
                }
            },
            key = "permanencia",
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KegelHold.entries.forEach { h ->
                    val locked = h.ordinal > ui.unlocked.ordinal
                    LevelChip(
                        title = stringResource(R.string.kegel_seconds, h.seconds),
                        subtitle = if (locked) stringResource(R.string.kegel_locked) else stringResource(R.string.kegel_level, h.level),
                        selected = h == ui.hold,
                        locked = locked,
                        tint = tint,
                        onClick = { onHold(h) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        section(
            header = { stringResource(R.string.kegel_length_header) },
            footer = {
                val plan = KegelPlan(ui.hold, ui.length)
                stringResource(R.string.kegel_plan, plan.reps, ui.hold.seconds, plan.quickTotal)
            },
            key = "tempo",
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KegelLength.entries.forEach { l ->
                    LevelChip(
                        title = stringResource(R.string.kegel_minutes, l.minutes),
                        subtitle = null,
                        selected = l == ui.length,
                        locked = false,
                        tint = tint,
                        onClick = { onLength(l) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item(key = "comecar") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                PrimaryButton(stringResource(R.string.kegel_start), onStart, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow, color = tint)
                if (ui.today >= Kegel.PER_DAY) {
                    Text(
                        stringResource(R.string.kegel_enough, Kegel.PER_DAY),
                        style = Ios.type.footnote,
                        color = c.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                }
            }
        }
        section(header = { stringResource(R.string.kegel_how) }, footer = { stringResource(R.string.kegel_how_safety) }, key = "como") {
            listOf(
                R.string.kegel_how_find,
                R.string.kegel_how_squeeze,
                R.string.kegel_how_breathe,
                R.string.kegel_how_relax,
                R.string.kegel_how_position,
            ).forEachIndexed { i, res ->
                if (i > 0) RowDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = Ios.type.caption1.copy(fontWeight = FontWeight.SemiBold), color = tint)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(res), style = Ios.type.subhead, color = c.label, modifier = Modifier.weight(1f))
                }
            }
            if (onRead != null) {
                RowDivider()
                IosRow(stringResource(R.string.kegel_read_article), icon = RowIcon(Icons.AutoMirrored.Rounded.MenuBook, c.blue), chevron = true, onClick = onRead)
            }
        }
        section(header = { stringResource(R.string.kegel_settings) }, footer = { stringResource(R.string.kegel_reminders_footer) }, key = "ajustes") {
            SwitchRow(
                stringResource(R.string.kegel_vibrate),
                ui.vibrate,
                onVibrate,
                subtitle = stringResource(R.string.kegel_vibrate_sub),
                icon = RowIcon(Icons.Rounded.Vibration, c.indigo),
            )
            RowDivider()
            SwitchRow(
                stringResource(R.string.kegel_nudges),
                ui.nudges,
                onNudges,
                subtitle = stringResource(R.string.kegel_nudges_sub),
                icon = RowIcon(Icons.Rounded.WbSunny, c.orange),
            )
            RowDivider()
            SwitchRow(
                stringResource(R.string.kegel_reminders),
                ui.reminders,
                { on -> if (on) turnOnReminders() else onReminders(false) },
                subtitle = stringResource(R.string.kegel_reminders_sub),
                icon = RowIcon(Icons.Rounded.NotificationsActive, c.red),
            )
            if (ui.reminders) {
                ui.times.forEachIndexed { i, t ->
                    RowDivider()
                    TimeRow(stringResource(R.string.kegel_reminder_n, i + 1), t, onChange = { onTime(i, it) })
                }
            }
        }
    }
    if (blocked) {
        IosAlert(
            title = stringResource(R.string.kegel_perm_title),
            message = stringResource(R.string.kegel_perm_text),
            actions = listOf(
                AlertAction(stringResource(R.string.action_cancel), { blocked = false }),
                AlertAction(stringResource(R.string.kegel_perm_open), {
                    blocked = false
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }, bold = true),
            ),
            onDismiss = { blocked = false },
        )
    }
}

/** Sete barrinhas: treinos por dia na última semana (até 3). */
@Composable
private fun WeekBars(week: List<Int>, tint: Color) {
    val c = Ios.colors
    Row(Modifier.height(40.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        week.forEachIndexed { i, n ->
            val f = (n.coerceAtMost(Kegel.PER_DAY).toFloat() / Kegel.PER_DAY)
            Box(
                Modifier
                    .width(8.dp)
                    .fillMaxHeight(0.12f + 0.88f * f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (n > 0) tint.copy(alpha = if (i == week.lastIndex) 1f else 0.7f) else c.label.copy(alpha = 0.12f)),
            )
        }
    }
}

/** Botão de nível (permanência ou tempo). */
@Composable
private fun LevelChip(
    title: String,
    subtitle: String?,
    selected: Boolean,
    locked: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) tint.copy(alpha = 0.22f) else c.label.copy(alpha = 0.06f))
            .border(if (selected) 1.5.dp else 0.75.dp, if (selected) tint else c.glassBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !locked,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (locked) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.tertiary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(
                title,
                style = Ios.type.headline.copy(fontFeatureSettings = "tnum"),
                color = when {
                    locked -> c.tertiary
                    selected -> tint
                    else -> c.label
                },
            )
        }
        if (subtitle != null) {
            Text(subtitle, style = Ios.type.caption2, color = if (locked) c.tertiary else c.secondary, maxLines = 1)
        }
    }
}

/** Um horário de lembrete, ajustável de 15 em 15 minutos. */
@Composable
private fun TimeRow(title: String, minutes: Int, onChange: (Int) -> Unit) {
    val c = Ios.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Ios.type.body, color = c.label, modifier = Modifier.weight(1f))
        StepButton(Icons.Rounded.Remove, stringResource(R.string.kegel_time_earlier)) { onChange(minutes - 15) }
        Text(
            formatMinutes(minutes),
            style = Ios.type.body.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold),
            color = c.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(84.dp),
        )
        StepButton(Icons.Rounded.Add, stringResource(R.string.kegel_time_later)) { onChange(minutes + 15) }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val c = Ios.colors
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(c.label.copy(alpha = 0.08f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = c.accent, modifier = Modifier.size(18.dp))
    }
}

// ---------- O treino ----------

/**
 * O treino: preparo, contrações lentas (segura e solta) e rápidas no fim. O tempo só corre com a
 * tela na frente (o laço de quadros para quando o app sai), como no Respirar.
 */
@Composable
fun KegelRunScreen(
    hold: KegelHold,
    length: KegelLength,
    sound: Boolean,
    track: Track,
    result: KegelResult?,
    onBegin: () -> Unit,
    onLevel: (Float) -> Unit,
    onCue: (squeeze: Boolean) -> Unit,
    onDone: (KegelPlan) -> Unit,
    onEnd: () -> Unit,
    onSound: (Boolean) -> Unit,
    onTrack: (Track) -> Unit,
    onClose: () -> Unit,
) {
    val plan = remember(hold, length) { KegelPlan(hold, length) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val begin by rememberUpdatedState(onBegin)
    val end by rememberUpdatedState(onEnd)
    DisposableEffect(Unit) {
        begin()
        onDispose { end() }
    }
    LaunchedEffect(plan) {
        var last = withFrameMillis { it }
        var lastStep: Triple<KegelPhase, Int, Int>? = null
        while (elapsed < plan.totalMs) {
            val now = withFrameMillis { it }
            elapsed = (elapsed + (now - last).coerceIn(0L, 250L)).coerceAtMost(plan.totalMs)
            last = now
            val st = plan.at(elapsed)
            onLevel(if (st.phase.squeezing) 0.9f else 0.15f)
            // toque a cada troca (contrai/solta), para dar para treinar de olhos fechados
            val step = Triple(st.phase, st.rep, st.quick)
            if (step != lastStep) {
                if (lastStep != null && st.phase != KegelPhase.PREPARE && st.phase != KegelPhase.DONE) onCue(st.phase.squeezing)
                lastStep = step
            }
        }
        onLevel(0f)
        onDone(plan)
    }
    KeepScreenOn()
    BackHandler(onBack = onClose)
    KegelRunContent(plan.at(elapsed), plan, sound, track, result, onSound, onTrack, onClose)
}

@Composable
fun KegelRunContent(
    state: KegelState,
    plan: KegelPlan,
    sound: Boolean,
    track: Track,
    result: KegelResult?,
    onSound: (Boolean) -> Unit,
    onTrack: (Track) -> Unit,
    onClose: () -> Unit,
) {
    val c = Ios.colors
    val tint = kegelTint()
    var picking by remember { mutableStateOf(false) }
    val done = state.phase == KegelPhase.DONE
    // o astro encolhe e brilha ao contrair; se abre ao soltar
    val squeeze by animateFloatAsState(
        targetValue = if (state.phase.squeezing) 1f else 0f,
        animationSpec = tween(if (state.phase == KegelPhase.QUICK_SQUEEZE || state.phase == KegelPhase.QUICK_RELAX) 180 else 600),
        label = "contrai",
    )
    SkyBackground(Modifier.fillMaxSize(), glow = tint, glowStrength = 1.2f + 0.8f * squeeze) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.kegel_title), style = Ios.type.headline, color = c.label)
                    Text(
                        stringResource(R.string.kegel_run_sub, plan.hold.seconds, plan.length.minutes),
                        style = Ios.type.subhead,
                        color = c.secondary,
                    )
                }
                GlassCircleButton(Icons.Rounded.MusicNote, stringResource(R.string.track_change), { picking = true }, size = 40.dp, iconSize = 20.dp)
                Spacer(Modifier.width(10.dp))
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
            Box(
                Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        val s = 1f - 0.2f * squeeze
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center,
            ) {
                Astro(progress = state.progress, tint = tint, size = 260.dp, level = 0.35f + 0.65f * squeeze)
                if (!done && state.phase != KegelPhase.PREPARE) {
                    Text(
                        "${((state.remainingMs + 999) / 1000)}",
                        style = Ios.type.display.copy(fontSize = 56.sp, lineHeight = 60.sp, fontFeatureSettings = "tnum"),
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            AnimatedContent(
                targetState = stringResource(
                    when (state.phase) {
                        KegelPhase.PREPARE -> R.string.kegel_prepare
                        KegelPhase.SQUEEZE -> R.string.kegel_squeeze
                        KegelPhase.RELAX -> R.string.kegel_relax
                        KegelPhase.QUICK_SQUEEZE, KegelPhase.QUICK_RELAX -> R.string.kegel_quick
                        KegelPhase.DONE -> R.string.kegel_done
                    },
                ),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "fase",
            ) { cue ->
                Text(cue, style = Ios.type.display.copy(fontSize = 42.sp, lineHeight = 48.sp), color = c.label)
            }
            val detail = when (state.phase) {
                KegelPhase.PREPARE -> stringResource(R.string.kegel_prepare_text)
                KegelPhase.SQUEEZE -> stringResource(R.string.kegel_squeeze_text)
                KegelPhase.RELAX -> stringResource(R.string.kegel_relax_text)
                KegelPhase.QUICK_SQUEEZE, KegelPhase.QUICK_RELAX -> stringResource(R.string.kegel_quick_text)
                KegelPhase.DONE -> when {
                    result == null -> ""
                    result.today >= Kegel.PER_DAY -> stringResource(R.string.kegel_done_all, Kegel.PER_DAY)
                    else -> stringResource(R.string.kegel_done_text, result.today, Kegel.PER_DAY)
                }
            }
            Text(
                detail,
                style = Ios.type.callout,
                color = c.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            if (!done && state.phase != KegelPhase.PREPARE) {
                Text(
                    if (state.quick > 0) {
                        stringResource(R.string.kegel_quick_rep, state.quick, state.quickTotal)
                    } else {
                        stringResource(R.string.kegel_rep, state.rep, state.reps)
                    },
                    style = Ios.type.footnote.copy(fontFeatureSettings = "tnum"),
                    color = c.secondary,
                )
            }
            val unlocked = result?.unlocked
            if (done && unlocked != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.kegel_unlocked, unlocked.seconds),
                    style = Ios.type.subhead.copy(fontWeight = FontWeight.SemiBold),
                    color = tint,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (!done) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatClock((state.progress * plan.totalMs).toLong()), style = Ios.type.caption1.copy(fontFeatureSettings = "tnum"), color = c.secondary)
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
                                .fillMaxWidth(state.progress)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.7f), tint))),
                        )
                    }
                    Text(
                        "−" + formatClock(plan.totalMs - (state.progress * plan.totalMs).toLong() + 999),
                        style = Ios.type.caption1.copy(fontFeatureSettings = "tnum"),
                        color = c.secondary,
                    )
                }
                Spacer(Modifier.height(18.dp))
                GlassButton(stringResource(R.string.action_end), onClose, Modifier.fillMaxWidth(), height = 56.dp)
            } else {
                Row(Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    KegelDots(result?.today ?: 0, color = tint, size = 12.dp, empty = c.label.copy(alpha = 0.14f))
                }
                PrimaryButton(stringResource(R.string.nav_back), onClose, Modifier.fillMaxWidth(), color = tint)
            }
        }
    }
    if (picking) TrackPicker(track, onPick = onTrack, onDismiss = { picking = false })
}
