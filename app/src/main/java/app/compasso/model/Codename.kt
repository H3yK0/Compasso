package app.compasso.model

import kotlin.random.Random

/**
 * Identidade anônima criada pelo próprio app. Guarda só a posição das palavras nas listas,
 * o número e a semente do avatar: o nome é montado no idioma do app ("Lobo Sereno 42",
 * "Serene Wolf 42", "Lobo Sereno 42"), então trocar de idioma não muda quem você é.
 */
data class Codename(val noun: Int, val adjective: Int, val number: Int, val seed: Long)

object Codenames {
    private data class Noun(val word: String, val feminine: Boolean = false)

    private class Words(val nouns: List<Noun>, val adjectives: List<Pair<String, String>>, val adjectiveFirst: Boolean)

    // As três listas andam juntas: a mesma posição é a mesma ideia em cada idioma.
    private val PT = Words(
        nouns = listOf(
            Noun("Lobo"), Noun("Cedro"), Noun("Falcão"), Noun("Rio"),
            Noun("Vento"), Noun("Carvalho"), Noun("Lince"), Noun("Farol"),
            Noun("Monte"), Noun("Urso"), Noun("Corvo"), Noun("Cometa"),
            Noun("Oceano"), Noun("Jequitibá"), Noun("Tucano"), Noun("Ipê"),
            Noun("Raposa", true), Noun("Coruja", true), Noun("Águia", true), Noun("Montanha", true),
            Noun("Onça", true), Noun("Estrela", true), Noun("Brisa", true), Noun("Aurora", true),
            Noun("Garça", true), Noun("Baleia", true), Noun("Serra", true), Noun("Lua", true),
            Noun("Ariranha", true), Noun("Sumaúma", true), Noun("Maré", true), Noun("Chuva", true),
        ),
        adjectives = listOf(
            "Sereno" to "Serena", "Firme" to "Firme", "Sábio" to "Sábia", "Calmo" to "Calma",
            "Valente" to "Valente", "Atento" to "Atenta", "Livre" to "Livre", "Forte" to "Forte",
            "Leal" to "Leal", "Desperto" to "Desperta", "Constante" to "Constante", "Paciente" to "Paciente",
            "Lúcido" to "Lúcida", "Tranquilo" to "Tranquila", "Bravo" to "Brava", "Silencioso" to "Silenciosa",
            "Focado" to "Focada", "Gentil" to "Gentil", "Íntegro" to "Íntegra", "Resiliente" to "Resiliente",
        ),
        adjectiveFirst = false,
    )

    private val ES = Words(
        nouns = listOf(
            Noun("Lobo"), Noun("Cedro"), Noun("Halcón"), Noun("Río"),
            Noun("Viento"), Noun("Roble"), Noun("Lince"), Noun("Faro"),
            Noun("Monte"), Noun("Oso"), Noun("Cuervo"), Noun("Cometa"),
            Noun("Océano"), Noun("Pino"), Noun("Tucán"), Noun("Lapacho"),
            Noun("Zorro"), Noun("Lechuza", true), Noun("Águila", true), Noun("Montaña", true),
            Noun("Jaguar"), Noun("Estrella", true), Noun("Brisa", true), Noun("Aurora", true),
            Noun("Garza", true), Noun("Ballena", true), Noun("Sierra", true), Noun("Luna", true),
            Noun("Nutria", true), Noun("Ceiba", true), Noun("Marea", true), Noun("Lluvia", true),
        ),
        adjectives = listOf(
            "Sereno" to "Serena", "Firme" to "Firme", "Sabio" to "Sabia", "Calmo" to "Calma",
            "Valiente" to "Valiente", "Atento" to "Atenta", "Libre" to "Libre", "Fuerte" to "Fuerte",
            "Leal" to "Leal", "Despierto" to "Despierta", "Constante" to "Constante", "Paciente" to "Paciente",
            "Lúcido" to "Lúcida", "Tranquilo" to "Tranquila", "Bravo" to "Brava", "Silencioso" to "Silenciosa",
            "Enfocado" to "Enfocada", "Gentil" to "Gentil", "Íntegro" to "Íntegra", "Resiliente" to "Resiliente",
        ),
        adjectiveFirst = false,
    )

    private val EN = Words(
        nouns = listOf(
            Noun("Wolf"), Noun("Cedar"), Noun("Falcon"), Noun("River"),
            Noun("Wind"), Noun("Oak"), Noun("Lynx"), Noun("Lighthouse"),
            Noun("Hill"), Noun("Bear"), Noun("Raven"), Noun("Comet"),
            Noun("Ocean"), Noun("Sequoia"), Noun("Toucan"), Noun("Willow"),
            Noun("Fox"), Noun("Owl"), Noun("Eagle"), Noun("Mountain"),
            Noun("Jaguar"), Noun("Star"), Noun("Breeze"), Noun("Aurora"),
            Noun("Heron"), Noun("Whale"), Noun("Ridge"), Noun("Moon"),
            Noun("Otter"), Noun("Kapok"), Noun("Tide"), Noun("Rain"),
        ),
        adjectives = listOf(
            "Serene", "Steady", "Wise", "Calm",
            "Brave", "Watchful", "Free", "Strong",
            "Loyal", "Awake", "Constant", "Patient",
            "Lucid", "Tranquil", "Bold", "Silent",
            "Focused", "Gentle", "Upright", "Resilient",
        ).map { it to it },
        adjectiveFirst = true,
    )

    val nounCount: Int = PT.nouns.size
    val adjectiveCount: Int = PT.adjectives.size

    private fun words(language: String): Words = when (language) {
        "pt" -> PT
        "es" -> ES
        else -> EN
    }

    fun generate(random: Random = Random.Default): Codename = Codename(
        noun = random.nextInt(nounCount),
        adjective = random.nextInt(adjectiveCount),
        number = random.nextInt(10, 1000),
        seed = random.nextLong(),
    )

    /** Nome completo no idioma ("Lobo Sereno 42" / "Serene Wolf 42"). */
    fun full(c: Codename, language: String): String {
        val w = words(language)
        val noun = w.nouns[c.noun.mod(w.nouns.size)]
        val (m, f) = w.adjectives[c.adjective.mod(w.adjectives.size)]
        val adj = if (noun.feminine) f else m
        return if (w.adjectiveFirst) "$adj ${noun.word} ${c.number}" else "${noun.word} $adj ${c.number}"
    }

    /** Só o substantivo, para a saudação ("Bom dia, Lobo"). */
    fun short(c: Codename, language: String): String {
        val w = words(language)
        return w.nouns[c.noun.mod(w.nouns.size)].word
    }
}
