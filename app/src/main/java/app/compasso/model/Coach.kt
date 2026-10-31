package app.compasso.model

import java.time.Instant
import java.time.ZoneId

/**
 * O que o histórico mostra sobre o momento da pessoa. A ordem é a prioridade: quando há mais de um
 * sinal, o primeiro decide o motivo da recomendação.
 */
enum class Signal {
    /** "Não aguentei" nos últimos 7 dias. */
    RECENT_LAPSE,

    /** "Não aguentei" logo no começo da sessão, ou chegando perto muitas vezes. */
    FAST_LOSS,

    /** Sessão interrompida (saiu do app) nas últimas 2 semanas. */
    INTERRUPTED,

    /** A maior parte das sessões e tropeços acontece tarde da noite. */
    NIGHT,

    /** Semana cheia: muitas sessões em 7 dias. */
    BUSY,

    /** Poucos dias com pontos nas últimas 2 semanas. */
    LOW_CONSISTENCY,

    /** Uma semana ou mais de constância seguida. */
    GOOD_STREAK,

    /** Ainda sem sessões. */
    NEW,

    /** Nenhum sinal forte: segue a ordem dos artigos. */
    GENERAL,
}

/** Leitura sugerida e por quê. */
data class ReadingAdvice(val articleId: String, val reason: Signal)

enum class LevelReason {
    /** Primeira sessão. */
    FIRST,

    /** Dois "não aguentei" nas três últimas: um nível abaixo. */
    STEP_DOWN,

    /** Já no Fácil e ainda difícil: ficar e usar "Estou perto" mais cedo. */
    STAY_EASY,

    /** Três concluídas seguidas, quase sem chegar perto: um nível acima. */
    STEP_UP,

    /** Já no Impossível e indo bem. */
    TOP,

    /** Continuar no mesmo nível. */
    KEEP,
}

/** Nível sugerido e por quê. */
data class LevelAdvice(val levelId: String, val reason: LevelReason)

/** O mínimo de um artigo que as recomendações precisam (vale para qualquer idioma). */
data class ArticleInfo(val id: String, val order: Int, val tags: Set<String>)

/**
 * Recomendações a partir do histórico, sem internet e com regras simples e explicáveis: a pessoa
 * sempre vê o motivo. Lê as sessões, os tropeços e os pontos, encontra os sinais do momento e
 * escolhe a leitura que mais combina com eles e o nível de treino mais adequado.
 */
object Coach {
    private const val DAY = 24 * 3_600_000L

    /** Assuntos que ajudam em cada sinal (as tags dos artigos usam este vocabulário). */
    val TOPICS: Map<Signal, Set<String>> = mapOf(
        Signal.RECENT_LAPSE to setOf("recaida", "autocompaixao", "vergonha"),
        Signal.FAST_LOSS to setOf("precoce", "ansiedade", "respiracao"),
        Signal.INTERRUPTED to setOf("foco", "gatilhos", "tedio"),
        Signal.NIGHT to setOf("noite", "sono", "alcool"),
        Signal.BUSY to setOf("pressa", "novidade", "dopamina", "futuro"),
        Signal.LOW_CONSISTENCY to setOf("habitos", "monitorar", "autoeficacia"),
        Signal.GOOD_STREAK to setOf("relacionamentos", "conversa", "futuro"),
        Signal.NEW to setOf("comecar", "vontade"),
    )

    /** Todas as tags conhecidas (o teste de conteúdo confere que os artigos usam só estas e algumas de assunto). */
    val VOCABULARY: Set<String> = TOPICS.values.flatten().toSet() +
        setOf("mente", "ciencia", "corpo", "ajuda", "solidao", "estresse")

    private val ORDER: List<Level> = Levels.presets

    fun signals(
        history: List<SessionRecord>,
        lapses: List<Lapse>,
        points: List<PointEntry>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Signal> {
        val out = mutableListOf<Signal>()
        // registros "no futuro" (relógio do aparelho mexido) não contam
        val past = history.filter { it.startedAt <= now }
        val pastLapses = lapses.filter { it.at <= now }
        val pastPoints = points.filter { it.at <= now }
        val recent = past.filter { it.startedAt > now - 28 * DAY }
        if (pastLapses.any { it.outcome == Outcome.LOST_CONTROL && it.at > now - 7 * DAY }) out += Signal.RECENT_LAPSE

        val last5 = past.sortedByDescending { it.startedAt }.take(5)
        val earlyLoss = last5.any { it.outcome == Outcome.LOST_CONTROL && it.roundsDone <= maxOf(1, it.roundsTotal / 3) }
        val completed = last5.filter { it.outcome == Outcome.RELEASED || it.outcome == Outcome.DENIED }
        val manyClose = completed.size >= 2 && completed.map { it.closeCount }.average() >= 3.0
        if (earlyLoss || manyClose) out += Signal.FAST_LOSS

        if (pastLapses.any { it.outcome == Outcome.INTERRUPTED && it.at > now - 14 * DAY }) out += Signal.INTERRUPTED

        // a sessão que virou lapso já está no histórico: conta o momento uma vez só
        val moments = (recent.map { it.startedAt } + pastLapses.filter { it.at > now - 28 * DAY }.map { it.at }).toSet()
        val nightly = moments.count { isNight(it, zone) }
        if (moments.size >= 3 && nightly * 2 >= moments.size) out += Signal.NIGHT

        if (past.count { it.startedAt > now - 7 * DAY } >= Pacing.BUSY_WEEK_SESSIONS) out += Signal.BUSY

        val activeDays = pastPoints.filter { it.at > now - 14 * DAY }.map { Progress.day(it.at, zone) }.toSet().size
        val firstUse = (past.map { it.startedAt } + pastPoints.map { it.at }).minOrNull()
        if (firstUse != null && firstUse < now - 7 * DAY && activeDays <= 4) out += Signal.LOW_CONSISTENCY

        if (Progress.streak(pastPoints, pastLapses, now, zone) >= 7) out += Signal.GOOD_STREAK
        if (past.isEmpty() && pastLapses.isEmpty()) out += Signal.NEW
        return out
    }

    /**
     * Leitura que mais combina com os sinais, entre as não lidas (e não dispensadas há pouco).
     * Sem nenhuma combinação, a próxima não lida pela ordem do app. Tudo lido: nada.
     */
    fun reading(
        signals: List<Signal>,
        articles: List<ArticleInfo>,
        read: Set<String>,
        skip: Set<String> = emptySet(),
    ): ReadingAdvice? {
        val pool = articles.filter { it.id !in read && it.id !in skip }.ifEmpty { articles.filter { it.id !in read } }
        if (pool.isEmpty()) return null
        var best: ArticleInfo? = null
        var bestScore = 0
        var bestReason = Signal.GENERAL
        for (a in pool.sortedBy { it.order }) {
            var score = 0
            var reason: Signal? = null
            signals.forEachIndexed { rank, s ->
                if (TOPICS[s].orEmpty().any { it in a.tags }) {
                    score += 10 * (signals.size - rank)
                    if (reason == null) reason = s
                }
            }
            if (score > bestScore) {
                best = a
                bestScore = score
                bestReason = reason ?: Signal.GENERAL
            }
        }
        val pick = best ?: pool.minBy { it.order }
        return ReadingAdvice(pick.id, if (best != null) bestReason else Signal.GENERAL)
    }

    /** Nível sugerido a partir das últimas sessões. */
    fun level(history: List<SessionRecord>, lapses: List<Lapse>, now: Long): LevelAdvice {
        val recent = history.filter { it.startedAt <= now }.sortedByDescending { it.startedAt }
        val last = recent.firstOrNull() ?: return LevelAdvice(Levels.EASY.id, LevelReason.FIRST)
        val idx = ORDER.indexOfFirst { it.id == last.levelId }
        if (idx < 0) return LevelAdvice(last.levelId, LevelReason.KEEP)
        val sameLevel = recent.filter { it.levelId == last.levelId }.take(3)
        val losses = sameLevel.count { it.outcome == Outcome.LOST_CONTROL }
        if (losses >= 2) {
            return if (idx == 0) LevelAdvice(Levels.EASY.id, LevelReason.STAY_EASY) else LevelAdvice(ORDER[idx - 1].id, LevelReason.STEP_DOWN)
        }
        val solid = sameLevel.size == 3 &&
            sameLevel.all { (it.outcome == Outcome.RELEASED || it.outcome == Outcome.DENIED) && it.closeCount <= 1 }
        val cleanWeek = lapses.none { it.at > now - 7 * DAY && it.at <= now }
        if (solid && cleanWeek) {
            return if (idx == ORDER.lastIndex) LevelAdvice(last.levelId, LevelReason.TOP) else LevelAdvice(ORDER[idx + 1].id, LevelReason.STEP_UP)
        }
        return LevelAdvice(last.levelId, LevelReason.KEEP)
    }

    /** Tarde da noite: das 22 h às 4 h. */
    fun isNight(at: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val h = Instant.ofEpochMilli(at).atZone(zone).hour
        return h >= 22 || h < 4
    }
}
