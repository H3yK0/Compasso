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
import app.compasso.model.GateRule
import app.compasso.model.Level
import app.compasso.model.Outcome
import app.compasso.model.ReleaseGate
import app.compasso.model.ReleaseMode
import app.compasso.ui.theme.Ios
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

// ---------- Resultado das sessões ----------

data class OutcomeLook(val icon: ImageVector, val color: Color, val title: String, val message: String, val short: String)

@Composable
fun outcomeLook(outcome: Outcome, controlOnly: Boolean = false): OutcomeLook {
    val c = Ios.colors
    return when (outcome) {
        Outcome.RELEASED -> OutcomeLook(
            Icons.Rounded.Check, c.green, "Sessão concluída",
            "Você respeitou todas as pausas até a liberação.", "Liberado",
        )
        Outcome.DENIED -> OutcomeLook(
            Icons.Rounded.EmojiEvents, c.indigo,
            if (controlOnly) "Treino de controle concluído" else "Você foi até o fim",
            "Você chegou ao fim sem liberação. Isso é controle de verdade.", "Segurou",
        )
        Outcome.LOST_CONTROL -> OutcomeLook(
            Icons.Rounded.Replay, c.orange, "Não foi dessa vez",
            "Faz parte do treino. Toque em “Estou perto” mais cedo ou tente um nível abaixo.", "Não segurou",
        )
        Outcome.STOPPED -> OutcomeLook(
            Icons.Rounded.Pause, c.gray, "Sessão encerrada",
            "Tudo bem parar no meio. Volte quando quiser.", "Encerrada",
        )
        Outcome.INTERRUPTED -> OutcomeLook(
            Icons.Rounded.DoNotDisturbOn, c.red, "Sessão interrompida",
            "Você saiu do app no meio da sessão com o modo foco ligado. Ela fica registrada, sem pontos.", "Interrompida",
        )
    }
}

// ---------- Controle inteligente ----------

data class GateLook(val icon: ImageVector, val tint: Color, val title: String, val subtitle: String)

@Composable
fun gateLook(gate: ReleaseGate, now: Long): GateLook {
    val c = Ios.colors
    return when (gate) {
        is ReleaseGate.Open -> GateLook(
            Icons.Rounded.CheckCircle, c.green, "Liberação disponível",
            "Controle inteligente · intervalo de ${gate.intervalHours} h",
        )
        is ReleaseGate.Closed -> GateLook(
            Icons.Rounded.HourglassTop, c.orange, "Só treino de controle",
            "A liberação volta ${formatUntil(gate.until, now)}",
        )
        ReleaseGate.Off -> GateLook(
            Icons.Rounded.Shield, c.gray, "Controle inteligente desligado",
            "Cada sessão segue o nível escolhido",
        )
    }
}

/** Frase com o motivo de a liberação estar fechada. */
fun gateReason(gate: ReleaseGate.Closed): String {
    val interval = GateRule.INTERVAL in gate.rules
    val rush = GateRule.RUSH in gate.rules
    val intervalText = if (gate.busyWeek) {
        "semana cheia: o intervalo dobrou para ${gate.intervalHours} h"
    } else {
        "ainda não passaram ${gate.intervalHours} h desde a última liberação"
    }
    val rushText = "faz menos de 2 h desde a última sessão"
    val text = when {
        interval && rush -> "$intervalText, e $rushText"
        interval -> intervalText
        else -> rushText
    }
    return text.replaceFirstChar { it.uppercaseChar() } + "."
}

fun levelSummary(level: Level, gate: ReleaseGate, withTime: Boolean = true): String {
    val closed = gate is ReleaseGate.Closed
    val end = when (level.release) {
        ReleaseMode.NEVER -> "sem liberação"
        ReleaseMode.ALWAYS -> if (closed) "só controle agora" else "libera no fim"
        ReleaseMode.CHANCE -> if (closed) "só controle agora" else "sorteio ${level.releaseChance}%"
    }
    val rounds = if (level.rounds == 1) "1 rodada" else "${level.rounds} rodadas"
    val time = if (withTime) "≈ ${level.estimatedMinutes} min · " else ""
    return "$time$rounds · ${level.bpmStart}–${level.bpmEnd} bpm · $end"
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
fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min"
        else -> String.format(Locale.ROOT, "%d h %02d", s / 3600, (s % 3600) / 60)
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val shortDateFmt = DateTimeFormatter.ofPattern("dd/MM")
private val dayHeaderFmt = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", PT_BR)

private fun zoned(epochMs: Long) = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())

fun localDate(epochMs: Long): LocalDate = zoned(epochMs).toLocalDate()

fun timeOfDay(epochMs: Long): String = zoned(epochMs).format(timeFmt)

/** Momento futuro: "hoje às 21:40", "amanhã às 08:10", "quarta-feira às 08:10". */
fun formatUntil(at: Long, now: Long = System.currentTimeMillis()): String {
    val dt = zoned(at)
    val today = localDate(now)
    val day = dt.toLocalDate()
    val time = dt.format(timeFmt)
    return when {
        day == today -> "hoje às $time"
        day == today.plusDays(1) -> "amanhã às $time"
        day.isBefore(today.plusDays(7)) -> "${day.dayOfWeek.getDisplayName(TextStyle.FULL, PT_BR)} às $time"
        else -> "${dt.format(shortDateFmt)} às $time"
    }
}

/** Momento passado: "hoje às 10:41", "ontem às 21:40", "sábado às 21:40", "03/10". */
fun formatPast(at: Long, now: Long = System.currentTimeMillis()): String {
    val dt = zoned(at)
    val today = localDate(now)
    val day = dt.toLocalDate()
    val time = dt.format(timeFmt)
    return when {
        day == today -> "hoje às $time"
        day == today.minusDays(1) -> "ontem às $time"
        day.isAfter(today.minusDays(7)) -> "${day.dayOfWeek.getDisplayName(TextStyle.FULL, PT_BR)} às $time"
        else -> dt.format(shortDateFmt)
    }
}

/** Cabeçalho dos grupos do histórico: "Hoje", "Ontem", "sáb., 10 de out.". */
fun dayHeader(date: LocalDate, now: Long = System.currentTimeMillis()): String {
    val today = localDate(now)
    return when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> date.format(dayHeaderFmt)
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
