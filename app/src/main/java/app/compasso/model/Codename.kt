package app.compasso.model

import kotlin.random.Random

/** Identidade anônima criada pelo próprio app: nome aleatório + semente do avatar. */
data class Codename(val name: String, val seed: Long)

object Codenames {
    private data class Noun(val word: String, val feminine: Boolean)

    private val nouns = listOf(
        Noun("Lobo", false), Noun("Cedro", false), Noun("Falcão", false), Noun("Rio", false),
        Noun("Vento", false), Noun("Carvalho", false), Noun("Lince", false), Noun("Farol", false),
        Noun("Monte", false), Noun("Urso", false), Noun("Corvo", false), Noun("Cometa", false),
        Noun("Oceano", false), Noun("Jequitibá", false), Noun("Tucano", false), Noun("Ipê", false),
        Noun("Raposa", true), Noun("Coruja", true), Noun("Águia", true), Noun("Montanha", true),
        Noun("Onça", true), Noun("Estrela", true), Noun("Brisa", true), Noun("Aurora", true),
        Noun("Garça", true), Noun("Baleia", true), Noun("Serra", true), Noun("Lua", true),
        Noun("Ariranha", true), Noun("Sumaúma", true), Noun("Maré", true), Noun("Chuva", true),
    )

    private val adjectives = listOf(
        "Sereno" to "Serena", "Firme" to "Firme", "Sábio" to "Sábia", "Calmo" to "Calma",
        "Valente" to "Valente", "Atento" to "Atenta", "Livre" to "Livre", "Forte" to "Forte",
        "Leal" to "Leal", "Desperto" to "Desperta", "Constante" to "Constante", "Paciente" to "Paciente",
        "Lúcido" to "Lúcida", "Tranquilo" to "Tranquila", "Bravo" to "Brava", "Silencioso" to "Silenciosa",
        "Focado" to "Focada", "Gentil" to "Gentil", "Íntegro" to "Íntegra", "Resiliente" to "Resiliente",
    )

    fun generate(random: Random = Random.Default): Codename {
        val noun = nouns.random(random)
        val (m, f) = adjectives.random(random)
        val number = random.nextInt(10, 1000)
        return Codename("${noun.word} ${if (noun.feminine) f else m} $number", random.nextLong())
    }
}
