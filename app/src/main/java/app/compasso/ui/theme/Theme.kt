package app.compasso.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.compasso.R
import app.compasso.model.Accent
import app.compasso.model.Tier

/**
 * Cores do Compasso. O visual principal é "Noite": um céu fundo, cartões de vidro e um astro
 * que brilha na cor de destaque. "Dia" é a versão clara, com os mesmos papéis.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    /** Céu: degradê do alto da tela até embaixo. */
    val skyTop: Color,
    val skyBottom: Color,
    /** Fundo sólido (quando não dá para usar o degradê). */
    val bg: Color,
    /** Vidro: cartões translúcidos sobre o céu, com uma borda fina. */
    val glass: Color,
    val glassBorder: Color,
    val card: Color,
    val cardHigh: Color,
    val fill: Color,
    val label: Color,
    val secondary: Color,
    val tertiary: Color,
    val separator: Color,
    val pressed: Color,
    val accent: Color,
    val onAccent: Color,
    val navBar: Color,
    val alert: Color,
    val alertCancel: Color,
    val switchOff: Color,
    val segment: Color,
    val sliderTrack: Color,
    val key: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val blue: Color,
    val indigo: Color,
    val purple: Color,
    val pink: Color,
    val teal: Color,
    val gray: Color,
    val lavender: Color,
    val amber: Color,
)

/** Noite: o céu de onde o Compasso tira o nome. */
val NightPalette = Palette(
    dark = true,
    skyTop = Color(0xFF111B45),
    skyBottom = Color(0xFF050817),
    bg = Color(0xFF080D22),
    glass = Color(0x14FFFFFF),
    glassBorder = Color(0x1AFFFFFF),
    card = Color(0x14FFFFFF),
    cardHigh = Color(0xFF1B2452),
    fill = Color(0x1FFFFFFF),
    label = Color(0xFFF3F1FF),
    secondary = Color(0xFFA9B0D8),
    tertiary = Color(0xFF6C759F),
    separator = Color(0x1CFFFFFF),
    pressed = Color(0x14FFFFFF),
    accent = Color(0xFF43D9C8),
    onAccent = Color(0xFF032420),
    navBar = Color(0xF20B1230),
    alert = Color(0xFF1A2250),
    alertCancel = Color(0xFF222B5E),
    switchOff = Color(0xFF2A3263),
    segment = Color(0xFF3A4580),
    sliderTrack = Color(0x26FFFFFF),
    key = Color(0x1AFFFFFF),
    green = Color(0xFF5BE39B),
    red = Color(0xFFFF7A7A),
    orange = Color(0xFFFFB65C),
    yellow = Color(0xFFFFD66B),
    blue = Color(0xFF7AA8FF),
    indigo = Color(0xFF8C8CFF),
    purple = Color(0xFFB98CFF),
    pink = Color(0xFFFF7EB3),
    teal = Color(0xFF43D9C8),
    gray = Color(0xFF8A90B8),
    lavender = Color(0xFF9D8CFF),
    amber = Color(0xFFFFB65C),
)

/** Dia: o mesmo céu, ao amanhecer. */
val DayPalette = Palette(
    dark = false,
    skyTop = Color(0xFFE6EBFF),
    skyBottom = Color(0xFFF7F8FD),
    bg = Color(0xFFF1F3FB),
    glass = Color(0xC7FFFFFF),
    glassBorder = Color(0x141B2250),
    card = Color(0xD9FFFFFF),
    cardHigh = Color(0xFFFFFFFF),
    fill = Color(0x141B2250),
    label = Color(0xFF121633),
    secondary = Color(0xFF575E86),
    tertiary = Color(0xFF9AA0BF),
    separator = Color(0x1F1B2250),
    pressed = Color(0x141B2250),
    accent = Color(0xFF0A8A80),
    onAccent = Color(0xFFFFFFFF),
    navBar = Color(0xF2F3F5FD),
    alert = Color(0xFFF7F8FD),
    alertCancel = Color(0xFFFFFFFF),
    switchOff = Color(0xFFE1E4F0),
    segment = Color(0xFFFFFFFF),
    sliderTrack = Color(0xFFDDE1EF),
    key = Color(0xFFE6E9F5),
    green = Color(0xFF1F9D55),
    red = Color(0xFFE5484D),
    orange = Color(0xFFC77800),
    yellow = Color(0xFFB98900),
    blue = Color(0xFF2F6FE0),
    indigo = Color(0xFF5B5BD6),
    purple = Color(0xFF8E4EC6),
    pink = Color(0xFFD6336C),
    teal = Color(0xFF0A8A80),
    gray = Color(0xFF7C82A3),
    lavender = Color(0xFF6C5CE7),
    amber = Color(0xFFC77800),
)

/** Nomes antigos (telas e testes da 1.2). */
val DarkPalette = NightPalette
val LightPalette = DayPalette

/** Inter (SIL OFL 1.1), variável: peso e tamanho óptico (texto ou título). */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int, opticalSize: Float) = Font(
    resId = R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

val InterText = FontFamily(inter(400, 14f), inter(500, 14f), inter(600, 14f), inter(700, 14f))
val InterDisplay = FontFamily(
    inter(200, 32f), inter(300, 32f), inter(400, 32f), inter(500, 32f), inter(600, 32f), inter(700, 32f),
)

/**
 * Cormorant Garamond (SIL OFL 1.1): a serifa das antigas cartas celestes. Só para títulos
 * grandes; números e textos do dia a dia ficam na Inter.
 */
val Serif = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_medium_italic, FontWeight.Medium, FontStyle.Italic),
)

@Immutable
data class IosType(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subhead: TextStyle,
    val footnote: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
    val timer: TextStyle,
    /** Título de destaque (saudação, nome do nível de herói). */
    val display: TextStyle,
    /** Título de seção na serifa. */
    val serifSection: TextStyle,
    /** Frase curta em itálico (uso raro: legendas poéticas). */
    val poetic: TextStyle,
    /** Números grandes (pontos, minutos). */
    val figure: TextStyle,
)

private fun ts(family: FontFamily, size: Int, line: Int, weight: FontWeight, tracking: Double, style: FontStyle = FontStyle.Normal) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    fontStyle = style,
    letterSpacing = tracking.em,
)

val DefaultType = IosType(
    largeTitle = ts(Serif, 38, 44, FontWeight.SemiBold, -0.005),
    title1 = ts(Serif, 32, 38, FontWeight.SemiBold, -0.004),
    title2 = ts(InterDisplay, 22, 28, FontWeight.Bold, -0.018),
    title3 = ts(InterDisplay, 20, 25, FontWeight.SemiBold, -0.017),
    headline = ts(InterText, 17, 22, FontWeight.SemiBold, -0.013),
    body = ts(InterText, 17, 23, FontWeight.Normal, -0.013),
    callout = ts(InterText, 16, 22, FontWeight.Normal, -0.011),
    subhead = ts(InterText, 15, 20, FontWeight.Normal, -0.009),
    footnote = ts(InterText, 13, 18, FontWeight.Normal, -0.003),
    caption1 = ts(InterText, 12, 16, FontWeight.Normal, 0.0),
    caption2 = ts(InterText, 11, 13, FontWeight.Normal, 0.005),
    timer = TextStyle(
        fontFamily = InterDisplay,
        fontSize = 92.sp,
        lineHeight = 98.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
    ),
    display = ts(Serif, 44, 50, FontWeight.SemiBold, -0.006),
    serifSection = ts(Serif, 26, 31, FontWeight.SemiBold, -0.002),
    poetic = ts(Serif, 20, 26, FontWeight.Medium, 0.0, FontStyle.Italic),
    figure = ts(InterDisplay, 34, 40, FontWeight.SemiBold, -0.02),
)

val LocalPalette = staticCompositionLocalOf { NightPalette }
val LocalIosType = staticCompositionLocalOf { DefaultType }

object Ios {
    val colors: Palette
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current

    val type: IosType
        @Composable @ReadOnlyComposable
        get() = LocalIosType.current
}

/** Cor de destaque (versão para o céu de noite, para o dia e cor do texto sobre ela à noite). */
data class AccentColors(val light: Color, val dark: Color, val onDark: Color)

fun accentColors(a: Accent): AccentColors = when (a) {
    Accent.TEAL -> AccentColors(Color(0xFF0A8A80), Color(0xFF43D9C8), Color(0xFF032420))
    Accent.AMBER -> AccentColors(Color(0xFFB45309), Color(0xFFF5A524), Color(0xFF331B00))
    Accent.OCEAN -> AccentColors(Color(0xFF1D4ED8), Color(0xFF7AA8FF), Color(0xFF071A3D))
    Accent.GOLD -> AccentColors(Color(0xFF946200), Color(0xFFF2C94C), Color(0xFF2E2200))
    Accent.LAVENDER -> AccentColors(Color(0xFF6D3FD6), Color(0xFFB9A3FF), Color(0xFF22104F))
    Accent.EMERALD -> AccentColors(Color(0xFF047857), Color(0xFF34D399), Color(0xFF002A1C))
    Accent.CRIMSON -> AccentColors(Color(0xFFBE123C), Color(0xFFFF6B8A), Color(0xFF3F0414))
}

/** Cor de cada degrau do ranking pessoal (medalhas e anéis). */
fun tierColor(t: Tier): Color = when (t) {
    Tier.START -> Color(0xFF8A90B8)
    Tier.BRONZE -> Color(0xFFC98A55)
    Tier.SILVER -> Color(0xFFB4BED0)
    Tier.GOLD -> Color(0xFFF2C94C)
    Tier.PLATINUM -> Color(0xFF7FE0D5)
    Tier.DIAMOND -> Color(0xFF8DB8FF)
    Tier.UNBEATEN -> Color(0xFFFF6B8A)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassoTheme(dark: Boolean, accent: Accent = Accent.TEAL, content: @Composable () -> Unit) {
    val base = if (dark) NightPalette else DayPalette
    val ac = accentColors(accent)
    val accentColor = if (dark) ac.dark else ac.light
    val p = base.copy(accent = accentColor, onAccent = if (dark) ac.onDark else Color.White, teal = if (accent == Accent.TEAL) accentColor else base.teal)
    val t = DefaultType
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, secondary = p.lavender,
            background = p.bg, onBackground = p.label, surface = p.cardHigh, onSurface = p.label,
            surfaceVariant = p.cardHigh, onSurfaceVariant = p.secondary,
            outline = p.separator, outlineVariant = p.separator, error = p.red,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = Color.White, secondary = p.lavender,
            background = p.bg, onBackground = p.label, surface = p.cardHigh, onSurface = p.label,
            surfaceVariant = p.bg, onSurfaceVariant = p.secondary,
            outline = p.separator, outlineVariant = p.separator, error = p.red,
        )
    }
    val typography = Typography(
        displayLarge = t.display, displayMedium = t.largeTitle, displaySmall = t.title1,
        headlineLarge = t.largeTitle, headlineMedium = t.title1, headlineSmall = t.title2,
        titleLarge = t.title3, titleMedium = t.headline, titleSmall = t.subhead,
        bodyLarge = t.body, bodyMedium = t.subhead, bodySmall = t.footnote,
        labelLarge = t.headline, labelMedium = t.footnote, labelSmall = t.caption2,
    )
    CompositionLocalProvider(LocalPalette provides p, LocalIosType provides t) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            // Sem o efeito de "onda" do Android: a resposta ao toque é feita à mão (escala e brilho).
            CompositionLocalProvider(LocalRippleConfiguration provides null, content = content)
        }
    }
}

/** Fundo de cada fase da sessão (degradê de cima para baixo). Igual nos temas de noite e de dia. */
object PhaseColors {
    val countdown = listOf(Color(0xFF26315E), Color(0xFF0C1230))
    val go = listOf(Color(0xFF138A93), Color(0xFF07364A))
    val stop = listOf(Color(0xFFB0334F), Color(0xFF4A0B24))
    val release = listOf(Color(0xFF2A8A57), Color(0xFF0B3524))
    val denied = listOf(Color(0xFF6551C4), Color(0xFF221A5E))
    val breathe = listOf(Color(0xFF2B3A8F), Color(0xFF0B1033))
}
