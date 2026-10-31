package app.compasso.model

import kotlin.math.roundToInt
import kotlin.random.Random

enum class PhaseType { COUNTDOWN, GO, STOP, RELEASE, DENIED }

sealed interface EngineEvent {
    /** Uma batida do ritmo (fase GO). */
    data object Beat : EngineEvent

    /** Um segundo da contagem inicial (3, 2, 1). */
    data object CountTick : EngineEvent

    data class PhaseStart(val type: PhaseType) : EngineEvent
}

/**
 * Lógica pura da sessão, sem nada de Android (por isso dá pra testar no computador):
 *
 * contagem → [ritmo → pausa] × (rodadas − 1) → ritmo → final (liberado ou não)
 *
 * O tempo só anda quando alguém chama [advance]. "Estou perto" ([pressClose]) corta o
 * ritmo na hora e manda para uma pausa longa; durante a pausa, aumenta a pausa em 10 s.
 */
class SessionEngine(
    levelInput: Level,
    private val random: Random = Random.Default,
    private val countdownSeconds: Int = 3,
    /** Falso quando o controle inteligente fechou a liberação: a sessão termina sem liberar. */
    val releaseAllowed: Boolean = true,
) {
    val level: Level = levelInput.sanitized()

    /** Sessão que liberaria pelo nível, mas virou só treino de controle. */
    val controlOnly: Boolean get() = !releaseAllowed && level.release != ReleaseMode.NEVER

    var phase: PhaseType = PhaseType.COUNTDOWN
        private set
    var round: Int = 0
        private set
    var phaseDurationMs: Long = 0L
        private set
    var phaseElapsedMs: Long = 0L
        private set
    var bpm: Int = 0
        private set
    var beatCount: Long = 0L
        private set
    var closeCount: Int = 0
        private set

    /** Tempo de treino (sem a contagem inicial), até chegar no final. */
    var activeElapsedMs: Long = 0L
        private set
    var roundsCompleted: Int = 0
        private set
    var maxBpm: Int = 0
        private set
    var started: Boolean = false
        private set

    private var nextBeatAtMs: Long = 0L
    private val bpms: IntArray = planBpms()

    val phaseRemainingMs: Long get() = (phaseDurationMs - phaseElapsedMs).coerceAtLeast(0L)
    val isFinal: Boolean get() = phase == PhaseType.RELEASE || phase == PhaseType.DENIED

    /** Velocidade da próxima rodada (útil para mostrar durante a pausa). */
    val nextBpm: Int? get() = if (round in 1 until level.rounds) bpms[round] else null

    /** Duração da pausa quando a pessoa toca em "Estou perto". */
    val closeStopMs: Long get() = (maxOf(level.stopMaxSec, 20) + 10) * 1000L

    fun plannedBpm(roundNumber: Int): Int = bpms[roundNumber - 1]

    fun start(): List<EngineEvent> {
        if (started) return emptyList()
        started = true
        val events = mutableListOf<EngineEvent>()
        if (countdownSeconds > 0) enter(PhaseType.COUNTDOWN, countdownSeconds * 1000L, events)
        else enterGo(1, events)
        return events
    }

    fun advance(dtMs: Long): List<EngineEvent> {
        if (!started || isFinal || dtMs <= 0L) return emptyList()
        val events = mutableListOf<EngineEvent>()
        var remaining = dtMs
        while (remaining > 0L && !isFinal) {
            val left = phaseDurationMs - phaseElapsedMs
            if (left <= 0L) {
                next(events)
                continue
            }
            val step = minOf(remaining, left)
            val end = phaseElapsedMs + step
            when (phase) {
                PhaseType.GO -> {
                    val interval = 60_000L / bpm
                    while (nextBeatAtMs < end) {
                        events += EngineEvent.Beat
                        beatCount++
                        nextBeatAtMs += interval
                    }
                }
                PhaseType.COUNTDOWN -> while (nextBeatAtMs < end) {
                    events += EngineEvent.CountTick
                    nextBeatAtMs += 1000L
                }
                else -> Unit
            }
            phaseElapsedMs = end
            if (phase != PhaseType.COUNTDOWN) activeElapsedMs += step
            remaining -= step
            if (phaseElapsedMs >= phaseDurationMs) next(events)
        }
        return events
    }

    fun pressClose(): List<EngineEvent> {
        val events = mutableListOf<EngineEvent>()
        when (phase) {
            PhaseType.GO -> {
                closeCount++
                roundsCompleted = round
                enter(PhaseType.STOP, closeStopMs, events)
            }
            PhaseType.STOP -> phaseDurationMs += EXTEND_STOP_MS
            else -> Unit
        }
        return events
    }

    private fun next(events: MutableList<EngineEvent>) {
        when (phase) {
            PhaseType.COUNTDOWN -> enterGo(1, events)
            PhaseType.GO -> {
                roundsCompleted = round
                if (round < level.rounds) {
                    enter(PhaseType.STOP, randomSeconds(level.stopMinSec, level.stopMaxSec), events)
                } else {
                    enterFinal(events)
                }
            }
            PhaseType.STOP -> if (round < level.rounds) enterGo(round + 1, events) else enterFinal(events)
            PhaseType.RELEASE, PhaseType.DENIED -> Unit
        }
    }

    private fun enterGo(r: Int, events: MutableList<EngineEvent>) {
        round = r
        bpm = bpms[r - 1]
        maxBpm = maxOf(maxBpm, bpm)
        enter(PhaseType.GO, randomSeconds(level.goMinSec, level.goMaxSec), events)
    }

    private fun enterFinal(events: MutableList<EngineEvent>) {
        roundsCompleted = level.rounds
        val release = releaseAllowed && when (level.release) {
            ReleaseMode.ALWAYS -> true
            ReleaseMode.NEVER -> false
            ReleaseMode.CHANCE -> random.nextInt(100) < level.releaseChance
        }
        enter(if (release) PhaseType.RELEASE else PhaseType.DENIED, 0L, events)
    }

    private fun enter(type: PhaseType, durationMs: Long, events: MutableList<EngineEvent>) {
        phase = type
        phaseDurationMs = durationMs
        phaseElapsedMs = 0L
        nextBeatAtMs = 0L
        events += EngineEvent.PhaseStart(type)
    }

    private fun randomSeconds(min: Int, max: Int): Long = random.nextInt(min, max + 1) * 1000L

    /** Velocidade de cada rodada: sobe do início ao fim, com uma variação pequena no meio. */
    private fun planBpms(): IntArray {
        val n = level.rounds
        return IntArray(n) { i ->
            val t = if (n == 1) 0.0 else i.toDouble() / (n - 1)
            val base = level.bpmStart + (level.bpmEnd - level.bpmStart) * t
            val jitter = if (i == 0 || i == n - 1) 0.0 else (random.nextDouble() - 0.5) * 0.12 * base
            (((base + jitter) / 5.0).roundToInt() * 5).coerceIn(30, 220)
        }
    }

    companion object {
        const val EXTEND_STOP_MS = 10_000L
    }
}
