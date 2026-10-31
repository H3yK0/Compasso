package app.compasso.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import app.compasso.data.AppLanguage
import java.util.Locale

/** Idioma escolhido nos ajustes, aplicado ao abrir a tela (sem depender da versão do Android). */
object AppLocale {
    /** Idiomas com tradução completa; outros caem no inglês. */
    private val SUPPORTED = setOf("pt", "en", "es")

    fun wrap(base: Context, language: AppLanguage): Context {
        val tag = language.tag
        if (tag == null) {
            Locale.setDefault(base.resources.configuration.locales[0])
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /** "pt", "en" ou "es": o idioma em que o app está de fato (para escolher os artigos). */
    fun language(locale: Locale): String = locale.language.takeIf { it in SUPPORTED } ?: "en"
}

/** Idioma da tela atual (o mesmo dos textos traduzidos). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** "pt", "en" ou "es". */
@Composable
@ReadOnlyComposable
fun currentLanguage(): String = AppLocale.language(currentLocale())
