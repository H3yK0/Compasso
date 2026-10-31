package app.compasso.model

/** Como a sessão terminou. */
enum class Outcome {
    RELEASED,
    DENIED,
    LOST_CONTROL,
    STOPPED,

    /** Saiu do app no meio da sessão com o modo foco ligado. Não vale pontos. */
    INTERRUPTED,
}

/**
 * Conta como liberação para o intervalo do controle inteligente: liberado, "não segurei" e
 * sessão interrompida (o app não sabe o que aconteceu fora dele).
 */
val Outcome.isRelease: Boolean
    get() = this == Outcome.RELEASED || this == Outcome.LOST_CONTROL || this == Outcome.INTERRUPTED

/**
 * Registro mínimo de um "não segurei" ou de uma sessão interrompida: só o momento e o tipo.
 * Fica guardado mesmo com o histórico desligado ou apagado, porque conta para a sequência e
 * para o nível Invicto (senão bastaria desligar o histórico para apagar os tropeços).
 */
data class Lapse(val at: Long, val outcome: Outcome) {
    companion object {
        fun from(record: SessionRecord): Lapse? =
            if (record.outcome == Outcome.LOST_CONTROL || record.outcome == Outcome.INTERRUPTED) Lapse(record.startedAt, record.outcome) else null
    }
}

data class SessionRecord(
    val startedAt: Long,
    val endedAt: Long,
    val levelId: String,
    val levelName: String,
    val durationMs: Long,
    val roundsDone: Int,
    val roundsTotal: Int,
    val closeCount: Int,
    val maxBpm: Int,
    val outcome: Outcome,
    /** A liberação estava fechada pelo controle inteligente: a sessão foi só de treino. */
    val controlOnly: Boolean = false,
    /** A sessão inteira rodou em modo foco, com a tela fixada (só assim vale pontos). */
    val focused: Boolean = false,
)
