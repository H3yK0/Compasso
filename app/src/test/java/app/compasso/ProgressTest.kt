package app.compasso

import app.compasso.model.Codename
import app.compasso.model.Codenames
import app.compasso.model.Focus
import app.compasso.model.Lapse
import app.compasso.model.FocusSettings
import app.compasso.model.NoPoints
import app.compasso.model.Outcome
import app.compasso.model.Pacing
import app.compasso.model.PointEntry
import app.compasso.model.PointKind
import app.compasso.model.Progress
import app.compasso.model.SessionRecord
import app.compasso.model.Span
import app.compasso.model.Tier
import app.compasso.model.UnbeatenNeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

class ProgressTest {
    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val day = 24 * 3_600_000L

    /** 12/10/2026 às 15:00, horário de Brasília. */
    private val now = LocalDateTime.of(2026, 10, 12, 15, 0).atZone(zone).toInstant().toEpochMilli()

    private fun at(daysAgo: Int, hour: Int = 12): Long =
        LocalDateTime.of(2026, 10, 12, hour, 0).minusDays(daysAgo.toLong()).atZone(zone).toInstant().toEpochMilli()

    private fun p(daysAgo: Int, kind: PointKind, points: Int = kind.base, hour: Int = 12) = PointEntry(at(daysAgo, hour), kind, points)

    private fun lapse(daysAgo: Int, outcome: Outcome) = Lapse(at(daysAgo, 20), outcome)

    private fun rec(daysAgo: Int, outcome: Outcome, minutes: Int = 10, focused: Boolean = true): SessionRecord {
        val start = at(daysAgo, 20)
        return SessionRecord(start, start + minutes * 60_000L, "medio", "Médio", minutes * 60_000L, 7, 7, 1, 120, outcome, focused = focused)
    }

    // ---------- Pontos do dia ----------

    @Test
    fun dailyCapLimitsToFiftyAndGivesPartialPoints() {
        val full = listOf(p(0, PointKind.CHECKIN), p(0, PointKind.CONTROL_SESSION)) + List(3) { p(0, PointKind.BREATHE) } +
            List(2) { p(0, PointKind.KEGEL) }
        assertEquals(50, Progress.pointsToday(full, now, zone))
        val capped = Progress.award(full, PointKind.GAME, now, zone)
        assertNull(capped.entry)
        assertEquals(NoPoints.DAILY_CAP, capped.reason)

        val almost = listOf(p(0, PointKind.CHECKIN), p(0, PointKind.CONTROL_SESSION), p(0, PointKind.BREATHE), p(0, PointKind.ARTICLE), p(0, PointKind.GAME)) +
            List(2) { p(0, PointKind.KEGEL) }
        assertEquals(48, Progress.pointsToday(almost, now, zone))
        assertEquals(2, Progress.award(almost, PointKind.GAME, now, zone).entry?.points)
    }

    @Test
    fun onlyFirstSessionOfTheDayScores() {
        val log = listOf(p(0, PointKind.CONTROL_SESSION))
        assertEquals(NoPoints.SESSION_LIMIT, Progress.award(log, PointKind.SESSION, now, zone).reason)
        assertEquals(NoPoints.SESSION_LIMIT, Progress.award(log, PointKind.HONEST, now, zone).reason)
        // ontem não conta para hoje
        assertEquals(10, Progress.award(listOf(p(1, PointKind.CONTROL_SESSION)), PointKind.SESSION, now, zone).entry?.points)
    }

    @Test
    fun eachActionHasItsOwnDailyLimit() {
        val breathes = List(3) { p(0, PointKind.BREATHE) }
        assertEquals(NoPoints.KIND_LIMIT, Progress.award(breathes, PointKind.BREATHE, now, zone).reason)
        assertNotNull(Progress.award(breathes, PointKind.GAME, now, zone).entry)
        val checkin = listOf(p(0, PointKind.CHECKIN))
        assertEquals(NoPoints.KIND_LIMIT, Progress.award(checkin, PointKind.CHECKIN, now, zone).reason)
        assertTrue(Progress.doneToday(checkin, PointKind.CHECKIN, now, zone))
        assertFalse(Progress.doneToday(listOf(p(1, PointKind.CHECKIN)), PointKind.CHECKIN, now, zone))
    }

    @Test
    fun sessionPointsNeedFocusModeWithPinnedScreen() {
        assertEquals(PointKind.CONTROL_SESSION, Progress.kindFor(rec(0, Outcome.DENIED)))
        assertEquals(PointKind.SESSION, Progress.kindFor(rec(0, Outcome.RELEASED)))
        assertEquals(PointKind.HONEST, Progress.kindFor(rec(0, Outcome.LOST_CONTROL)))
        assertNull(Progress.kindFor(rec(0, Outcome.DENIED, focused = false)))
        assertNull(Progress.kindFor(rec(0, Outcome.INTERRUPTED)))
        assertNull(Progress.kindFor(rec(0, Outcome.STOPPED)))
    }

    // ---------- Níveis ----------

    /** Um dia "bom": check-in + sessão sem liberação + respirar = 30 pontos. */
    private fun goodDays(n: Int, startDaysAgo: Int = 0): List<PointEntry> = (startDaysAgo until startDaysAgo + n).flatMap { d ->
        listOf(p(d, PointKind.CHECKIN), p(d, PointKind.CONTROL_SESSION), p(d, PointKind.BREATHE))
    }

    @Test
    fun tierCountsOnlyTheLast28Days() {
        val log = goodDays(13) + goodDays(10, startDaysAgo = 40) // 390 recentes + 300 antigos
        val s = Progress.level(log, emptyList(), now, zone)
        assertEquals(390, s.windowPoints)
        assertEquals(Tier.GOLD, s.tier)
        assertEquals(Tier.PLATINUM, s.next)
        assertEquals(150, s.toNext)
        assertEquals(30f / 180f, s.progress, 0.001f)
    }

    @Test
    fun unbeatenNeedsStreakAndCleanFortnight() {
        // 33 por dia em 28 dias (924) + 4 artigos (20) = 944
        val log = goodDays(28) + List(28) { p(it, PointKind.GAME) } + (10..13).map { p(it, PointKind.ARTICLE) }
        val clean = Progress.level(log, listOf(lapse(20, Outcome.LOST_CONTROL)), now, zone)
        assertEquals(944, clean.windowPoints)
        assertEquals(Tier.UNBEATEN, clean.tier)
        assertTrue(clean.missing.isEmpty())

        val lostRecently = Progress.level(log, listOf(lapse(3, Outcome.LOST_CONTROL)), now, zone)
        assertEquals(Tier.DIAMOND, lostRecently.tier)
        assertEquals(listOf(UnbeatenNeed.CLEAN_DAYS), lostRecently.missing)

        val gap = log.filter { Progress.day(it.at, zone) != Progress.day(at(5), zone) } // um dia sem nada
        val noStreak = Progress.level(gap, emptyList(), now, zone)
        assertTrue(noStreak.windowPoints >= Tier.UNBEATEN.minPoints)
        assertEquals(Tier.DIAMOND, noStreak.tier)
        assertEquals(listOf(UnbeatenNeed.STREAK), noStreak.missing)
    }

    // ---------- Sequência, pódio e recordes ----------

    @Test
    fun streakCountsConsecutiveDaysAndInterruptedBreaksIt() {
        val log = listOf(p(0, PointKind.BREATHE), p(1, PointKind.GAME), p(2, PointKind.CHECKIN), p(4, PointKind.CHECKIN))
        assertEquals(3, Progress.streak(log, emptyList(), now, zone))
        // hoje ainda vazio: conta a partir de ontem
        assertEquals(2, Progress.streak(log.drop(1), emptyList(), now, zone))
        // dia com sessão interrompida não vale
        assertEquals(1, Progress.streak(log, listOf(lapse(1, Outcome.INTERRUPTED)), now, zone))
        assertEquals(3, Progress.bestStreak(log, emptyList(), zone))
    }

    @Test
    fun podiumHasTheThreeBestWeeks() {
        val log = goodDays(2) + goodDays(5, startDaysAgo = 7) + goodDays(3, startDaysAgo = 21) + goodDays(1, startDaysAgo = 35)
        val podium = Progress.podium(log, zone)
        assertEquals(3, podium.size)
        assertTrue(podium[0].points >= podium[1].points && podium[1].points >= podium[2].points)
        assertEquals(podium[0].points, Progress.records(log, emptyList(), emptyList(), zone).bestWeekPoints)
    }

    @Test
    fun recordsIgnoreInterruptedSessions() {
        val r = Progress.records(emptyList(), listOf(rec(1, Outcome.DENIED, 12), rec(2, Outcome.INTERRUPTED, 40), rec(3, Outcome.RELEASED, 8)), emptyList(), zone)
        assertEquals(12 * 60_000L, r.bestControlMs)
        assertEquals(2, r.completedSessions)
        assertEquals(1, r.controlSessions)
    }

    // ---------- Séries dos gráficos ----------

    @Test
    fun controlSeriesByDayUsesTheLongestSessionOfEachDay() {
        val history = listOf(rec(0, Outcome.DENIED, 12), rec(0, Outcome.RELEASED, 5), rec(2, Outcome.DENIED, 9), rec(1, Outcome.INTERRUPTED, 30))
        val bars = Progress.controlSeries(history, Span.DAYS, now, zone)
        assertEquals(14, bars.size)
        assertEquals(12f, bars.last().value, 0.001f)
        assertEquals(0f, bars[12].value, 0.001f) // ontem: só a interrompida, que fica de fora
        assertEquals(9f, bars[11].value, 0.001f)
        assertEquals("12", bars.last().label)
    }

    @Test
    fun controlSeriesBySessionKeepsTheLastTwelveInOrder() {
        val history = (0 until 15).map { rec(it, Outcome.DENIED, it + 1) }
        val bars = Progress.controlSeries(history, Span.SESSIONS, now, zone)
        assertEquals(12, bars.size)
        assertEquals(12f, bars.first().value, 0.001f) // a mais antiga das 12
        assertEquals(1f, bars.last().value, 0.001f) // a de hoje
    }

    @Test
    fun pointSeriesByWeekAndMonth() {
        val log = goodDays(10)
        val weeks = Progress.pointsSeries(log, Span.WEEKS, now, zone)
        assertEquals(8, weeks.size)
        assertEquals(300f, weeks.sumOf { it.value.toDouble() }.toFloat(), 0.001f)
        val months = Progress.pointsSeries(log, Span.MONTHS, now, zone, Locale.forLanguageTag("pt-BR"))
        assertEquals(6, months.size)
        assertEquals("out", months.last().label)
        assertEquals("Oct", Progress.pointsSeries(log, Span.MONTHS, now, zone, Locale.ENGLISH).last().label)
        assertEquals("oct", Progress.pointsSeries(log, Span.MONTHS, now, zone, Locale.forLanguageTag("es")).last().label)
        // semana: dia/mês em português e espanhol, mês/dia em inglês
        assertEquals("11/10", Progress.pointsSeries(log, Span.WEEKS, now, zone, Locale.forLanguageTag("pt-BR")).last().label)
        assertEquals("10/11", Progress.pointsSeries(log, Span.WEEKS, now, zone, Locale.ENGLISH).last().label)
    }

    @Test
    fun pruneKeepsAboutOneYear() {
        val log = listOf(p(10, PointKind.GAME), p(399, PointKind.GAME), p(401, PointKind.GAME))
        assertEquals(2, Progress.prune(log, now).size)
    }

    // ---------- Modo foco ----------

    @Test
    fun turningFocusOffWaits24Hours() {
        val on = FocusSettings()
        val off = Focus.request(on, enable = false, now = now)
        assertTrue(off.enabled)
        assertTrue(off.pendingOff)
        assertFalse(off.requested)
        assertEquals(now + Pacing.LOOSEN_DELAY, off.offAt)
        // pedir de novo não adia o prazo
        assertEquals(off, Focus.request(off, enable = false, now = now + 5 * 3_600_000L))
        assertEquals(off, Focus.applyDue(off, now + day - 1))
        assertEquals(FocusSettings(enabled = false), Focus.applyDue(off, now + day))
        // ligar de novo vale na hora e cancela o desligamento
        assertEquals(FocusSettings(enabled = true), Focus.request(off, enable = true, now = now))
    }

    // ---------- Codinome ----------

    @Test
    fun codenamesAreAnonymousAndVaried() {
        val codes = (1..40).map { Codenames.generate(Random(it)) }
        val pattern = Regex("""^\p{L}+ \p{L}+ \d{2,3}$""")
        listOf("pt", "en", "es").forEach { lang ->
            val names = codes.map { Codenames.full(it, lang) }
            names.forEach { assertTrue("$lang: $it", pattern.matches(it)) }
            assertTrue(names.toSet().size > 30)
        }
        // concordância: "Coruja Serena", nunca "Coruja Sereno"; "Lechuza Serena" em espanhol
        val femininePt = setOf("Raposa", "Coruja", "Águia", "Montanha", "Onça", "Estrela", "Brisa", "Aurora", "Garça", "Baleia", "Serra", "Lua", "Ariranha", "Sumaúma", "Maré", "Chuva")
        val feminineEs = setOf("Lechuza", "Águila", "Montaña", "Estrella", "Brisa", "Aurora", "Garza", "Ballena", "Sierra", "Luna", "Nutria", "Ceiba", "Marea", "Lluvia")
        (1..300).map { Codenames.generate(Random(it)) }.forEach { c ->
            val (noun, adj) = Codenames.full(c, "pt").split(" ")
            if (noun in femininePt) assertFalse("$noun $adj", adj.endsWith("o") && adj != "Livre")
            val (nounEs, adjEs) = Codenames.full(c, "es").split(" ")
            if (nounEs in feminineEs) assertFalse("$nounEs $adjEs", adjEs.endsWith("o") && adjEs != "Libre")
        }
        // o mesmo codinome em cada idioma: "Lobo Sereno 42" / "Serene Wolf 42"
        val wolf = Codename(noun = 0, adjective = 0, number = 42, seed = 1L)
        assertEquals("Lobo Sereno 42", Codenames.full(wolf, "pt"))
        assertEquals("Serene Wolf 42", Codenames.full(wolf, "en"))
        assertEquals("Lobo Sereno 42", Codenames.full(wolf, "es"))
        assertEquals("Wolf", Codenames.short(wolf, "en"))
    }
}
