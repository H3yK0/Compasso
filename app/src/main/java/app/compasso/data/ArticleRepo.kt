package app.compasso.data

import android.content.Context
import app.compasso.model.Article
import app.compasso.model.ArticleParser

/** Artigos que vêm dentro do app (assets/artigos), lidos sem internet. */
object ArticleRepo {
    private const val DIR = "artigos"

    fun load(context: Context): List<Article> {
        val assets = context.applicationContext.assets
        val files = runCatching { assets.list(DIR)?.toList() }.getOrNull().orEmpty().filter { it.endsWith(".md") }
        return files.mapNotNull { name ->
            runCatching {
                assets.open("$DIR/$name").bufferedReader(Charsets.UTF_8).use { ArticleParser.parse(it.readText()) }
            }.getOrNull()
        }.sortedWith(compareBy<Article> { it.order }.thenBy { it.title })
    }
}
