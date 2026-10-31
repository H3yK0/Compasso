package app.compasso.model

/**
 * Modo foco: durante a sessão o app pede para fixar a tela (recurso do Android), esconde janelas
 * flutuantes, pausa quando perde o foco e, se a pessoa sair do app por mais de [Focus.GRACE_MS]
 * (ou sair [Focus.MAX_EXITS] vezes), encerra a sessão como interrompida.
 *
 * Desligar segue a mesma regra do controle inteligente: só vale 24 h depois.
 */
data class FocusSettings(val enabled: Boolean = true, val offAt: Long = 0L) {
    val pendingOff: Boolean get() = enabled && offAt > 0L

    /** O que a tela de ajustes mostra (a escolha mais recente). */
    val requested: Boolean get() = enabled && offAt == 0L
}

object Focus {
    /** Tempo fora do app tolerado (ex.: uma notificação aberta sem querer). */
    const val GRACE_MS = 10_000L

    /** Na terceira saída a sessão é encerrada. */
    const val MAX_EXITS = 3

    fun request(current: FocusSettings, enable: Boolean, now: Long): FocusSettings = when {
        enable -> FocusSettings(enabled = true, offAt = 0L)
        !current.enabled -> current
        current.pendingOff -> current
        else -> current.copy(offAt = now + Pacing.LOOSEN_DELAY)
    }

    fun applyDue(s: FocusSettings, now: Long): FocusSettings =
        if (s.pendingOff && now >= s.offAt) FocusSettings(enabled = false, offAt = 0L) else s
}
