package app.compasso

import app.compasso.model.Breath
import app.compasso.model.GateRule
import app.compasso.model.Pacing
import app.compasso.model.PacingConfig
import app.compasso.model.PacingEntry
import app.compasso.model.PacingSettings
import app.compasso.model.PendingChange
import app.compasso.model.ReleaseGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacingTest {
    private val h = Pacing.HOUR
    private val now = 1_900_000_000_000L
    private val on24 = PacingConfig(enabled = true, intervalHours = 24)

    /** Sessão que começou há [startAgoH] horas e durou [minutes] minutos. */
    private fun session(startAgoH: Double, minutes: Int = 10, released: Boolean = false, counts: Boolean = true): PacingEntry {
        val start = now - (startAgoH * h).toLong()
        return PacingEntry(start, start + minutes * 60_000L, released, counts)
    }

    @Test
    fun noHistoryReleaseIsOpen() {
        assertEquals(ReleaseGate.Open(24, false), Pacing.evaluate(on24, emptyList(), now))
    }

    @Test
    fun releaseTenHoursAgoClosesUntil24hAfterIt() {
        val s = session(10.0, released = true)
        val g = Pacing.evaluate(on24, listOf(s), now) as ReleaseGate.Closed
        assertEquals(setOf(GateRule.INTERVAL), g.rules)
        assertEquals(s.endedAt + 24 * h, g.until)
    }

    @Test
    fun releaseThirtyHoursAgoIsOpen() {
        assertTrue(Pacing.evaluate(on24, listOf(session(30.0, released = true)), now) is ReleaseGate.Open)
    }

    @Test
    fun lostControlCountsAsRelease() {
        // PacingEntry.from marca "não aguentei" como liberação
        val g = Pacing.evaluate(on24, listOf(session(5.0, released = true)), now)
        assertTrue(g is ReleaseGate.Closed)
    }

    @Test
    fun sessionRightAfterAnotherIsControlOnly() {
        val old = session(72.0, released = true)
        val recent = session(1.0) // terminou há 50 min, sem liberação
        val g = Pacing.evaluate(on24, listOf(old, recent), now) as ReleaseGate.Closed
        assertEquals(setOf(GateRule.RUSH), g.rules)
        assertEquals(recent.endedAt + 2 * h, g.until)
    }

    @Test
    fun bothRulesUseTheLatestDeadline() {
        val s = session(1.0, released = true)
        val g = Pacing.evaluate(on24, listOf(s), now) as ReleaseGate.Closed
        assertEquals(setOf(GateRule.INTERVAL, GateRule.RUSH), g.rules)
        assertEquals(s.endedAt + 24 * h, g.until)
    }

    @Test
    fun accidentalShortSessionDoesNotCount() {
        val old = session(72.0, released = true)
        val oops = session(0.5, minutes = 1, counts = false)
        assertTrue(Pacing.evaluate(on24, listOf(old, oops), now) is ReleaseGate.Open)
    }

    @Test
    fun busyWeekDoublesTheInterval() {
        val log = listOf(session(30.0, released = true), session(50.0), session(75.0), session(100.0), session(125.0))
        val g = Pacing.evaluate(on24, log, now) as ReleaseGate.Closed
        assertTrue(g.busyWeek)
        assertEquals(48, g.intervalHours)
        assertEquals(setOf(GateRule.INTERVAL), g.rules)
        assertEquals(log[0].endedAt + 48 * h, g.until)
    }

    @Test
    fun busyWeekEndsWhenTheOldestSessionLeavesTheWindow() {
        // a sessão mais antiga sai da janela de 7 dias daqui a 2 h; aí volta a valer 24 h (já passaram)
        val log = listOf(session(30.0, released = true), session(50.0), session(75.0), session(100.0), session(166.0))
        val g = Pacing.evaluate(on24, log, now) as ReleaseGate.Closed
        assertEquals(log[4].startedAt + Pacing.WEEK, g.until)
        assertTrue(Pacing.evaluate(on24, log, g.until) is ReleaseGate.Open)
        assertTrue(Pacing.evaluate(on24, log, g.until - 1) is ReleaseGate.Closed)
    }

    @Test
    fun untilIsAlwaysTheFirstMomentItOpens() {
        val logs = listOf(
            listOf(session(10.0, released = true)),
            listOf(session(3.0, released = true), session(1.5)),
            listOf(session(30.0, released = true), session(50.0), session(75.0), session(100.0), session(166.0)),
            listOf(session(20.0, released = true), session(40.0, released = true), session(60.0), session(80.0), session(150.0)),
        )
        for (cfg in Pacing.INTERVAL_OPTIONS.map { PacingConfig(true, it) }) {
            for (log in logs) {
                val g = Pacing.evaluate(cfg, log, now)
                if (g is ReleaseGate.Closed) {
                    assertTrue("abre em until", Pacing.evaluate(cfg, log, g.until) !is ReleaseGate.Closed)
                    assertTrue("fechado logo antes", Pacing.evaluate(cfg, log, g.until - 1) is ReleaseGate.Closed)
                }
            }
        }
    }

    @Test
    fun disabledIsOffAndFutureEntriesAreIgnored() {
        assertEquals(ReleaseGate.Off, Pacing.evaluate(PacingConfig(false, 24), listOf(session(1.0, released = true)), now))
        assertTrue(Pacing.evaluate(on24, listOf(session(-5.0, released = true)), now) is ReleaseGate.Open)
    }

    @Test
    fun snapshotCountsTheWeek() {
        val log = listOf(session(30.0, released = true), session(50.0), session(200.0))
        val snap = Pacing.snapshot(on24, log, now)
        assertEquals(2, snap.sessionsThisWeek)
        assertEquals(log[0].endedAt, snap.lastRelease)
    }

    // ---------- Mudanças nos ajustes ----------

    @Test
    fun shorterIntervalOnlyAppliesAfter24h() {
        val r = Pacing.request(PacingSettings(on24), PacingConfig(true, 12), now)
        assertEquals(on24, r.config)
        assertEquals(PendingChange(PacingConfig(true, 12), now + 24 * h), r.pending)
        assertEquals(PacingConfig(true, 12), r.requested)
        assertEquals(on24, Pacing.applyDue(r, now + 23 * h).config)
        assertEquals(PacingConfig(true, 12), Pacing.applyDue(r, now + 24 * h).config)
        assertNull(Pacing.applyDue(r, now + 24 * h).pending)
    }

    @Test
    fun turningOffOnlyAppliesAfter24h() {
        val r = Pacing.request(PacingSettings(on24), PacingConfig(false, 24), now)
        assertTrue(r.config.enabled)
        assertEquals(now + 24 * h, r.pending?.effectiveAt)
    }

    @Test
    fun stricterAppliesNowAndCancelsPending() {
        val pending = Pacing.request(PacingSettings(on24), PacingConfig(true, 12), now)
        val r = Pacing.request(pending, PacingConfig(true, 48), now + h)
        assertEquals(PacingConfig(true, 48), r.config)
        assertNull(r.pending)
    }

    @Test
    fun choosingCurrentAgainCancelsPending() {
        val pending = Pacing.request(PacingSettings(on24), PacingConfig(false, 24), now)
        val r = Pacing.request(pending, on24, now + h)
        assertEquals(on24, r.config)
        assertNull(r.pending)
    }

    @Test
    fun repeatingTheSameLooserChangeKeepsTheOriginalTime() {
        val first = Pacing.request(PacingSettings(on24), PacingConfig(true, 12), now)
        val again = Pacing.request(first, PacingConfig(true, 12), now + 5 * h)
        assertEquals(now + 24 * h, again.pending?.effectiveAt)
    }

    @Test
    fun whenOffAnyChangeIsImmediate() {
        val r = Pacing.request(PacingSettings(PacingConfig(false, 24)), PacingConfig(true, 12), now)
        assertEquals(PacingConfig(true, 12), r.config)
        assertNull(r.pending)
    }

    // ---------- Respiração ----------

    @Test
    fun breathingCycle() {
        assertEquals(0f, Breath.level(0), 0.001f)
        assertEquals(0.5f, Breath.level(2_000), 0.001f)
        assertEquals(1f, Breath.level(4_000), 0.001f)
        assertEquals(0.5f, Breath.level(7_000), 0.001f)
        assertEquals(0f, Breath.level(10_000), 0.001f)
        assertTrue(Breath.isInhale(3_999))
        assertTrue(!Breath.isInhale(4_000))
        assertEquals(0L, Breath.step(0))
        assertEquals(1L, Breath.step(4_000))
        assertEquals(2L, Breath.step(10_000))
    }
}
