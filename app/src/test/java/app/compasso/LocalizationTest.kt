package app.compasso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Os três idiomas têm exatamente os mesmos textos, com os mesmos espaços para valores. */
class LocalizationTest {
    private data class Entry(val name: String, val texts: List<String>)

    private fun load(dir: String): Map<String, Entry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$dir/strings.xml"))
        val out = LinkedHashMap<String, Entry>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            if (e.getAttribute("translatable") == "false") continue
            out[e.getAttribute("name")] = Entry(e.getAttribute("name"), listOf(e.textContent))
        }
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val e = plurals.item(i) as Element
            val items = e.getElementsByTagName("item")
            val texts = (0 until items.length).map { (items.item(it) as Element).let { item -> item.getAttribute("quantity") + "=" + item.textContent } }
            out["plural:" + e.getAttribute("name")] = Entry(e.getAttribute("name"), texts)
        }
        return out
    }

    private val placeholder = Regex("""%(\d+)\$[-0-9]*[sdf]""")

    private fun args(t: String) = placeholder.findAll(t).map { it.value }.toSortedSet()

    @Test
    fun everyLanguageHasTheSameKeysAndPlaceholders() {
        val en = load("values")
        val pt = load("values-pt")
        val es = load("values-es")
        assertTrue("textos demais para ser verdade", en.size > 400)
        listOf("pt" to pt, "es" to es).forEach { (lang, other) ->
            assertEquals("$lang: chaves", en.keys, other.keys)
            en.forEach { (key, e) ->
                val o = other.getValue(key)
                val base = e.texts.flatMap { args(it.substringAfter('=')) }.toSortedSet()
                val mine = o.texts.flatMap { args(it.substringAfter('=')) }.toSortedSet()
                assertEquals("$lang/$key: valores", base, mine)
                o.texts.forEach { assertTrue("$lang/$key vazio", it.substringAfter('=').isNotBlank()) }
            }
        }
    }

    /** Português e espanhol não podem ter sobrado em inglês (e vice-versa) por engano. */
    @Test
    fun translationsAreNotCopiesOfEnglish() {
        val en = load("values")
        val same = listOf("values-pt", "values-es").associateWith { dir ->
            load(dir).filter { (k, v) -> en.getValue(k).texts == v.texts && v.texts.joinToString().length > 14 }.keys
        }
        // nomes próprios e fórmulas que são iguais em todo idioma
        val allowed = setOf("license_agpl", "hint_go_title", "level_summary", "day_points", "greeting_comma", "duration_hours", "duration_min_sec")
        same.forEach { (dir, keys) -> assertEquals(dir, emptySet<String>(), keys - allowed) }
    }
}
