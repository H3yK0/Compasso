package app.compasso.model

/** O que o controle inteligente guarda de cada sessão: só quando foi e se houve liberação. */
data class PacingEntry(
    val startedAt: Long,
    val endedAt: Long,
    val released: Boolean,
    /** Sessões encerradas logo no começo não contam para "sem pressa" nem para "semana cheia". */
    val counts: Boolean,
) {
    companion object {
        fun from(record: SessionRecord) = PacingEntry(
            startedAt = record.startedAt,
            endedAt = record.endedAt,
            released = record.outcome.isRelease,
            counts = record.outcome != Outcome.STOPPED || record.durationMs >= Pacing.MIN_COUNTED_MS,
        )
    }
}

data class PacingConfig(val enabled: Boolean = true, val intervalHours: Int = 24)

/** Mudança que deixa o controle mais leve: só vale a partir de [effectiveAt]. */
data class PendingChange(val target: PacingConfig, val effectiveAt: Long)

data class PacingSettings(val config: PacingConfig = PacingConfig(), val pending: PendingChange? = null) {
    /** O que a pessoa escolheu por último (é o que a tela de ajustes mostra). */
    val requested: PacingConfig get() = pending?.target ?: config
}

enum class GateRule {
    /** Ainda não passou o intervalo desde a última liberação. */
    INTERVAL,

    /** A sessão anterior terminou há menos de 2 h. */
    RUSH,
}

sealed interface ReleaseGate {
    data object Off : ReleaseGate

    data class Open(val intervalHours: Int, val busyWeek: Boolean) : ReleaseGate

    data class Closed(
        val until: Long,
        val rules: Set<GateRule>,
        val intervalHours: Int,
        val busyWeek: Boolean,
    ) : ReleaseGate
}

/** Retrato do momento, para a tela de detalhes. */
data class PacingSnapshot(
    val gate: ReleaseGate,
    val lastRelease: Long?,
    val sessionsThisWeek: Int,
)

/**
 * Controle inteligente da liberação. Três regras fixas e visíveis, sem sorteio:
 *
 * 1. Intervalo: depois de uma liberação (ou de um "não segurei"), a próxima só vem depois
 *    do intervalo escolhido (12, 24, 48 ou 72 h).
 * 2. Sem pressa: sessão começada menos de 2 h depois do fim da anterior é só de controle.
 * 3. Semana cheia: com 5 sessões ou mais nos últimos 7 dias, o intervalo dobra.
 *
 * Quando a liberação está fechada, a sessão não é cancelada: vira treino de controle.
 */
object Pacing {
    const val HOUR = 3_600_000L
    const val DAY = 24 * HOUR
    const val WEEK = 7 * DAY
    const val RUSH_GAP = 2 * HOUR
    const val BUSY_WEEK_SESSIONS = 5
    const val MIN_COUNTED_MS = 2 * 60_000L
    const val LOOSEN_DELAY = DAY

    /** Quanto o registro precisa guardar: a semana e o maior intervalo possível (72 h × 2). */
    const val KEEP = 8 * DAY
    val INTERVAL_OPTIONS = listOf(12, 24, 48, 72)

    fun evaluate(config: PacingConfig, log: List<PacingEntry>, now: Long): ReleaseGate {
        if (!config.enabled) return ReleaseGate.Off
        val past = log.filter { it.startedAt <= now }
        val busy = isBusy(past, now)
        val hours = effectiveHours(config, busy)
        val rules = rulesAt(config, past, now)
        if (rules.isEmpty()) return ReleaseGate.Open(hours, busy)

        // Primeiro momento em que nenhuma regra segura mais. Só muda quando um prazo vence
        // ou quando uma sessão antiga sai da janela de 7 dias, então basta testar esses momentos.
        val candidates = buildList {
            lastEnd(past)?.let { add(it + RUSH_GAP) }
            lastRelease(past)?.let {
                add(it + config.intervalHours * HOUR)
                add(it + 2 * config.intervalHours * HOUR)
            }
            past.filter { it.counts }.forEach { add(it.startedAt + WEEK) }
        }.filter { it > now }.distinct().sorted()
        val until = candidates.firstOrNull { rulesAt(config, past, it).isEmpty() } ?: candidates.lastOrNull() ?: now
        return ReleaseGate.Closed(until, rules, hours, busy)
    }

    fun snapshot(config: PacingConfig, log: List<PacingEntry>, now: Long): PacingSnapshot {
        val past = log.filter { it.startedAt <= now }
        return PacingSnapshot(
            gate = evaluate(config, log, now),
            lastRelease = lastRelease(past),
            sessionsThisWeek = past.count { it.counts && it.startedAt > now - WEEK },
        )
    }

    private fun rulesAt(config: PacingConfig, past: List<PacingEntry>, t: Long): Set<GateRule> {
        val rules = mutableSetOf<GateRule>()
        val interval = effectiveHours(config, isBusy(past, t)) * HOUR
        lastRelease(past)?.let { if (t < it + interval) rules += GateRule.INTERVAL }
        lastEnd(past)?.let { if (t < it + RUSH_GAP) rules += GateRule.RUSH }
        return rules
    }

    private fun effectiveHours(config: PacingConfig, busy: Boolean) = config.intervalHours * if (busy) 2 else 1

    private fun isBusy(past: List<PacingEntry>, t: Long) =
        past.count { it.counts && it.startedAt > t - WEEK } >= BUSY_WEEK_SESSIONS

    private fun lastRelease(past: List<PacingEntry>) = past.filter { it.released }.maxOfOrNull { it.endedAt }

    private fun lastEnd(past: List<PacingEntry>) = past.filter { it.counts }.maxOfOrNull { it.endedAt }

    // ---------- Mudanças nos ajustes ----------

    fun isLooser(target: PacingConfig, current: PacingConfig): Boolean =
        current.enabled && (!target.enabled || target.intervalHours < current.intervalHours)

    /**
     * Deixar mais firme vale na hora. Deixar mais leve (intervalo menor ou desligar) só vale 24 h
     * depois, para uma vontade do momento não desfazer o que foi decidido com calma.
     */
    fun request(current: PacingSettings, target: PacingConfig, now: Long): PacingSettings {
        val cur = current.config
        return when {
            target == cur -> current.copy(pending = null)
            isLooser(target, cur) -> {
                val keep = current.pending?.takeIf { it.target == target }?.effectiveAt
                current.copy(pending = PendingChange(target, keep ?: (now + LOOSEN_DELAY)))
            }
            else -> PacingSettings(target, null)
        }
    }

    fun applyDue(settings: PacingSettings, now: Long): PacingSettings {
        val pending = settings.pending ?: return settings
        return if (now >= pending.effectiveAt) PacingSettings(pending.target, null) else settings
    }

    fun prune(log: List<PacingEntry>, now: Long): List<PacingEntry> = log.filter { it.endedAt > now - KEEP }
}
