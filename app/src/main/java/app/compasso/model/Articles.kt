package app.compasso.model

/** Uma referência citada no fim do artigo. */
data class Source(val text: String, val url: String?)

/** Trechos do artigo, já separados para a tela desenhar. Texto com **negrito** e *itálico*. */
sealed interface Block {
    data class Heading(val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullets(val items: List<String>) : Block
    data class Numbered(val items: List<String>) : Block
    data class Quote(val text: String) : Block
}

/** Tema do artigo: decide a cor da capa (o nome do tema vem traduzido no próprio artigo). */
enum class Theme { MIND, SCIENCE, BODY, SUPPORT, RELATIONS }

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val theme: Theme,
    val minutes: Int,
    val order: Int,
    val blocks: List<Block>,
    val sources: List<Source>,
    /** Assuntos do artigo (iguais nos três idiomas), usados pelas recomendações. */
    val tags: Set<String> = emptySet(),
)

/**
 * Lê os artigos em Markdown simples (assets/artigos/<idioma>). Cabeçalho entre linhas "---" com
 * id, titulo, resumo, categoria, tema, tags (separadas por vírgula), minutos e ordem. A seção "## Fontes" (ou "Sources",
 * "Fuentes") vira a lista de referências.
 */
object ArticleParser {
    private val sourceHeadings = setOf("fontes", "referências", "sources", "references", "fuentes", "referencias")

    private val urlRegex = Regex("""https?://\S+""")
    private val numbered = Regex("""^\d+[.)]\s+(.*)$""")

    fun parse(text: String): Article {
        val lines = text.replace("\r\n", "\n").lines()
        val meta = HashMap<String, String>()
        var i = 0
        if (lines.firstOrNull()?.trim() == "---") {
            i = 1
            while (i < lines.size && lines[i].trim() != "---") {
                val line = lines[i]
                val colon = line.indexOf(':')
                if (colon > 0) {
                    meta[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim().removeSurrounding("\"")
                }
                i++
            }
            i++
        }

        val blocks = ArrayList<Block>()
        val sources = ArrayList<Source>()
        var inSources = false
        val paragraph = StringBuilder()
        val list = ArrayList<String>()
        var listNumbered = false
        val quote = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotBlank()) blocks += Block.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }

        fun flushList() {
            if (list.isNotEmpty()) {
                if (inSources) {
                    list.forEach { item ->
                        val url = urlRegex.find(item)?.value?.trimEnd('.', ',', ')')
                        val clean = if (url != null) item.replace(url, "").trim().trimEnd(':', '·', '-').trim() else item
                        sources += Source(clean, url)
                    }
                } else {
                    blocks += if (listNumbered) Block.Numbered(list.toList()) else Block.Bullets(list.toList())
                }
            }
            list.clear()
        }

        fun flushQuote() {
            if (quote.isNotBlank()) blocks += Block.Quote(quote.toString().trim())
            quote.clear()
        }

        fun flushAll() {
            flushParagraph()
            flushList()
            flushQuote()
        }

        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trim()
            when {
                line.isEmpty() -> flushAll()
                line.startsWith("## ") || line.startsWith("### ") -> {
                    flushAll()
                    val title = line.trimStart('#').trim()
                    inSources = title.lowercase() in sourceHeadings
                    if (!inSources) blocks += Block.Heading(title)
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    flushParagraph()
                    flushQuote()
                    if (list.isNotEmpty() && listNumbered) flushList()
                    listNumbered = false
                    list += line.substring(2).trim()
                }
                numbered.matches(line) -> {
                    flushParagraph()
                    flushQuote()
                    if (list.isNotEmpty() && !listNumbered) flushList()
                    listNumbered = true
                    list += numbered.find(line)!!.groupValues[1].trim()
                }
                line.startsWith(">") -> {
                    flushParagraph()
                    flushList()
                    if (quote.isNotEmpty()) quote.append(' ')
                    quote.append(line.removePrefix(">").trim())
                }
                list.isNotEmpty() && raw.startsWith("  ") -> {
                    // continuação de um item da lista
                    list[list.lastIndex] = list.last() + " " + line
                }
                else -> {
                    flushList()
                    flushQuote()
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line)
                }
            }
            i++
        }
        flushAll()

        return Article(
            id = meta["id"] ?: error("artigo sem id"),
            title = meta["titulo"] ?: meta["title"] ?: "",
            summary = meta["resumo"] ?: "",
            category = meta["categoria"] ?: meta["category"] ?: "",
            theme = when (meta["tema"] ?: meta["theme"]) {
                "mente" -> Theme.MIND
                "ciencia" -> Theme.SCIENCE
                "corpo" -> Theme.BODY
                "relacoes" -> Theme.RELATIONS
                else -> Theme.SUPPORT
            },
            tags = meta["tags"].orEmpty().split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
            minutes = meta["minutos"]?.toIntOrNull() ?: 3,
            order = meta["ordem"]?.toIntOrNull() ?: 99,
            blocks = blocks,
            sources = sources,
        )
    }

    /** Tira as marcas de **negrito** e *itálico* (para contar palavras, buscar etc.). */
    fun plain(text: String): String = text.replace("**", "").replace(Regex("""(?<!\*)\*(?!\*)"""), "")

    fun wordCount(article: Article): Int = article.blocks.sumOf { b ->
        when (b) {
            is Block.Heading -> words(b.text)
            is Block.Paragraph -> words(b.text)
            is Block.Quote -> words(b.text)
            is Block.Bullets -> b.items.sumOf { words(it) }
            is Block.Numbered -> b.items.sumOf { words(it) }
        }
    }

    private fun words(s: String) = plain(s).split(Regex("""\s+""")).count { it.isNotBlank() }
}
