package app.compasso.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Quanto tempo segurar cada contração. Os níveis liberam aos poucos (veja [Kegel.TO_UNLOCK]). */
enum class KegelHold(val seconds: Int) {
    H3(3),
    H5(5),
    H8(8),
    H10(10),
    ;

    /** 1 a 4. */
    val level: Int get() = ordinal + 1

    val next: KegelHold? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun ofSeconds(s: Int): KegelHold? = entries.firstOrNull { it.seconds == s }
    }
}

/** Quanto tempo dura o treino inteiro. Escolha livre. */
enum class KegelLength(val minutes: Int) {
    M2(2),
    M4(4),
    M6(6),
    ;

    companion object {
        fun ofMinutes(m: Int): KegelLength? = entries.firstOrNull { it.minutes == m }
    }
}

enum class KegelPhase {
    /** Alguns segundos para se ajeitar antes de começar. */
    PREPARE,

    /** Contrair e segurar. */
    SQUEEZE,

    /** Soltar por completo (dura o mesmo que segurar). */
    RELAX,

    /** Contrações rápidas do fim: 1 s contrai, 1 s solta. */
    QUICK_SQUEEZE,
    QUICK_RELAX,

    DONE,
    ;

    val squeezing: Boolean get() = this == SQUEEZE || this == QUICK_SQUEEZE
}

/** Onde o treino está num instante. [remainingMs] é o que falta na fase atual. */
data class KegelState(
    val phase: KegelPhase,
    val remainingMs: Long,
    /** Repetição lenta atual (1…[reps]); nas rápidas, a última. */
    val rep: Int,
    val reps: Int,
    /** Contração rápida atual (0 antes delas, 1…[quickTotal] durante). */
    val quick: Int,
    val quickTotal: Int,
    /** 0 a 1 no treino inteiro. */
    val progress: Float,
    /** 0 a 1 dentro da fase atual. */
    val phaseProgress: Float,
)

/** Roteiro de um treino: preparo, contrações lentas (segura e solta) e, no fim, as rápidas. */
class KegelPlan(val hold: KegelHold, val length: KegelLength) {
    private data class Step(val phase: KegelPhase, val start: Long, val duration: Long, val rep: Int, val quick: Int)

    val reps: Int = Kegel.reps(hold, length)
    val quickTotal: Int = Kegel.QUICK_REPS

    private val steps: List<Step>
    val totalMs: Long

    init {
        val list = mutableListOf<Step>()
        var t = 0L
        fun add(phase: KegelPhase, ms: Long, rep: Int, quick: Int) {
            list += Step(phase, t, ms, rep, quick)
            t += ms
        }
        add(KegelPhase.PREPARE, Kegel.PREPARE_MS, 1, 0)
        val holdMs = hold.seconds * 1_000L
        for (r in 1..reps) {
            add(KegelPhase.SQUEEZE, holdMs, r, 0)
            add(KegelPhase.RELAX, holdMs, r, 0)
        }
        for (q in 1..quickTotal) {
            add(KegelPhase.QUICK_SQUEEZE, Kegel.QUICK_MS, reps, q)
            add(KegelPhase.QUICK_RELAX, Kegel.QUICK_MS, reps, q)
        }
        steps = list
        totalMs = t
    }

    fun at(elapsedMs: Long): KegelState {
        if (elapsedMs >= totalMs) {
            return KegelState(KegelPhase.DONE, 0L, reps, reps, quickTotal, quickTotal, 1f, 1f)
        }
        val e = elapsedMs.coerceAtLeast(0L)
        // busca binária: os passos estão em ordem
        var lo = 0
        var hi = steps.lastIndex
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (steps[mid].start <= e) lo = mid else hi = mid - 1
        }
        val s = steps[lo]
        val inPhase = e - s.start
        return KegelState(
            phase = s.phase,
            remainingMs = s.duration - inPhase,
            rep = s.rep,
            reps = reps,
            quick = s.quick,
            quickTotal = quickTotal,
            progress = (e.toFloat() / totalMs).coerceIn(0f, 1f),
            phaseProgress = (inPhase.toFloat() / s.duration).coerceIn(0f, 1f),
        )
    }
}

/** Um treino de Kegel completo (só os completos ficam registrados). */
data class KegelEntry(val at: Long, val holdSec: Int, val minutes: Int, val reps: Int)

object Kegel {
    const val PREPARE_MS = 5_000L
    const val QUICK_REPS = 10
    const val QUICK_MS = 1_000L

    /** Treinos por dia que contam (e o quanto basta: descansar também é treino). */
    const val PER_DAY = 3

    /** Treinos completos no nível mais alto já liberado para liberar o próximo. */
    const val TO_UNLOCK = 5

    /** Mínimo de contrações lentas, mesmo no treino mais curto com a permanência mais longa. */
    const val MIN_REPS = 5

    private const val DAY = 24 * 3_600_000L
    const val KEEP_DAYS = 90L

    /** Sugestão "Hora do Kegel": no máximo a cada 3 h, só de dia. */
    const val NUDGE_EVERY_MS = 3 * 3_600_000L
    const val NUDGE_FROM_HOUR = 8
    const val NUDGE_UNTIL_HOUR = 22

    /** Contrações lentas que cabem no tempo escolhido, descontando o preparo e as rápidas. */
    fun reps(hold: KegelHold, length: KegelLength): Int {
        val fixed = PREPARE_MS + QUICK_REPS * 2 * QUICK_MS
        val available = length.minutes * 60_000L - fixed
        return maxOf(MIN_REPS, (available / (2L * hold.seconds * 1_000L)).toInt())
    }

    private fun day(at: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    /** Treinos completos hoje. */
    fun today(log: List<KegelEntry>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val d = day(now, zone)
        return log.count { it.at <= now && day(it.at, zone) == d }
    }

    /** Treinos por dia nos últimos 7 dias (do mais antigo até hoje). */
    fun week(log: List<KegelEntry>, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Int> {
        val today = day(now, zone)
        val counts = log.filter { it.at <= now }.groupingBy { day(it.at, zone) }.eachCount()
        return (6 downTo 0).map { counts[today.minusDays(it.toLong())] ?: 0 }
    }

    /** Quantos treinos completos ainda faltam no nível [unlocked] para liberar o próximo (0 = já pode). */
    fun remainingToUnlock(unlocked: KegelHold, log: List<KegelEntry>): Int {
        if (unlocked.next == null) return 0
        val done = log.count { it.holdSec == unlocked.seconds }
        return (TO_UNLOCK - done).coerceAtLeast(0)
    }

    /** O nível mais alto liberado depois dos treinos do [log] (só sobe, um degrau por vez). */
    fun unlockedAfter(unlocked: KegelHold, log: List<KegelEntry>): KegelHold {
        val next = unlocked.next ?: return unlocked
        return if (remainingToUnlock(unlocked, log) == 0) next else unlocked
    }

    fun prune(log: List<KegelEntry>, now: Long): List<KegelEntry> = log.filter { it.at > now - KEEP_DAYS * DAY }

    /**
     * Mostrar a sugestão "Hora do Kegel" ao abrir? Só se ainda faltar treino no dia, de dia, e se já
     * passaram [NUDGE_EVERY_MS] do último treino e da última sugestão.
     */
    fun shouldNudge(log: List<KegelEntry>, now: Long, lastNudgeAt: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (today(log, now, zone) >= PER_DAY) return false
        val hour = Instant.ofEpochMilli(now).atZone(zone).hour
        if (hour < NUDGE_FROM_HOUR || hour >= NUDGE_UNTIL_HOUR) return false
        if (lastNudgeAt > 0L && now - lastNudgeAt < NUDGE_EVERY_MS) return false
        val last = log.filter { it.at <= now }.maxOfOrNull { it.at }
        return last == null || now - last >= NUDGE_EVERY_MS
    }
}
