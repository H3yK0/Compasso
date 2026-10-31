package app.compasso.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.compasso.R
import app.compasso.model.Codename
import app.compasso.model.Codenames
import app.compasso.model.GateRule
import app.compasso.model.Level
import app.compasso.model.Levels
import app.compasso.model.Outcome
import app.compasso.model.ReleaseGate
import app.compasso.model.ReleaseMode
import app.compasso.model.Speed
import app.compasso.model.speedOf
import app.compasso.ui.theme.Ios
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

// ---------- Resultado das sessões ----------

data class OutcomeLook(val icon: ImageVector, val color: Color, val title: String, val message: String, val short: String)

@Composable
fun outcomeLook(outcome: Outcome, controlOnly: Boolean = false): OutcomeLook {
    val c = Ios.colors
    return when (outcome) {
        Outcome.RELEASED -> OutcomeLook(
            Icons.Rounded.Check, c.green, stringResource(R.string.outcome_released_title),
            stringResource(R.string.outcome_released_text), stringResource(R.string.outcome_released_short),
        )
        Outcome.DENIED -> OutcomeLook(
            Icons.Rounded.EmojiEvents, c.indigo,
            stringResource(if (controlOnly) R.string.outcome_denied_control_title else R.string.outcome_denied_title),
            stringResource(R.string.outcome_denied_text), stringResource(R.string.outcome_denied_short),
        )
        Outcome.LOST_CONTROL -> OutcomeLook(
            Icons.Rounded.Replay, c.orange, stringResource(R.string.outcome_lost_title),
            stringResource(R.string.outcome_lost_text), stringResource(R.string.outcome_lost_short),
        )
        Outcome.STOPPED -> OutcomeLook(
            Icons.Rounded.Pause, c.gray, stringResource(R.string.outcome_stopped_title),
            stringResource(R.string.outcome_stopped_text), stringResource(R.string.outcome_stopped_short),
        )
        Outcome.INTERRUPTED -> OutcomeLook(
            Icons.Rounded.DoNotDisturbOn, c.red, stringResource(R.string.outcome_interrupted_title),
            stringResource(R.string.outcome_interrupted_text), stringResource(R.string.outcome_interrupted_short),
        )
    }
}

// ---------- Nomes traduzidos ----------

/** Nome do nível no idioma do app (os gravados no histórico usam o id; outros mostram o nome salvo). */
@Composable
fun levelName(id: String, fallback: String): String = when (id) {
    Levels.EASY.id -> stringResource(R.string.level_easy)
    Levels.MEDIUM.id -> stringResource(R.string.level_medium)
    Levels.HARD.id -> stringResource(R.string.level_hard)
    Levels.IMPOSSIBLE.id -> stringResource(R.string.level_impossible)
    Level.CUSTOM_ID -> stringResource(R.string.level_custom)
    else -> fallback
}

@Composable
fun levelName(level: Level): String = levelName(level.id, level.name)

@Composable
fun speedName(bpm: Int): String = stringResource(
    when (speedOf(bpm)) {
        Speed.SLOW -> R.string.speed_slow
        Speed.MODERATE -> R.string.speed_moderate
        Speed.FAST -> R.string.speed_fast
        Speed.VERY_FAST -> R.string.speed_very_fast
    },
)

@Composable
fun codenameText(c: Codename): String = Codenames.full(c, currentLanguage())

@Composable
fun codenameShort(c: Codename): String = Codenames.short(c, currentLanguage())

@Composable
fun roundsText(n: Int): String = pluralStringResource(R.plurals.rounds_count, n, n)

@Composable
fun daysText(n: Int): String = pluralStringResource(R.plurals.days_count, n, n)

// ---------- Controle inteligente ----------

data class GateLook(val icon: ImageVector, val tint: Color, val title: String, val subtitle: String)

@Composable
fun gateLook(gate: ReleaseGate, now: Long): GateLook {
    val c = Ios.colors
    return when (gate) {
        is ReleaseGate.Open -> GateLook(
            Icons.Rounded.CheckCircle, c.green, stringResource(R.string.gate_open_title),
            stringResource(R.string.gate_open_text, gate.intervalHours),
        )
        is ReleaseGate.Closed -> GateLook(
            Icons.Rounded.HourglassTop, c.orange, stringResource(R.string.gate_closed_title),
            stringResource(R.string.gate_closed_text, formatUntil(gate.until, now)),
        )
        ReleaseGate.Off -> GateLook(
            Icons.Rounded.Shield, c.gray, stringResource(R.string.gate_off_title),
            stringResource(R.string.gate_off_text),
        )
    }
}

/** Frase com o motivo de a liberação estar fechada. */
@Composable
fun gateReason(gate: ReleaseGate.Closed): String {
    val interval = GateRule.INTERVAL in gate.rules
    val rush = GateRule.RUSH in gate.rules
    val intervalText = if (gate.busyWeek) {
        stringResource(R.string.gate_reason_busy, gate.intervalHours)
    } else {
        stringResource(R.string.gate_reason_interval, gate.intervalHours)
    }
    val rushText = stringResource(R.string.gate_reason_rush)
    val text = when {
        interval && rush -> stringResource(R.string.gate_reason_both, intervalText, rushText)
        interval -> intervalText
        else -> rushText
    }
    return text.replaceFirstChar { it.uppercaseChar() } + "."
}

@Composable
fun levelEnding(level: Level, gate: ReleaseGate): String {
    val closed = gate is ReleaseGate.Closed
    return when (level.release) {
        ReleaseMode.NEVER -> stringResource(R.string.level_end_never)
        ReleaseMode.ALWAYS -> stringResource(if (closed) R.string.level_end_control_now else R.string.level_end_release)
        ReleaseMode.CHANCE -> if (closed) stringResource(R.string.level_end_control_now) else stringResource(R.string.level_end_chance, level.releaseChance)
    }
}

@Composable
fun levelSummary(level: Level, gate: ReleaseGate, withTime: Boolean = true): String {
    val time = if (withTime) stringResource(R.string.level_summary_time, level.estimatedMinutes) else ""
    return time + stringResource(R.string.level_summary, roundsText(level.rounds), level.bpmStart, level.bpmEnd, levelEnding(level, gate))
}

// ---------- Formatação ----------

fun formatClock(ms: Long): String {
    val total = (ms / 1000).toInt().coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
}

fun ceilSeconds(ms: Long): Int = ((ms + 999) / 1000).toInt()

/** Duração curta para listas: "45 s", "8 min", "1 h 05". */
@Composable
fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0).toInt()
    return when {
        s < 60 -> stringResource(R.string.duration_seconds, s)
        s < 3600 -> stringResource(R.string.duration_minutes, s / 60)
        else -> stringResource(R.string.duration_hours, s / 3600, (s % 3600) / 60)
    }
}

private fun zoned(epochMs: Long) = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())

fun localDate(epochMs: Long): LocalDate = zoned(epochMs).toLocalDate()

/** Hora no formato do idioma ("21:40" em português e espanhol, "9:40 PM" em inglês). */
@Composable
fun timeOfDay(epochMs: Long): String =
    zoned(epochMs).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()))

@Composable
private fun shortDate(epochMs: Long): String =
    zoned(epochMs).format(DateTimeFormatter.ofPattern(stringResource(R.string.fmt_short_date), currentLocale()))

@Composable
private fun weekdayName(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, currentLocale())

/** Momento futuro: "hoje às 21:40", "amanhã às 08:10", "quarta-feira às 08:10". */
@Composable
fun formatUntil(at: Long, now: Long = System.currentTimeMillis()): String {
    val today = localDate(now)
    val day = localDate(at)
    val time = timeOfDay(at)
    return when {
        day == today -> stringResource(R.string.when_today_at, time)
        day == today.plusDays(1) -> stringResource(R.string.when_tomorrow_at, time)
        day.isBefore(today.plusDays(7)) -> stringResource(R.string.when_day_at, weekdayName(day), time)
        else -> stringResource(R.string.when_day_at, shortDate(at), time)
    }
}

/** Momento passado: "hoje às 10:41", "ontem às 21:40", "sábado às 21:40", "03/10". */
@Composable
fun formatPast(at: Long, now: Long = System.currentTimeMillis()): String {
    val today = localDate(now)
    val day = localDate(at)
    val time = timeOfDay(at)
    return when {
        day == today -> stringResource(R.string.when_today_at, time)
        day == today.minusDays(1) -> stringResource(R.string.when_yesterday_at, time)
        day.isAfter(today.minusDays(7)) -> stringResource(R.string.when_day_at, weekdayName(day), time)
        else -> shortDate(at)
    }
}

/** Cabeçalho dos grupos do histórico: "Hoje", "Ontem", "sáb., 10 de out.". */
@Composable
fun dayHeader(date: LocalDate, now: Long = System.currentTimeMillis()): String {
    val today = localDate(now)
    return when (date) {
        today -> stringResource(R.string.day_today)
        today.minusDays(1) -> stringResource(R.string.day_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern(stringResource(R.string.fmt_day_header), currentLocale()))
    }
}

// ---------- Utilidades ----------

/** Mantém a tela acesa enquanto este trecho estiver visível. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Ondas de respiração: saem do centro (o timer) e, no fim da inspiração, chegam até a borda
 * da tela. [level] vai de 0 (pulmão vazio) a 1 (cheio).
 */
fun DrawScope.drawBreathWaves(center: Offset, baseRadius: Float, level: Float) {
    val corners = listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height))
    val farthest = corners.maxOf { (it - center).getDistance() } + 12.dp.toPx()
    val front = baseRadius + (farthest - baseRadius) * level.coerceIn(0f, 1f)
    val glow = front * 1.06f
    // brilho suave que enche a tela até a frente da onda
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0f),
            0.45f to Color.White.copy(alpha = 0.035f),
            0.88f to Color.White.copy(alpha = 0.09f + 0.07f * level),
            0.94f to Color.White.copy(alpha = 0.05f),
            1f to Color.White.copy(alpha = 0f),
            center = center,
            radius = glow,
        ),
        radius = glow,
        center = center,
    )
    // ondulações internas, como na água
    for (k in 1..3) {
        val r = baseRadius + (front - baseRadius) * (k / 4f)
        drawCircle(Color.White.copy(alpha = 0.05f + 0.05f * level), radius = r, center = center, style = Stroke(width = 1.dp.toPx()))
    }
    // frente da onda
    drawCircle(Color.White.copy(alpha = 0.16f + 0.26f * level), radius = front, center = center, style = Stroke(width = 1.6.dp.toPx()))
}
