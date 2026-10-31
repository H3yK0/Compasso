package app.compasso.model

import kotlin.math.PI
import kotlin.math.cos

/**
 * Respiração guiada das pausas: inspira em 4 s e solta o ar em 6 s (6 respirações por minuto,
 * com a expiração mais longa, que é a parte que acalma).
 */
object Breath {
    const val INHALE_MS = 4_000L
    const val EXHALE_MS = 6_000L
    const val CYCLE_MS = INHALE_MS + EXHALE_MS

    fun isInhale(elapsedMs: Long): Boolean = Math.floorMod(elapsedMs, CYCLE_MS) < INHALE_MS

    /** 0 = pulmão vazio, 1 = cheio. Curva suave nas pontas, como uma respiração de verdade. */
    fun level(elapsedMs: Long): Float {
        val t = Math.floorMod(elapsedMs, CYCLE_MS)
        return if (t < INHALE_MS) {
            ease(t.toFloat() / INHALE_MS)
        } else {
            1f - ease((t - INHALE_MS).toFloat() / EXHALE_MS)
        }
    }

    /** Número que muda a cada troca entre inspirar e soltar (para vibrar na troca). */
    fun step(elapsedMs: Long): Long = Math.floorDiv(elapsedMs, CYCLE_MS) * 2 + if (isInhale(elapsedMs)) 0 else 1

    private fun ease(x: Float): Float = ((1.0 - cos(PI * x.coerceIn(0f, 1f))) / 2.0).toFloat()
}
