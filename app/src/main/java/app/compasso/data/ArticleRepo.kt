package app.compasso.data

import android.content.Context
import app.compasso.model.Article
import app.compasso.model.ArticleParser

/** Artigos que vêm dentro do app (assets/artigos/<idioma>), lidos sem internet. */
object ArticleRepo {
    private const val DIR = "artigos"
    val LANGUAGES = listOf("pt", "en", "es")

    /** [language]: "pt", "en" ou "es" (outro idioma cai no inglês). */
    fun load(context: Context, language: String): List<Article> {
        val lang = if (language in LANGUAGES) language else "en"
        val assets = context.applicationContext.assets
        val dir = "$DIR/$lang"
        val files = runCatching { assets.list(dir)?.toList() }.getOrNull().orEmpty().filter { it.endsWith(".md") }
        return files.mapNotNull { name ->
            runCatching {
                assets.open("$dir/$name").bufferedReader(Charsets.UTF_8).use { ArticleParser.parse(it.readText()) }
            }.getOrNull()
        }.sortedWith(compareBy<Article> { it.order }.thenBy { it.title })
    }
}
