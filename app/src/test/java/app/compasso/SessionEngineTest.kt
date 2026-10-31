package app.compasso

import app.compasso.model.EngineEvent
import app.compasso.model.Level
import app.compasso.model.Levels
import app.compasso.model.PhaseType
import app.compasso.model.ReleaseMode
import app.compasso.model.SessionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SessionEngineTest {

    private data class Seen(val type: PhaseType, val round: Int, val durationMs: Long)

    /** Roda a sessão inteira em passos de [stepMs] e devolve a sequência de fases. */
    private fun runToEnd(engine: SessionEngine, stepMs: Long = 10, onEvent: (EngineEvent) -> Unit = {}): List<Seen> {
        val seen = mutableListOf<Seen>()
        fun collect(events: List<EngineEvent>) = events.forEach {
            onEvent(it)
            if (it is EngineEvent.PhaseStart) seen += Seen(it.type, engine.round, engine.phaseDurationMs)
        }
        collect(engine.start())
        var guard = 0
        while (!engine.isFinal && guard++ < 5_000_000) collect(engine.advance(stepMs))
        return seen
    }

    private fun simple(rounds: Int, goSec: Int, stopSec: Int, bpm: Int, release: ReleaseMode, chance: Int = 50) = Level(
        id = "t", name = "Teste", difficulty = 1, rounds = rounds,
        goMinSec = goSec, goMaxSec = goSec, stopMinSec = stopSec, stopMaxSec = stopSec,
        bpmStart = bpm, bpmEnd = bpm, release = release, releaseChance = chance,
    )

    @Test
    fun easyLevelAlternatesRhythmAndPauseThenReleases() {
        val seen = runToEnd(SessionEngine(Levels.EASY, Random(1)))
        val expected = listOf(
            PhaseType.COUNTDOWN to 0,
            PhaseType.GO to 1, PhaseType.STOP to 1,
            PhaseType.GO to 2, PhaseType.STOP to 2,
            PhaseType.GO to 3, PhaseType.STOP to 3,
            PhaseType.GO to 4,
            PhaseType.RELEASE to 4,
        )
        assertEquals(expected, seen.map { it.type to it.round })
    }

    @Test
    fun impossibleNeverReleases() {
        repeat(20) { seed ->
            val engine = SessionEngine(Levels.IMPOSSIBLE, Random(seed))
            val seen = runToEnd(engine, stepMs = 50)
            assertEquals(PhaseType.DENIED, seen.last().type)
            assertEquals(Levels.IMPOSSIBLE.rounds, seen.count { it.type == PhaseType.GO })
            assertEquals(Levels.IMPOSSIBLE.rounds - 1, seen.count { it.type == PhaseType.STOP })
            assertEquals(Levels.IMPOSSIBLE.rounds, engine.roundsCompleted)
        }
    }

    @Test
    fun durationsStayInsideTheLevelRanges() {
        for (level in Levels.presets) {
            repeat(10) { seed ->
                val seen = runToEnd(SessionEngine(level, Random(seed)), stepMs = 50)
                seen.filter { it.type == PhaseType.GO }.forEach {
                    assertTrue("${level.name} ritmo ${it.durationMs}", it.durationMs in level.goMinSec * 1000L..level.goMaxSec * 1000L)
                }
                seen.filter { it.type == PhaseType.STOP }.forEach {
                    assertTrue("${level.name} pausa ${it.durationMs}", it.durationMs in level.stopMinSec * 1000L..level.stopMaxSec * 1000L)
                }
            }
        }
    }

    @Test
    fun beatsFollowTheBpm() {
        var beats = 0
        val engine = SessionEngine(simple(rounds = 1, goSec = 10, stopSec = 5, bpm = 120, release = ReleaseMode.ALWAYS), Random(3), countdownSeconds = 0)
        runToEnd(engine, stepMs = 7) { if (it == EngineEvent.Beat) beats++ }
        assertEquals(20, beats) // 120 bpm por 10 s
        assertEquals(10_000L, engine.activeElapsedMs)
    }

    @Test
    fun countdownTicksThreeTimesAndIsNotCountedAsTraining() {
        var ticks = 0
        val engine = SessionEngine(simple(rounds = 1, goSec = 10, stopSec = 5, bpm = 60, release = ReleaseMode.ALWAYS), Random(3))
        runToEnd(engine) { if (it == EngineEvent.CountTick) ticks++ }
        assertEquals(3, ticks)
        assertEquals(10_000L, engine.activeElapsedMs)
    }

    @Test
    fun closeButtonCutsRhythmIntoLongPauseAndCanExtendIt() {
        val level = simple(rounds = 3, goSec = 30, stopSec = 10, bpm = 90, release = ReleaseMode.ALWAYS)
        val engine = SessionEngine(level, Random(5))
        engine.start()
        engine.advance(3_000) // contagem
        engine.advance(5_000) // 5 s de ritmo na rodada 1
        assertEquals(PhaseType.GO, engine.phase)

        val events = engine.pressClose()
        assertEquals(listOf<EngineEvent>(EngineEvent.PhaseStart(PhaseType.STOP)), events)
        assertEquals(PhaseType.STOP, engine.phase)
        assertEquals(1, engine.closeCount)
        assertEquals(engine.closeStopMs, engine.phaseDurationMs)
        assertEquals(30_000L, engine.closeStopMs) // máx(10, 20) + 10

        engine.pressClose() // ainda perto: +10 s
        assertEquals(40_000L, engine.phaseDurationMs)
        assertEquals(1, engine.closeCount)

        engine.advance(40_000)
        assertEquals(PhaseType.GO, engine.phase)
        assertEquals(2, engine.round)
    }

    @Test
    fun closeOnLastRoundStillPausesBeforeTheEnd() {
        val engine = SessionEngine(simple(rounds = 1, goSec = 20, stopSec = 10, bpm = 90, release = ReleaseMode.ALWAYS), Random(1), countdownSeconds = 0)
        engine.start()
        engine.advance(4_000)
        engine.pressClose()
        assertEquals(PhaseType.STOP, engine.phase)
        engine.advance(engine.closeStopMs)
        assertEquals(PhaseType.RELEASE, engine.phase)
    }

    @Test
    fun chanceZeroNeverReleasesAndHundredAlwaysReleases() {
        repeat(10) { seed ->
            val never = SessionEngine(simple(2, 5, 5, 100, ReleaseMode.CHANCE, chance = 0), Random(seed), 0)
            assertEquals(PhaseType.DENIED, runToEnd(never).last().type)
            val always = SessionEngine(simple(2, 5, 5, 100, ReleaseMode.CHANCE, chance = 100), Random(seed), 0)
            assertEquals(PhaseType.RELEASE, runToEnd(always).last().type)
        }
    }

    @Test
    fun releaseBlockedBySmartControlEndsWithoutRelease() {
        repeat(5) { seed ->
            val engine = SessionEngine(Levels.EASY, Random(seed), countdownSeconds = 0, releaseAllowed = false)
            assertTrue(engine.controlOnly)
            assertEquals(PhaseType.DENIED, runToEnd(engine, stepMs = 50).last().type)
        }
        // nível que já não libera não é "só controle"
        assertTrue(!SessionEngine(Levels.IMPOSSIBLE, Random(1), releaseAllowed = false).controlOnly)
    }

    @Test
    fun speedGoesFromStartToEnd() {
        val engine = SessionEngine(Levels.HARD, Random(9))
        assertEquals(Levels.HARD.bpmStart, engine.plannedBpm(1))
        assertEquals(Levels.HARD.bpmEnd, engine.plannedBpm(Levels.HARD.rounds))
        for (r in 1..Levels.HARD.rounds) assertTrue(engine.plannedBpm(r) in 30..220)
    }

    @Test
    fun estimatedDurations() {
        assertEquals(3, Levels.EASY.estimatedMinutes)
        assertEquals(8, Levels.MEDIUM.estimatedMinutes)
        assertEquals(15, Levels.HARD.estimatedMinutes)
        assertEquals(20, Levels.IMPOSSIBLE.estimatedMinutes)
    }
}
