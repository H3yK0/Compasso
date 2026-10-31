package app.compasso

import app.compasso.model.Kegel
import app.compasso.model.KegelEntry
import app.compasso.model.KegelHold
import app.compasso.model.KegelLength
import app.compasso.model.KegelPhase
import app.compasso.model.KegelPlan
import app.compasso.model.PointEntry
import app.compasso.model.PointKind
import app.compasso.model.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Treino de Kegel: roteiro, níveis, desbloqueio, sugestão e pontos. */
class KegelTest {
    private val zone = ZoneOffset.UTC
    private val hour = 3_600_000L

    private fun at(daysAgo: Int, h: Int = 15, m: Int = 0): Long =
        LocalDateTime.of(2026, 3, 11, h, m).minusDays(daysAgo.toLong()).toInstant(zone).toEpochMilli()

    private val now = at(0)

    private fun entry(at: Long, hold: KegelHold = KegelHold.H3) = KegelEntry(at, hold.seconds, 2, 15)

    @Test
    fun repsFitTheChosenTime() {
        assertEquals(15, Kegel.reps(KegelHold.H3, KegelLength.M2))
        assertEquals(9, Kegel.reps(KegelHold.H5, KegelLength.M2))
        assertEquals(16, Kegel.reps(KegelHold.H10, KegelLength.M6))
        // permanência longa no tempo curto: nunca menos que o mínimo
        assertEquals(Kegel.MIN_REPS, Kegel.reps(KegelHold.H10, KegelLength.M2))
        KegelHold.entries.forEach { h ->
            KegelLength.entries.forEach { l ->
                val plan = KegelPlan(h, l)
                // dura o tempo escolhido (ou um pouco mais, quando vale o mínimo de repetições)
                assertTrue("$h $l: ${plan.totalMs}", plan.totalMs <= l.minutes * 60_000L || plan.reps == Kegel.MIN_REPS)
                assertTrue("$h $l: ${plan.totalMs}", plan.totalMs >= l.minutes * 60_000L - 2L * h.seconds * 1_000L)
            }
        }
    }

    @Test
    fun planWalksThroughPrepareHoldsRelaxAndQuickOnes() {
        val plan = KegelPlan(KegelHold.H5, KegelLength.M2)
        assertEquals(KegelPhase.PREPARE, plan.at(0).phase)
        assertEquals(KegelPhase.PREPARE, plan.at(4_999).phase)

        val s1 = plan.at(5_000)
        assertEquals(KegelPhase.SQUEEZE, s1.phase)
        assertEquals(1, s1.rep)
        assertEquals(5_000, s1.remainingMs)
        // soltar dura o mesmo que segurar
        val r1 = plan.at(10_000)
        assertEquals(KegelPhase.RELAX, r1.phase)
        assertEquals(1, r1.rep)
        assertEquals(KegelPhase.SQUEEZE, plan.at(15_000).phase)
        assertEquals(2, plan.at(15_000).rep)

        // depois das lentas, 10 rápidas de 1 s
        val quickStart = 5_000L + plan.reps * 10_000L
        val q = plan.at(quickStart)
        assertEquals(KegelPhase.QUICK_SQUEEZE, q.phase)
        assertEquals(1, q.quick)
        assertEquals(KegelPhase.QUICK_RELAX, plan.at(quickStart + 1_000).phase)
        assertEquals(10, plan.at(plan.totalMs - 1).quick)

        val end = plan.at(plan.totalMs)
        assertEquals(KegelPhase.DONE, end.phase)
        assertEquals(1f, end.progress)
        assertEquals(quickStart + 20_000L, plan.totalMs)
    }

    @Test
    fun squeezingPhases() {
        assertTrue(KegelPhase.SQUEEZE.squeezing)
        assertTrue(KegelPhase.QUICK_SQUEEZE.squeezing)
        assertFalse(KegelPhase.RELAX.squeezing)
        assertFalse(KegelPhase.PREPARE.squeezing)
    }

    @Test
    fun nextHoldUnlocksAfterFiveCompleteWorkouts() {
        val four = (1..4).map { entry(at(it)) }
        assertEquals(1, Kegel.remainingToUnlock(KegelHold.H3, four))
        assertEquals(KegelHold.H3, Kegel.unlockedAfter(KegelHold.H3, four))
        val five = four + entry(at(0))
        assertEquals(0, Kegel.remainingToUnlock(KegelHold.H3, five))
        assertEquals(KegelHold.H5, Kegel.unlockedAfter(KegelHold.H3, five))
        // treinos num nível mais leve não contam para liberar o próximo
        assertEquals(5, Kegel.remainingToUnlock(KegelHold.H5, five))
        // um degrau por vez, e o último não vai além
        assertEquals(KegelHold.H10, Kegel.unlockedAfter(KegelHold.H10, (1..9).map { entry(at(it), KegelHold.H10) }))
        assertEquals(0, Kegel.remainingToUnlock(KegelHold.H10, emptyList()))
    }

    @Test
    fun countsTodayAndTheWeek() {
        val log = listOf(entry(at(0, 9)), entry(at(0, 13)), entry(at(1)), entry(at(3)), entry(at(3, 20)), entry(at(9)))
        assertEquals(2, Kegel.today(log, now, zone))
        assertEquals(listOf(0, 0, 0, 2, 0, 1, 2), Kegel.week(log, now, zone))
        // entradas "no futuro" (relógio mexido) não contam
        assertEquals(2, Kegel.today(log + entry(now + hour), now, zone))
    }

    @Test
    fun pruneKeepsThreeMonths() {
        val log = listOf(entry(at(1)), entry(at(89)), entry(at(91)))
        assertEquals(2, Kegel.prune(log, now).size)
    }

    @Test
    fun nudgeOnlyWhenThereIsKegelLeftDuringTheDay() {
        val morning = at(0, 10)
        assertTrue(Kegel.shouldNudge(emptyList(), morning, lastNudgeAt = at(1), zone = zone))
        // de noite e de madrugada, não
        assertFalse(Kegel.shouldNudge(emptyList(), at(0, 22, 30), at(1), zone))
        assertFalse(Kegel.shouldNudge(emptyList(), at(0, 7, 59), at(1), zone))
        // sugestão há menos de 3 h, não
        assertFalse(Kegel.shouldNudge(emptyList(), morning, morning - 2 * hour, zone))
        // treino há menos de 3 h, não
        assertFalse(Kegel.shouldNudge(listOf(entry(morning - hour)), morning, at(1), zone))
        assertTrue(Kegel.shouldNudge(listOf(entry(morning - 4 * hour)), morning, at(1), zone))
        // os 3 do dia feitos, não
        val three = listOf(entry(at(0, 8)), entry(at(0, 9)), entry(at(0, 9, 30)))
        assertFalse(Kegel.shouldNudge(three, at(0, 16), at(1), zone))
    }

    @Test
    fun kegelEarnsPointsThreeTimesADayOnTopOfTheRest() {
        var log = listOf(
            PointEntry(now - 3 * hour, PointKind.CHECKIN, 10),
            PointEntry(now - 3 * hour, PointKind.CONTROL_SESSION, 15),
        ) + List(3) { PointEntry(now - 2 * hour, PointKind.BREATHE, 5) }
        // 40 pontos de outras coisas: o Kegel ainda soma (teto de 50)
        repeat(Kegel.PER_DAY - 1) {
            val a = Progress.award(log, PointKind.KEGEL, now, zone)
            assertEquals(5, a.entry?.points)
            log = log + a.entry!!
        }
        assertEquals(50, Progress.pointsToday(log, now, zone))
        val third = Progress.award(log, PointKind.KEGEL, now, zone)
        assertNull(third.entry)

        // sem outras coisas: 3 por dia, o 4º não rende
        var only = emptyList<PointEntry>()
        repeat(3) { only = only + Progress.award(only, PointKind.KEGEL, now, zone).entry!! }
        assertNull(Progress.award(only, PointKind.KEGEL, now, zone).entry)
        assertEquals(15, Progress.pointsToday(only, now, zone))
    }
}
