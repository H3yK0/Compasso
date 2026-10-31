package app.compasso

import app.compasso.model.Lapse
import app.compasso.model.Outcome
import app.compasso.model.Pacing
import app.compasso.model.PacingConfig
import app.compasso.model.PacingEntry
import app.compasso.model.PinPolicy
import app.compasso.model.ReleaseGate
import app.compasso.model.SessionRecord
import app.compasso.model.TrustedTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proteções contra burlar as regras (relógio, senha, histórico). */
class SafeguardsTest {
    private val hour = 3_600_000L
    private val t0 = 1_800_000_000_000L

    // ---------- Relógio confiável ----------

    @Test
    fun firstReadingTrustsTheWallClock() {
        assertEquals(TrustedTime.Mark(t0, 5_000L), TrustedTime.next(null, t0, 5_000L))
    }

    @Test
    fun normalTimeFollowsTheWallClock() {
        val m = TrustedTime.next(TrustedTime.Mark(t0, 1_000L), t0 + hour, 1_000L + hour)
        assertEquals(t0 + hour, m.trusted)
    }

    @Test
    fun turningTheClockBackDoesNotGoBackInTime() {
        val last = TrustedTime.Mark(t0, 10_000L)
        // a pessoa atrasou o relógio 3 dias, e passou 1 minuto de verdade
        val m = TrustedTime.next(last, t0 - 72 * hour, 10_000L + 60_000L)
        assertEquals(t0 + 60_000L, m.trusted)
    }

    @Test
    fun afterARebootOnlyCountsTheTimeSinceBoot() {
        val last = TrustedTime.Mark(t0, 5 * hour)
        // reiniciou (uptime menor) e o relógio foi atrasado: conta só os 10 min desde que ligou
        val m = TrustedTime.next(last, t0 - 48 * hour, 10 * 60_000L)
        assertEquals(t0 + 10 * 60_000L, m.trusted)
    }

    /** Atrasar a hora depois de liberar não reabre a liberação. */
    @Test
    fun clockBackCannotReopenTheRelease() {
        val cfg = PacingConfig(true, 24)
        val log = listOf(PacingEntry(t0 - hour, t0, released = true, counts = true))
        val trusted = TrustedTime.next(TrustedTime.Mark(t0, 0L), t0 - 30 * hour, 60_000L).trusted
        assertTrue(Pacing.evaluate(cfg, log, trusted) is ReleaseGate.Closed)
    }

    // ---------- Senha ----------

    @Test
    fun pinWaitStartsAtTheFifthErrorAndGrows() {
        assertEquals(0L, PinPolicy.waitAfter(4))
        assertEquals(30_000L, PinPolicy.waitAfter(5))
        assertEquals(30_000L, PinPolicy.waitAfter(9))
        assertEquals(60_000L, PinPolicy.waitAfter(10))
        assertEquals(PinPolicy.MAX_WAIT_MS, PinPolicy.waitAfter(1_000))
    }

    // ---------- Lapsos ----------

    @Test
    fun onlyLostControlAndInterruptedAreLapses() {
        fun rec(o: Outcome) = SessionRecord(t0, t0 + hour, "facil", "Fácil", hour, 4, 4, 0, 120, o)
        assertEquals(Lapse(t0, Outcome.LOST_CONTROL), Lapse.from(rec(Outcome.LOST_CONTROL)))
        assertEquals(Lapse(t0, Outcome.INTERRUPTED), Lapse.from(rec(Outcome.INTERRUPTED)))
        assertNull(Lapse.from(rec(Outcome.RELEASED)))
        assertNull(Lapse.from(rec(Outcome.DENIED)))
        assertNull(Lapse.from(rec(Outcome.STOPPED)))
    }
}
