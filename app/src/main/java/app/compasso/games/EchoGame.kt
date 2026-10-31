package app.compasso.games

import kotlin.random.Random

enum class EchoStep { CORRECT, ROUND_DONE, WRONG }

/**
 * Eco: o app toca uma sequência nos 4 botões; você repete. A cada rodada a sequência
 * ganha mais um passo. Um erro encerra a partida.
 */
class EchoGame(private val random: Random = Random.Default) {
    private val sequence = ArrayList<Int>()
    private var position = 0

    val round: Int get() = sequence.size
    val steps: List<Int> get() = sequence

    /** Pontuação: rodadas completas. */
    val score: Int get() = (sequence.size - 1).coerceAtLeast(0)

    fun nextRound(): List<Int> {
        sequence += random.nextInt(4)
        position = 0
        return sequence.toList()
    }

    fun press(pad: Int): EchoStep {
        if (sequence.isEmpty() || pad != sequence[position]) return EchoStep.WRONG
        position++
        return if (position == sequence.size) EchoStep.ROUND_DONE else EchoStep.CORRECT
    }

    /** Velocidade de exibição: acelera aos poucos. */
    fun stepMs(): Long = (560L - round * 18L).coerceAtLeast(260L)
}
