package app.compasso

import app.compasso.audio.AmbientSynth
import app.compasso.model.ArticleParser
import app.compasso.model.Block
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/** Artigos (Markdown em assets) e trilha sintetizada. */
class ContentTest {
    @Test
    fun parserReadsFrontmatterBlocksAndSources() {
        val a = ArticleParser.parse(
            """
            ---
            id: teste
            titulo: "Título: com dois-pontos"
            resumo: Um resumo.
            categoria: Mente
            minutos: 3
            ordem: 4
            ---
            Primeiro parágrafo
            continua aqui com **negrito**.

            ## Seção

            - item um
              continua
            - item dois

            1. passo
            2. outro passo

            > Uma citação.

            ## Fontes

            - Autor A (2020). Título. *Revista*, 1, 2–3. https://doi.org/10.1/abc
            - Livro sem link.
            """.trimIndent(),
        )
        assertEquals("teste", a.id)
        assertEquals("Título: com dois-pontos", a.title)
        assertEquals(3, a.minutes)
        assertEquals(4, a.order)
        assertEquals(Block.Paragraph("Primeiro parágrafo continua aqui com **negrito**."), a.blocks[0])
        assertEquals(Block.Heading("Seção"), a.blocks[1])
        assertEquals(Block.Bullets(listOf("item um continua", "item dois")), a.blocks[2])
        assertEquals(Block.Numbered(listOf("passo", "outro passo")), a.blocks[3])
        assertEquals(Block.Quote("Uma citação."), a.blocks[4])
        assertEquals(5, a.blocks.size)
        assertEquals(2, a.sources.size)
        assertEquals("https://doi.org/10.1/abc", a.sources[0].url)
        assertFalse("http" in a.sources[0].text)
        assertEquals(null, a.sources[1].url)
    }

    private fun articles(language: String) = File("src/main/assets/artigos/$language")
        .listFiles { f -> f.name.endsWith(".md") }.orEmpty().sortedBy { it.name }
        .map { ArticleParser.parse(it.readText()) }

    /** Os três idiomas têm os mesmos artigos (mesmo id, ordem, tema, tempo e fontes). */
    @Test
    fun articlesExistInEveryLanguage() {
        val pt = articles("pt")
        listOf("en", "es").forEach { lang ->
            val other = articles(lang)
            assertEquals(lang, pt.map { it.id }, other.map { it.id })
            assertEquals(lang, pt.map { it.order }, other.map { it.order })
            assertEquals(lang, pt.map { it.theme }, other.map { it.theme })
            assertEquals(lang, pt.map { it.minutes }, other.map { it.minutes })
            pt.zip(other).forEach { (a, b) ->
                assertEquals("$lang ${a.id}: títulos de seção", a.blocks.count { it is Block.Heading }, b.blocks.count { it is Block.Heading })
                assertTrue("$lang ${a.id}: fontes", b.sources.size >= a.sources.size - 1 && b.sources.isNotEmpty())
                assertFalse("$lang ${a.id}: categoria", b.category.isBlank())
            }
        }
    }

    @Test
    fun bundledArticlesAreCompleteAndCiteSources() {
        listOf("pt", "en", "es").forEach { checkArticles(it) }
    }

    private fun checkArticles(language: String) {
        val articles = articles(language)
        assertEquals(9, articles.size)
        assertEquals(articles.size, articles.map { it.id }.toSet().size)
        assertEquals(articles.size, articles.map { it.order }.toSet().size)
        articles.forEach { a ->
            assertTrue(a.id, a.title.isNotBlank() && a.summary.isNotBlank())
            assertFalse(a.id, a.title.startsWith("\""))
            assertTrue(a.id, a.sources.isNotEmpty())
            assertTrue(a.id, a.sources.all { it.text.length > 10 })
            assertTrue(a.id, a.blocks.count { it is Block.Heading } >= 2)
            // tempo de leitura coerente: 120 a 260 palavras por minuto
            val wpm = ArticleParser.wordCount(a) / a.minutes.toFloat()
            assertTrue("$language ${a.id}: $wpm palavras/min", wpm in 120f..260f)
            // sem promessas absolutas
            val text = a.blocks.joinToString(" ") { b ->
                when (b) {
                    is Block.Heading -> b.text
                    is Block.Paragraph -> b.text
                    is Block.Quote -> b.text
                    is Block.Bullets -> b.items.joinToString(" ")
                    is Block.Numbered -> b.items.joinToString(" ")
                }
            }.lowercase()
            listOf("cura garantida", "fatal", "100% eficaz", "guaranteed cure", "100% effective", "cura garantizada", "100% eficaz")
                .forEach { assertFalse("$language ${a.id}: $it", it in text) }
        }
    }

    @Test
    fun ambientIsAudibleBoundedAndFollowsTheBreath() {
        val sr = 22_050
        val synth = AmbientSynth(sr, seed = 3)
        val frames = sr / 10
        val buf = FloatArray(frames * 2)
        fun rms(breath: Float, seconds: Int): Double {
            var sum = 0.0
            var n = 0
            repeat(seconds * 10) {
                synth.render(buf, frames, breath)
                buf.forEach { x ->
                    assertTrue(x.isFinite())
                    assertTrue(abs(x) <= 1f)
                    sum += x * x
                    n++
                }
            }
            return sqrt(sum / n)
        }
        rms(0f, 3) // entrada suave (fade-in)
        val exhale = rms(0f, 4)
        val inhale = rms(1f, 4)
        assertTrue("silêncio: $exhale", exhale > 0.005)
        assertTrue("inspirar deveria encher o som: $inhale vs $exhale", inhale > exhale * 1.15)

        synth.masterTarget = 0.0
        rms(0f, 6)
        assertTrue(synth.masterLevel < 0.01)
        assertNotNull(synth)
    }
}
