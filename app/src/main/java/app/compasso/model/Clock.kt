package app.compasso.model

/**
 * Relógio que não volta para trás. Atrasar a hora do celular não reabre a liberação, não
 * devolve pontos do dia nem encurta a espera da senha: o app guarda o último horário que viu
 * e, a partir dele, soma só o tempo que de fato passou (medido pelo relógio do sistema que
 * conta desde que o celular ligou, que a pessoa não consegue mudar).
 *
 * Adiantar a hora não dá para detectar sem internet, mas também não compensa: depois disso,
 * o app continua contando a partir do horário adiantado, mesmo que a hora volte ao normal.
 */
object TrustedTime {
    /** Último horário confiável visto e o tempo desde que o celular ligou naquele momento. */
    data class Mark(val trusted: Long, val uptime: Long)

    fun next(last: Mark?, wall: Long, uptime: Long): Mark {
        if (last == null) return Mark(wall, uptime)
        // Se o tempo desde que ligou diminuiu, o celular reiniciou: conta só o tempo desde então
        // (nunca mais do que passou de verdade).
        val passed = if (uptime >= last.uptime) uptime - last.uptime else uptime
        return Mark(maxOf(wall, last.trusted + passed), uptime)
    }
}

/**
 * Espera depois de errar a senha. As tentativas ficam gravadas: fechar e abrir o app não zera.
 * Da 5ª tentativa errada em diante, cada erro pede uma pausa que cresce aos poucos.
 */
object PinPolicy {
    const val FREE_ATTEMPTS = 5
    const val MAX_WAIT_MS = 15 * 60_000L

    fun waitAfter(failures: Int): Long {
        if (failures < FREE_ATTEMPTS) return 0L
        val doublings = ((failures - FREE_ATTEMPTS) / FREE_ATTEMPTS).coerceAtMost(10)
        return (30_000L shl doublings).coerceAtMost(MAX_WAIT_MS)
    }
}
