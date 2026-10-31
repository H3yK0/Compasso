package app.compasso.model

import kotlin.math.roundToInt

enum class ReleaseMode { ALWAYS, CHANCE, NEVER }

/**
 * Um nível de treino: quantas rodadas de ritmo, quanto tempo cada rodada e cada pausa
 * duram (sorteado dentro da faixa), a velocidade (batidas por minuto, subindo do início
 * ao fim) e se a sessão libera no final.
 */
data class Level(
    val id: String,
    val name: String,
    val tagline: String,
    val difficulty: Int,
    val rounds: Int,
    val goMinSec: Int,
    val goMaxSec: Int,
    val stopMinSec: Int,
    val stopMaxSec: Int,
    val bpmStart: Int,
    val bpmEnd: Int,
    val release: ReleaseMode,
    val releaseChance: Int = 50,
) {
    val isCustom: Boolean get() = id == CUSTOM_ID

    /** Duração estimada em segundos, sem contar pausas extras do "Estou perto". */
    val estimatedSeconds: Int
        get() {
            val go = (goMinSec + goMaxSec) / 2.0
            val stop = (stopMinSec + stopMaxSec) / 2.0
            return (rounds * go + (rounds - 1).coerceAtLeast(0) * stop).roundToInt()
        }

    val estimatedMinutes: Int get() = ((estimatedSeconds + 30) / 60).coerceAtLeast(1)

    val releaseLabel: String
        get() = when (release) {
            ReleaseMode.ALWAYS -> "Libera no fim"
            ReleaseMode.NEVER -> "Sem liberação"
            ReleaseMode.CHANCE -> "Sorteio $releaseChance%"
        }

    /** Garante valores coerentes (mínimo ≤ máximo, faixas razoáveis). */
    fun sanitized(): Level {
        val gMin = goMinSec.coerceIn(5, 600)
        val sMin = stopMinSec.coerceIn(3, 300)
        return copy(
            rounds = rounds.coerceIn(1, 30),
            goMinSec = gMin,
            goMaxSec = goMaxSec.coerceIn(gMin, 600),
            stopMinSec = sMin,
            stopMaxSec = stopMaxSec.coerceIn(sMin, 300),
            bpmStart = bpmStart.coerceIn(30, 220),
            bpmEnd = bpmEnd.coerceIn(30, 220),
            releaseChance = releaseChance.coerceIn(0, 100),
        )
    }

    companion object {
        const val CUSTOM_ID = "custom"
    }
}

object Levels {
    val EASY = Level(
        id = "facil",
        name = "Fácil",
        tagline = "Poucas pausas e liberação rápida. Bom pra pegar o jeito.",
        difficulty = 1,
        rounds = 4,
        goMinSec = 25, goMaxSec = 40,
        stopMinSec = 10, stopMaxSec = 15,
        bpmStart = 70, bpmEnd = 120,
        release = ReleaseMode.ALWAYS,
    )

    val MEDIUM = Level(
        id = "medio",
        name = "Médio",
        tagline = "Mais rodadas e ritmo subindo. Demora, mas libera.",
        difficulty = 2,
        rounds = 7,
        goMinSec = 35, goMaxSec = 60,
        stopMinSec = 15, stopMaxSec = 25,
        bpmStart = 65, bpmEnd = 130,
        release = ReleaseMode.ALWAYS,
    )

    val HARD = Level(
        id = "dificil",
        name = "Difícil",
        tagline = "Sessão longa com ritmo forte no fim. Libera só no final.",
        difficulty = 3,
        rounds = 10,
        goMinSec = 45, goMaxSec = 90,
        stopMinSec = 20, stopMaxSec = 35,
        bpmStart = 60, bpmEnd = 150,
        release = ReleaseMode.ALWAYS,
    )

    val IMPOSSIBLE = Level(
        id = "impossivel",
        name = "Impossível",
        tagline = "Ritmo alto e pausas curtas. Aqui não tem liberação.",
        difficulty = 4,
        rounds = 12,
        goMinSec = 60, goMaxSec = 100,
        stopMinSec = 15, stopMaxSec = 25,
        bpmStart = 80, bpmEnd = 170,
        release = ReleaseMode.NEVER,
    )

    val presets: List<Level> = listOf(EASY, MEDIUM, HARD, IMPOSSIBLE)

    val DEFAULT_CUSTOM = Level(
        id = Level.CUSTOM_ID,
        name = "Personalizado",
        tagline = "Você decide rodadas, tempos, velocidade e se libera.",
        difficulty = 0,
        rounds = 6,
        goMinSec = 30, goMaxSec = 60,
        stopMinSec = 15, stopMaxSec = 30,
        bpmStart = 70, bpmEnd = 140,
        release = ReleaseMode.CHANCE,
        releaseChance = 50,
    )
}

fun speedLabel(bpm: Int): String = when {
    bpm < 75 -> "Lento"
    bpm < 105 -> "Moderado"
    bpm < 140 -> "Rápido"
    else -> "Muito rápido"
}
