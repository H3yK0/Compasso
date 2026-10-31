package app.compasso.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Pontos de constância. Premiam aparecer e respeitar o treino, não "durar mais" nem fazer
 * mais sessões: só a primeira sessão do dia vale pontos e existe um teto diário.
 */
enum class PointKind(val base: Int, val label: String, val maxPerDay: Int, val isSession: Boolean = false) {
    SESSION(10, "Sessão concluída com liberação", 1, isSession = true),
    CONTROL_SESSION(15, "Sessão concluída sem liberação", 1, isSession = true),
    HONEST(3, "“Não segurei” marcado com honestidade", 1, isSession = true),
    CHECKIN(10, "Check-in: um dia longe da pornografia", 1),
    BREATHE(5, "Respirar até o fim", 3),
    GAME(3, "Jogar pelo menos 3 minutos", 2),
    ARTICLE(5, "Ler um artigo até o fim", 3),
}

data class PointEntry(val at: Long, val kind: PointKind, val points: Int)

/** Por que uma ação não rendeu pontos. */
enum class NoPoints(val message: String) {
    DAILY_CAP("Você já fez os 40 pontos de hoje. Amanhã tem mais."),
    KIND_LIMIT("Isso já rendeu o máximo de pontos hoje."),
    SESSION_LIMIT("Só a primeira sessão do dia vale pontos."),
}

/** Resultado de tentar ganhar pontos: ou a entrada nova, ou o motivo de não ter rendido. */
data class Award(val entry: PointEntry?, val reason: NoPoints?)

/** Degraus do ranking pessoal. Contam os pontos dos últimos 28 dias: precisa manter para continuar. */
enum class Tier(val label: String, val minPoints: Int) {
    START("Iniciante", 0),
    BRONZE("Bronze", 60),
    SILVER("Prata", 180),
    GOLD("Ouro", 360),
    PLATINUM("Platina", 540),
    DIAMOND("Diamante", 720),
    UNBEATEN("Invicto", 900),
}

data class LevelStatus(
    val tier: Tier,
    val windowPoints: Int,
    val next: Tier?,
    val toNext: Int,
    val progress: Float,
    /** O que ainda falta para Invicto quando os pontos já bastam. */
    val missing: List<String>,
)

data class WeekScore(val start: LocalDate, val points: Int)

data class Bar(val label: String, val value: Float)

enum class Span { SESSIONS, DAYS, WEEKS, MONTHS }

data class Records(
    val bestControlMs: Long,
    val bestStreak: Int,
    val bestWeekPoints: Int,
    val completedSessions: Int,
    val controlSessions: Int,
)

object Progress {
    const val DAILY_CAP = 40
    const val WINDOW_DAYS = 28
    const val CLEAN_DAYS = 14
    const val STREAK_FOR_UNBEATEN = 14
    private const val DAY = 24 * 3_600_000L
    private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    /** Que tipo de ponto uma sessão rende (nenhum se encerrada antes ou interrompida). */
    fun kindFor(outcome: Outcome): PointKind? = when (outcome) {
        Outcome.RELEASED -> PointKind.SESSION
        Outcome.DENIED -> PointKind.CONTROL_SESSION
        Outcome.LOST_CONTROL -> PointKind.HONEST
        Outcome.STOPPED, Outcome.INTERRUPTED -> null
    }

    /** Pontos de uma sessão: só contam as que rodaram inteiras em modo foco, com a tela fixada. */
    fun kindFor(record: SessionRecord): PointKind? = if (record.focused) kindFor(record.outcome) else null

    /**
     * Tenta dar os pontos de [kind] agora, respeitando: teto do dia, limite de vezes por dia
     * de cada ação e uma sessão pontuada por dia.
     */
    fun award(log: List<PointEntry>, kind: PointKind, now: Long, zone: ZoneId = ZoneId.systemDefault()): Award {
        val today = day(now, zone)
        val todays = log.filter { day(it.at, zone) == today }
        if (kind.isSession && todays.any { it.kind.isSession }) return Award(null, NoPoints.SESSION_LIMIT)
        if (todays.count { it.kind == kind } >= kind.maxPerDay) return Award(null, NoPoints.KIND_LIMIT)
        val pts = minOf(kind.base, DAILY_CAP - todays.sumOf { it.points })
        return if (pts > 0) Award(PointEntry(now, kind, pts), null) else Award(null, NoPoints.DAILY_CAP)
    }

    fun doneToday(log: List<PointEntry>, kind: PointKind, now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val today = day(now, zone)
        return log.any { it.kind == kind && day(it.at, zone) == today }
    }

    /** Guarda os pontos de pouco mais de um ano (o bastante para os gráficos e o pódio). */
    fun prune(log: List<PointEntry>, now: Long): List<PointEntry> = log.filter { it.at > now - KEEP_DAYS * DAY }

    const val KEEP_DAYS = 400L

    fun pointsToday(log: List<PointEntry>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val today = day(now, zone)
        return log.filter { day(it.at, zone) == today }.sumOf { it.points }
    }

    fun windowPoints(log: List<PointEntry>, now: Long): Int =
        log.filter { it.at > now - WINDOW_DAYS * DAY && it.at <= now }.sumOf { it.points }

    fun level(log: List<PointEntry>, history: List<SessionRecord>, now: Long, zone: ZoneId = ZoneId.systemDefault()): LevelStatus {
        val pts = windowPoints(log, now)
        var tier = Tier.entries.last { pts >= it.minPoints }
        val missing = mutableListOf<String>()
        if (tier == Tier.UNBEATEN) {
            val since = now - CLEAN_DAYS * DAY
            if (history.any { it.startedAt > since && (it.outcome == Outcome.LOST_CONTROL || it.outcome == Outcome.INTERRUPTED) }) {
                missing += "14 dias sem “não segurei” nem sessão interrompida"
            }
            if (streak(log, history, now, zone) < STREAK_FOR_UNBEATEN) missing += "14 dias seguidos de constância"
            if (missing.isNotEmpty()) tier = Tier.DIAMOND
        }
        val next = Tier.entries.getOrNull(tier.ordinal + 1)
        val toNext = next?.let { (it.minPoints - pts).coerceAtLeast(0) } ?: 0
        val progress = if (next == null) 1f else ((pts - tier.minPoints).toFloat() / (next.minPoints - tier.minPoints)).coerceIn(0f, 1f)
        return LevelStatus(tier, pts, next, toNext, progress, missing)
    }

    /**
     * Dias seguidos com pelo menos uma ação que vale pontos, terminando hoje (ou ontem, se hoje
     * ainda não teve nada). Dia com sessão interrompida não conta.
     */
    fun streak(log: List<PointEntry>, history: List<SessionRecord>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val good = goodDays(log, history, zone)
        var d = day(now, zone)
        if (d !in good) d = d.minusDays(1)
        var n = 0
        while (d in good) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun bestStreak(log: List<PointEntry>, history: List<SessionRecord>, zone: ZoneId = ZoneId.systemDefault()): Int {
        val good = goodDays(log, history, zone).sorted()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in good) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    private fun goodDays(log: List<PointEntry>, history: List<SessionRecord>, zone: ZoneId): Set<LocalDate> {
        val bad = history.filter { it.outcome == Outcome.INTERRUPTED }.map { day(it.startedAt, zone) }.toSet()
        return log.map { day(it.at, zone) }.toSet() - bad
    }

    /** As 3 melhores semanas (pódio pessoal). */
    fun podium(log: List<PointEntry>, zone: ZoneId = ZoneId.systemDefault()): List<WeekScore> =
        log.groupBy { weekStart(day(it.at, zone)) }
            .map { (start, list) -> WeekScore(start, list.sumOf { it.points }) }
            .sortedWith(compareByDescending<WeekScore> { it.points }.thenByDescending { it.start })
            .take(3)

    fun records(log: List<PointEntry>, history: List<SessionRecord>, zone: ZoneId = ZoneId.systemDefault()): Records = Records(
        bestControlMs = history.filter { it.outcome != Outcome.INTERRUPTED }.maxOfOrNull { it.durationMs } ?: 0L,
        bestStreak = bestStreak(log, history, zone),
        bestWeekPoints = podium(log, zone).firstOrNull()?.points ?: 0,
        completedSessions = history.count { it.outcome == Outcome.RELEASED || it.outcome == Outcome.DENIED },
        controlSessions = history.count { it.outcome == Outcome.DENIED },
    )

    // ---------- Séries para os gráficos ----------

    /** Maior tempo de controle (minutos) em cada período. Sessões interrompidas ficam de fora. */
    fun controlSeries(history: List<SessionRecord>, span: Span, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Bar> {
        val valid = history.filter { it.outcome != Outcome.INTERRUPTED }
        fun minutes(list: List<SessionRecord>) = (list.maxOfOrNull { it.durationMs } ?: 0L) / 60_000f
        return when (span) {
            Span.SESSIONS -> valid.sortedBy { it.startedAt }.takeLast(12).map { r ->
                Bar(day(r.startedAt, zone).dayOfMonth.toString(), r.durationMs / 60_000f)
            }
            else -> buckets(span, now, zone).map { (label, range) ->
                Bar(label, minutes(valid.filter { day(it.startedAt, zone) in range }))
            }
        }
    }

    /** Soma de pontos em cada período. */
    fun pointsSeries(log: List<PointEntry>, span: Span, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Bar> =
        buckets(if (span == Span.SESSIONS) Span.DAYS else span, now, zone).map { (label, range) ->
            Bar(label, log.filter { day(it.at, zone) in range }.sumOf { it.points }.toFloat())
        }

    private fun buckets(span: Span, now: Long, zone: ZoneId): List<Pair<String, ClosedRange<LocalDate>>> {
        val today = day(now, zone)
        return when (span) {
            Span.DAYS -> (13 downTo 0).map { i ->
                val d = today.minusDays(i.toLong())
                d.dayOfMonth.toString() to (d..d)
            }
            Span.WEEKS -> (7 downTo 0).map { i ->
                val start = weekStart(today).minusWeeks(i.toLong())
                "${start.dayOfMonth}/${start.monthValue}" to (start..start.plusDays(6))
            }
            Span.MONTHS -> (5 downTo 0).map { i ->
                val start = today.withDayOfMonth(1).minusMonths(i.toLong())
                start.month.getDisplayName(TextStyle.SHORT, PT_BR).trimEnd('.') to (start..start.plusMonths(1).minusDays(1))
            }
            Span.SESSIONS -> emptyList()
        }
    }

    fun day(at: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    fun weekStart(d: LocalDate): LocalDate = d.with(WeekFields.of(PT_BR).dayOfWeek(), 1L)

    fun daysBetween(a: LocalDate, b: LocalDate): Long = ChronoUnit.DAYS.between(a, b)
}
