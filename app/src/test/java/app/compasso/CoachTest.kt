package app.compasso

import app.compasso.model.ArticleInfo
import app.compasso.model.ArticleParser
import app.compasso.model.Coach
import app.compasso.model.Lapse
import app.compasso.model.LevelReason
import app.compasso.model.Levels
import app.compasso.model.Outcome
import app.compasso.model.PointEntry
import app.compasso.model.PointKind
import app.compasso.model.SessionRecord
import app.compasso.model.Signal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Regras do algoritmo de recomendação (sinais, leitura e nível), com históricos sintéticos. */
class CoachTest {
    private val zone = ZoneOffset.UTC
    private val day = 24 * 3_600_000L

    /** Quarta-feira, 15 h (longe da faixa da noite). */
    private val now = LocalDateTime.of(2026, 3, 11, 15, 0).toInstant(zone).toEpochMilli()

    private fun at(daysAgo: Int, hour: Int = 15): Long =
        LocalDateTime.of(2026, 3, 11, hour, 0).minusDays(daysAgo.toLong()).toInstant(zone).toEpochMilli()

    private fun session(
        start: Long,
        outcome: Outcome = Outcome.RELEASED,
        level: String = Levels.EASY.id,
        rounds: Int = 4,
        done: Int = rounds,
        close: Int = 0,
    ) = SessionRecord(
        startedAt = start,
        endedAt = start + 600_000,
        levelId = level,
        levelName = level,
        durationMs = 600_000,
        roundsDone = done,
        roundsTotal = rounds,
        closeCount = close,
        maxBpm = 120,
        outcome = outcome,
    )

    private fun point(at: Long) = PointEntry(at, PointKind.CHECKIN, 10)

    private fun signals(
        history: List<SessionRecord> = emptyList(),
        lapses: List<Lapse> = history.mapNotNull { Lapse.from(it) },
        points: List<PointEntry> = emptyList(),
    ) = Coach.signals(history, lapses, points, now, zone)

    /** Os artigos de verdade (pt): as tags são iguais nos três idiomas. */
    private val articles: List<ArticleInfo> = File("src/main/assets/artigos/pt")
        .listFiles { f -> f.name.endsWith(".md") }.orEmpty()
        .map { ArticleParser.parse(it.readText()) }
        .map { ArticleInfo(it.id, it.order, it.tags) }

    @Test
    fun newUserStartsEasyAndReadsTheBasics() {
        val s = signals()
        assertEquals(listOf(Signal.NEW), s)
        val advice = Coach.reading(s, articles, read = emptySet())
        assertNotNull(advice)
        assertEquals(Signal.NEW, advice!!.reason)
        val tags = articles.first { it.id == advice.articleId }.tags
        assertTrue(tags.any { it in Coach.TOPICS.getValue(Signal.NEW) })
        assertEquals(LevelReason.FIRST, Coach.level(emptyList(), emptyList(), now).reason)
        assertEquals(Levels.EASY.id, Coach.level(emptyList(), emptyList(), now).levelId)
    }

    @Test
    fun recentLapseComesFirstAndPointsToRelapseReading() {
        val history = listOf(
            session(at(10)),
            session(at(2), Outcome.LOST_CONTROL, done = 3),
        )
        val s = signals(history)
        assertEquals(Signal.RECENT_LAPSE, s.first())
        val advice = Coach.reading(s, articles, read = emptySet())!!
        assertEquals(Signal.RECENT_LAPSE, advice.reason)
        assertEquals("recaida", advice.articleId) // o primeiro, pela ordem, com essa tag
        // já leu: vai para a próxima sobre o mesmo assunto
        val next = Coach.reading(s, articles, read = setOf("recaida"))!!
        assertEquals(Signal.RECENT_LAPSE, next.reason)
        assertTrue(articles.first { it.id == next.articleId }.tags.any { it in Coach.TOPICS.getValue(Signal.RECENT_LAPSE) })
    }

    @Test
    fun oldLapseIsNotRecent() {
        val s = signals(listOf(session(at(9), Outcome.LOST_CONTROL, done = 4)))
        assertFalse(Signal.RECENT_LAPSE in s)
    }

    @Test
    fun earlyLossOrManyClosesMeanFastLoss() {
        assertTrue(Signal.FAST_LOSS in signals(listOf(session(at(12), Outcome.LOST_CONTROL, rounds = 6, done = 1))))
        val closes = listOf(session(at(3), close = 4), session(at(2), close = 3))
        assertTrue(Signal.FAST_LOSS in signals(closes))
        assertFalse(Signal.FAST_LOSS in signals(listOf(session(at(3), close = 1), session(at(2), close = 0))))
        // perdeu já perto do fim: não é "rápido"
        assertFalse(Signal.FAST_LOSS in signals(listOf(session(at(12), Outcome.LOST_CONTROL, rounds = 6, done = 5))))
    }

    @Test
    fun interruptedSessionWithinTwoWeeks() {
        assertTrue(Signal.INTERRUPTED in signals(listOf(session(at(10), Outcome.INTERRUPTED, done = 2))))
        assertFalse(Signal.INTERRUPTED in signals(listOf(session(at(20), Outcome.INTERRUPTED, done = 2))))
    }

    @Test
    fun mostlyLateNightMeansNight() {
        val nights = listOf(session(at(6, hour = 23)), session(at(4, hour = 1)), session(at(2, hour = 14)))
        val s = signals(nights)
        assertTrue(Signal.NIGHT in s)
        val advice = Coach.reading(listOf(Signal.NIGHT), articles, read = emptySet())!!
        assertTrue(articles.first { it.id == advice.articleId }.tags.any { it in setOf("noite", "sono", "alcool") })
        // poucas ocorrências não bastam
        assertFalse(Signal.NIGHT in signals(listOf(session(at(3, hour = 23)), session(at(2, hour = 23)))))
        // maioria de dia
        assertFalse(Signal.NIGHT in signals(listOf(session(at(6, hour = 23)), session(at(4, hour = 10)), session(at(2, hour = 14)))))
    }

    @Test
    fun nightIsFrom22To4() {
        assertTrue(Coach.isNight(at(0, hour = 22), zone))
        assertTrue(Coach.isNight(at(0, hour = 3), zone))
        assertFalse(Coach.isNight(at(0, hour = 4), zone))
        assertFalse(Coach.isNight(at(0, hour = 21), zone))
    }

    @Test
    fun fullWeekMeansBusy() {
        val week = (1..5).map { session(at(it)) }
        assertTrue(Signal.BUSY in signals(week))
        assertFalse(Signal.BUSY in signals(week.drop(1)))
    }

    @Test
    fun fewActiveDaysMeanLowConsistency() {
        val points = listOf(point(at(12)), point(at(5)))
        assertTrue(Signal.LOW_CONSISTENCY in signals(points = points))
        // começou há pouco: ainda não dá para dizer
        assertFalse(Signal.LOW_CONSISTENCY in signals(points = listOf(point(at(3)), point(at(1)))))
        // constante: não
        assertFalse(Signal.LOW_CONSISTENCY in signals(points = (0..13).map { point(at(it)) }))
    }

    @Test
    fun weekStreakMeansGoodStreak() {
        val points = (0..7).map { point(at(it)) }
        val s = signals(points = points)
        assertTrue(Signal.GOOD_STREAK in s)
        assertFalse(Signal.LOW_CONSISTENCY in s)
        assertFalse(Signal.GOOD_STREAK in signals(points = (0..4).map { point(at(it)) }))
    }

    @Test
    fun readingSkipsReadAndDismissedAndEndsWhenAllRead() {
        val pool = listOf(
            ArticleInfo("a", 1, setOf("recaida")),
            ArticleInfo("b", 2, setOf("noite")),
            ArticleInfo("c", 3, setOf("ciencia")),
        )
        assertEquals("a", Coach.reading(listOf(Signal.RECENT_LAPSE), pool, read = emptySet())!!.articleId)
        assertEquals("b", Coach.reading(listOf(Signal.RECENT_LAPSE, Signal.NIGHT), pool, read = setOf("a"))!!.articleId)
        // dispensado há pouco: pula, mas volta se for o único que sobrou
        assertEquals("b", Coach.reading(listOf(Signal.RECENT_LAPSE, Signal.NIGHT), pool, read = emptySet(), skip = setOf("a"))!!.articleId)
        assertEquals("c", Coach.reading(emptyList(), pool, read = setOf("a", "b"), skip = setOf("c"))!!.articleId)
        // nada combina: próximo pela ordem, motivo geral
        val general = Coach.reading(listOf(Signal.BUSY), pool, read = emptySet())!!
        assertEquals("a", general.articleId)
        assertEquals(Signal.GENERAL, general.reason)
        assertNull(Coach.reading(listOf(Signal.NEW), pool, read = setOf("a", "b", "c")))
    }

    @Test
    fun higherPrioritySignalWins() {
        val pool = listOf(
            ArticleInfo("noite", 1, setOf("noite")),
            ArticleInfo("recaida", 2, setOf("recaida")),
        )
        val advice = Coach.reading(listOf(Signal.RECENT_LAPSE, Signal.NIGHT), pool, read = emptySet())!!
        assertEquals("recaida", advice.articleId)
        assertEquals(Signal.RECENT_LAPSE, advice.reason)
    }

    @Test
    fun twoLossesStepDownOrStayEasy() {
        val medium = listOf(
            session(at(5), level = Levels.MEDIUM.id, rounds = 7),
            session(at(3), Outcome.LOST_CONTROL, level = Levels.MEDIUM.id, rounds = 7, done = 4),
            session(at(1), Outcome.LOST_CONTROL, level = Levels.MEDIUM.id, rounds = 7, done = 5),
        )
        val down = Coach.level(medium, medium.mapNotNull { Lapse.from(it) }, now)
        assertEquals(Levels.EASY.id, down.levelId)
        assertEquals(LevelReason.STEP_DOWN, down.reason)

        val easy = listOf(
            session(at(3), Outcome.LOST_CONTROL, done = 2),
            session(at(1), Outcome.LOST_CONTROL, done = 3),
        )
        val stay = Coach.level(easy, easy.mapNotNull { Lapse.from(it) }, now)
        assertEquals(Levels.EASY.id, stay.levelId)
        assertEquals(LevelReason.STAY_EASY, stay.reason)
    }

    @Test
    fun threeSolidSessionsStepUpUnlessThereWasALapse() {
        val solid = listOf(session(at(5)), session(at(3), close = 1), session(at(1), Outcome.DENIED))
        val up = Coach.level(solid, emptyList(), now)
        assertEquals(Levels.MEDIUM.id, up.levelId)
        assertEquals(LevelReason.STEP_UP, up.reason)

        val withLapse = Coach.level(solid, listOf(Lapse(at(2), Outcome.LOST_CONTROL)), now)
        assertEquals(Levels.EASY.id, withLapse.levelId)
        assertEquals(LevelReason.KEEP, withLapse.reason)

        val shaky = listOf(session(at(5)), session(at(3), close = 2), session(at(1)))
        assertEquals(LevelReason.KEEP, Coach.level(shaky, emptyList(), now).reason)

        val top = (1..3).map { session(at(it), Outcome.DENIED, level = Levels.IMPOSSIBLE.id, rounds = 12) }
        val keepTop = Coach.level(top, emptyList(), now)
        assertEquals(Levels.IMPOSSIBLE.id, keepTop.levelId)
        assertEquals(LevelReason.TOP, keepTop.reason)
    }

    @Test
    fun customLevelIsKept() {
        val custom = listOf(session(at(1), level = "custom"))
        val advice = Coach.level(custom, emptyList(), now)
        assertEquals("custom", advice.levelId)
        assertEquals(LevelReason.KEEP, advice.reason)
    }

    @Test
    fun futureEntriesAreIgnored() {
        // relógio do aparelho adiantado não pode inventar sinais
        val ahead = listOf(session(now + 3 * day, Outcome.LOST_CONTROL, done = 1))
        val s = Coach.signals(ahead, ahead.mapNotNull { Lapse.from(it) }, emptyList(), now, zone)
        assertFalse(Signal.RECENT_LAPSE in s)
        assertEquals(LevelReason.FIRST, Coach.level(ahead, emptyList(), now).reason)
    }

    /** Cada sinal tem pelo menos um artigo que fala dele. */
    @Test
    fun everySignalHasArticles() {
        Coach.TOPICS.forEach { (signal, topics) ->
            assertTrue("$signal sem artigo", articles.count { a -> a.tags.any { it in topics } } >= 2)
        }
    }
}
