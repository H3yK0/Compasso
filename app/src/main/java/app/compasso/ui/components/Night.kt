package app.compasso.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.compasso.ui.theme.Ios
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------- Céu ----------

private class Star(val x: Float, val y: Float, val r: Float, val a: Float, val sparkle: Boolean)

private val skyStars: List<Star> by lazy {
    val rnd = Random(20261012)
    List(78) { i ->
        // mais estrelas no alto, como num céu de verdade visto da janela
        val y = rnd.nextFloat().let { it * it }
        Star(
            x = rnd.nextFloat(),
            y = y,
            r = 0.4f + rnd.nextFloat() * 0.7f,
            a = 0.10f + rnd.nextFloat() * 0.42f * (1f - y * 0.6f),
            sparkle = i % 19 == 0 && y < 0.3f,
        )
    }
}

/** Desenha o céu: degradê, um brilho suave da cor do momento e estrelas fixas (à noite). */
fun DrawScope.drawSky(top: Color, bottom: Color, glow: Color, glowStrength: Float, dark: Boolean, stars: Boolean) {
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
    val g = glow.copy(alpha = (if (dark) 0.20f else 0.14f) * glowStrength)
    drawCircle(
        Brush.radialGradient(listOf(g, Color.Transparent), center = Offset(size.width * 0.5f, size.height * 0.10f), radius = size.width * 0.95f),
        radius = size.width * 0.95f,
        center = Offset(size.width * 0.5f, size.height * 0.10f),
    )
    val lav = Color(0xFF9D8CFF).copy(alpha = if (dark) 0.08f else 0.06f)
    drawCircle(
        Brush.radialGradient(listOf(lav, Color.Transparent), center = Offset(0f, size.height * 0.85f), radius = size.width * 0.9f),
        radius = size.width * 0.9f,
        center = Offset(0f, size.height * 0.85f),
    )
    if (!stars || !dark) return
    val d = density
    for (s in skyStars) {
        // atrás de subtítulos e datas uma estrelinha parece pontuação: essa faixa fica limpa
        if (s.x < 0.92f && s.y * 0.9f in 0.075f..0.20f) continue
        val c = Offset(s.x * size.width, s.y * size.height * 0.9f)
        drawCircle(Color.White.copy(alpha = s.a), radius = s.r * d, center = c)
        if (s.sparkle) {
            val l = 4f * d
            drawLine(Color.White.copy(alpha = s.a * 0.5f), c - Offset(l, 0f), c + Offset(l, 0f), strokeWidth = 0.5f * d)
            drawLine(Color.White.copy(alpha = s.a * 0.5f), c - Offset(0f, l), c + Offset(0f, l), strokeWidth = 0.5f * d)
        }
    }
}

/** Só as estrelas (para fundos coloridos, como as fases da sessão). */
fun DrawScope.drawStarField(alpha: Float = 1f) {
    val d = density
    for (s in skyStars) {
        if (s.x < 0.92f && s.y * 0.9f in 0.075f..0.20f) continue
        val c = Offset(s.x * size.width, s.y * size.height * 0.9f)
        drawCircle(Color.White.copy(alpha = s.a * alpha), radius = s.r * d, center = c)
    }
}

/** Fundo das telas: o céu do Compasso. */
@Composable
fun SkyBackground(
    modifier: Modifier = Modifier,
    glow: Color = Ios.colors.accent,
    glowStrength: Float = 1f,
    stars: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = Ios.colors
    Box(
        modifier.drawBehind { drawSky(c.skyTop, c.skyBottom, glow, glowStrength, c.dark, stars) },
        content = content,
    )
}

// ---------- Vidro ----------

/** Cartão de vidro: translúcido sobre o céu, com borda fina. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, spring(stiffness = 1400f), label = "vidro")
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (pressed) lerp(c.glass, c.label, 0.06f) else c.glass)
            .border(0.75.dp, c.glassBorder, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

// ---------- Astro ----------

/** A rosa dos ventos do Compasso: 4 pontas longas e 4 curtas. */
fun DrawScope.drawCompassRose(center: Offset, radius: Float, color: Color, rotation: Float = 0f) {
    val path = Path()
    for (i in 0 until 16) {
        val ang = rotation * PI / 180.0 + i * PI / 8 - PI / 2
        val len = when (i % 4) {
            0 -> radius
            2 -> radius * 0.5f
            else -> radius * 0.16f
        }
        val x = center.x + (cos(ang) * len).toFloat()
        val y = center.y + (sin(ang) * len).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun Color.mix(other: Color, t: Float) = lerp(this, other, t)

/**
 * O astro: a peça que dá identidade ao Compasso. Uma esfera luminosa com a rosa dos ventos,
 * um halo da cor do momento e um anel que se completa com os pontos do dia (como uma lua
 * que vai enchendo). Respira devagar quando [breathing].
 */
@Composable
fun Astro(
    progress: Float,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    breathing: Boolean = true,
    showRing: Boolean = true,
    level: Float? = null,
) {
    val c = Ios.colors
    val pulse = if (breathing && level == null) {
        val t = rememberInfiniteTransition(label = "astro")
        val v by t.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                keyframes {
                    durationMillis = 10_000
                    0f at 0
                    1f at 4_000 using FastOutSlowInEasing
                    0f at 10_000 using FastOutSlowInEasing
                },
            ),
            label = "respira",
        )
        v
    } else {
        level ?: 0.5f
    }
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "anel")
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val center = this.center
        // halo
        val haloR = r * (0.98f + 0.04f * pulse)
        drawCircle(
            Brush.radialGradient(
                0f to tint.copy(alpha = if (c.dark) 0.42f else 0.30f),
                0.55f to tint.copy(alpha = if (c.dark) 0.14f else 0.10f),
                1f to Color.Transparent,
                center = center,
                radius = haloR,
            ),
            radius = haloR,
            center = center,
        )
        // anel do dia
        val ringR = r * 0.80f
        val stroke = 3.dp.toPx()
        if (showRing) {
            drawCircle(c.label.copy(alpha = 0.10f), radius = ringR, center = center, style = Stroke(stroke))
            if (shown > 0.001f) {
                rotate(-90f, center) {
                    drawArc(
                        Brush.sweepGradient(listOf(tint.copy(alpha = 0.55f), tint, c.lavender, tint), center = center),
                        startAngle = 0f,
                        sweepAngle = 360f * shown,
                        useCenter = false,
                        topLeft = Offset(center.x - ringR, center.y - ringR),
                        size = Size(ringR * 2, ringR * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                val ang = -PI / 2 + 2 * PI * shown
                val dot = Offset(center.x + (cos(ang) * ringR).toFloat(), center.y + (sin(ang) * ringR).toFloat())
                drawCircle(tint.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = dot)
                drawCircle(Color.White, radius = 3.dp.toPx(), center = dot)
            }
        }
        // esfera
        val sr = r * (0.56f + 0.035f * pulse)
        drawCircle(
            Brush.radialGradient(
                0f to Color.White.mix(tint, 0.35f),
                0.45f to tint,
                1f to tint.mix(Color(0xFF0A0F2E), if (c.dark) 0.62f else 0.36f),
                center = Offset(center.x - sr * 0.32f, center.y - sr * 0.38f),
                radius = sr * 1.45f,
            ),
            radius = sr,
            center = center,
        )
        // luz de borda
        drawCircle(Color.White.copy(alpha = 0.22f), radius = sr, center = center, style = Stroke(1.dp.toPx()))
        drawCompassRose(center, sr * 0.62f, Color.White.copy(alpha = 0.95f), rotation = 6f * pulse)
        drawCircle(Color.White.copy(alpha = 0.35f), radius = sr * 0.07f, center = center)
    }
}

// ---------- Botões ----------

/** Botão principal: grande, em pílula, na cor de destaque com brilho. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = Ios.colors.accent,
    contentColor: Color? = null,
    height: Dp = 58.dp,
) {
    val c = Ios.colors
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, spring(stiffness = 1500f), label = "principal")
    // texto escuro sobre cores claras (contraste), branco sobre as escuras
    val fg = contentColor ?: when {
        color == c.accent -> c.onAccent
        color.luminance() > 0.25f -> Color(0xFF0B1234)
        else -> Color.White
    }
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(height)
            .shadow(if (enabled) 18.dp else 0.dp, CircleShape, ambientColor = color, spotColor = color)
            .clip(CircleShape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(color, color.mix(Color.White, 0.22f))) else Brush.linearGradient(listOf(c.fill, c.fill)),
            )
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) fg else c.tertiary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = Ios.type.headline.copy(fontSize = 18.sp), color = if (enabled) fg else c.tertiary, maxLines = 1)
    }
}

/** Botão secundário de vidro, em pílula. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 50.dp,
    tint: Color = Ios.colors.label,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 1500f), label = "vidro-botao")
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(height)
            .clip(CircleShape)
            .background(c.glass)
            .border(0.75.dp, c.glassBorder, CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Ios.type.headline, color = tint, maxLines = 1)
    }
}

/** Botão redondo grande de tocar/pausar (o "play" do Respirar). */
@Composable
fun RoundPlayButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    color: Color = Ios.colors.accent,
    contentColor: Color? = null,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = 1300f), label = "play")
    val fg = contentColor ?: when {
        color == c.accent -> c.onAccent
        color.luminance() > 0.6f -> Color(0xFF0B1234)
        else -> Color.White
    }
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .shadow(20.dp, CircleShape, ambientColor = color, spotColor = color)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color.mix(Color.White, 0.25f), color)))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = fg, modifier = Modifier.size(size * 0.42f))
    }
}

/** Escolha em pílula (ex.: 1, 2 ou 5 minutos). */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Box(
        modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) c.accent.copy(alpha = 0.22f) else c.glass)
            .border(0.75.dp, if (selected) c.accent.copy(alpha = 0.6f) else c.glassBorder, CircleShape)
            .selectable(selected = selected, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Ios.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = if (selected) c.label else c.secondary)
    }
}

/** Título de seção na serifa, com uma ação opcional à direita. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = Ios.colors
    Row(modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 26.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = Ios.type.serifSection, color = c.label, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) NavTextButton(action, onAction)
    }
}

// ---------- Barra de abas flutuante ----------

/** Altura ocupada pela barra flutuante (para o conteúdo não ficar escondido atrás dela). */
val FloatingTabBarSpace = 96.dp

/** Barra de abas que flutua sobre o céu, em forma de cápsula. */
@Composable
fun FloatingTabBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val haptic = LocalHapticFeedback.current
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.42f to c.skyBottom.copy(alpha = 0.88f), 1f to c.skyBottom))
            .padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = bottom + 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(24.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(c.navBar)
                .border(0.75.dp, c.glassBorder, CircleShape)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, item ->
                val isSel = i == selected
                val tint = if (isSel) c.accent else c.secondary
                val bg by animateFloatAsState(if (isSel) 1f else 0f, tween(220), label = "aba")
                Column(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.14f * bg))
                        .selectable(selected = isSel, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) {
                            if (!isSel) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(if (isSel) item.selectedIcon else item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, style = Ios.type.caption2.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = tint, maxLines = 1)
                }
            }
        }
    }
}

// ---------- Semana em estrelas ----------

data class DayStar(val label: String, val fraction: Float, val isToday: Boolean, val description: String)

/**
 * A semana como uma constelação: cada dia é uma estrela, maior e mais brilhante quanto mais
 * pontos; dias seguidos com pontos ficam ligados por uma linha.
 */
@Composable
fun StarWeek(days: List<DayStar>, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(46.dp)) {
            val n = days.size
            if (n == 0) return@Canvas
            val slot = size.width / n
            val cy = size.height / 2f
            fun cx(i: Int) = slot * i + slot / 2f
            // ligações
            for (i in 0 until n - 1) {
                if (days[i].fraction > 0f && days[i + 1].fraction > 0f) {
                    drawLine(c.accent.copy(alpha = 0.45f), Offset(cx(i), cy), Offset(cx(i + 1), cy), strokeWidth = 1.2.dp.toPx())
                }
            }
            days.forEachIndexed { i, d ->
                val center = Offset(cx(i), cy)
                if (d.isToday) drawCircle(c.label.copy(alpha = 0.35f), radius = 17.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                if (d.fraction > 0f) {
                    val rr = (4f + 8f * d.fraction).dp.toPx()
                    drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.45f), Color.Transparent), center = center, radius = rr * 2.4f), radius = rr * 2.4f, center = center)
                    drawCompassRose(center, rr, lerp(c.accent, Color.White, 0.35f))
                } else {
                    drawCircle(c.tertiary.copy(alpha = 0.6f), radius = 2.dp.toPx(), center = center)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            days.forEach { d ->
                Text(
                    d.label,
                    style = Ios.type.caption1.copy(fontWeight = if (d.isToday) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (d.isToday) c.label else c.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------- Capas geradas ----------

enum class Motif { WAVES, ORBITS, BLOCKS, TILES, PADS, STARS, SPARK }

/**
 * Capa desenhada pelo app (sem imagens de terceiros): degradê de duas cores e um desenho
 * simples que lembra o conteúdo.
 */
@Composable
fun CoverArt(colors: List<Color>, motif: Motif, modifier: Modifier = Modifier, seed: Int = 1, centered: Boolean = false) {
    Canvas(modifier) {
        drawRect(Brush.linearGradient(colors, start = Offset(0f, 0f), end = Offset(size.width, size.height)))
        val w = size.width
        val h = size.height
        val white = Color.White
        when (motif) {
            Motif.WAVES -> for (k in 0 until 4) {
                val path = Path()
                val y0 = h * (0.45f + k * 0.12f)
                val amp = h * (0.05f + 0.01f * k)
                path.moveTo(0f, y0)
                var x = 0f
                while (x <= w) {
                    path.lineTo(x, y0 + amp * sin((x / w * 2.4 * PI + k * 0.9 + seed)).toFloat())
                    x += w / 40f
                }
                drawPath(path, white.copy(alpha = 0.38f - k * 0.07f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
            Motif.ORBITS -> {
                val center = if (centered) Offset(w * 0.5f, h * 0.5f) else Offset(w * 0.72f, h * 0.30f)
                for (k in 1..4) drawCircle(white.copy(alpha = 0.22f - k * 0.03f), radius = h * 0.18f * k, center = center, style = Stroke(1.2.dp.toPx()))
                drawCircle(white.copy(alpha = 0.85f), radius = h * 0.09f, center = center)
                val ang = seed * 1.3
                drawCircle(white, radius = 3.dp.toPx(), center = center + Offset((cos(ang) * h * 0.36f).toFloat(), (sin(ang) * h * 0.36f).toFloat()))
            }
            Motif.BLOCKS -> {
                val cell = h * 0.13f
                val gap = cell * 0.18f
                val pattern = listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1, 0 to 2, 1 to 2, 2 to 2, 3 to 2)
                pattern.forEachIndexed { i, (cx, cy) ->
                    drawRoundRect(
                        white.copy(alpha = 0.30f + 0.08f * (i % 4)),
                        topLeft = Offset(w * 0.50f + cx * (cell + gap), h * 0.12f + cy * (cell + gap)),
                        size = Size(cell, cell),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.25f),
                    )
                }
            }
            Motif.TILES -> {
                val s = h * 0.26f
                for (k in 0 until 3) {
                    drawRoundRect(
                        white.copy(alpha = 0.18f + 0.14f * k),
                        topLeft = Offset(w * 0.50f + k * s * 0.32f, h * 0.10f + k * s * 0.32f),
                        size = Size(s, s),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.2f),
                    )
                }
            }
            Motif.PADS -> {
                val r = h * 0.10f
                val cx = w * 0.70f
                val cy = h * 0.32f
                listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1).forEachIndexed { i, (dx, dy) ->
                    val center = Offset(cx + dx * r * 1.25f, cy + dy * r * 1.25f)
                    drawCircle(white.copy(alpha = if (i == seed % 4) 0.9f else 0.30f), radius = r, center = center)
                }
            }
            Motif.STARS -> {
                val rnd = Random(seed)
                val x0 = if (centered) 0.15f else 0.40f
                val span = if (centered) 0.70f else 0.55f
                val pts = List(if (centered) 4 else 6) { Offset(w * (x0 + rnd.nextFloat() * span), h * (0.15f + rnd.nextFloat() * (if (centered) 0.70f else 0.55f))) }
                for (i in 0 until pts.size - 1) drawLine(white.copy(alpha = 0.35f), pts[i], pts[i + 1], strokeWidth = 1.dp.toPx())
                pts.forEach { drawCompassRose(it, (if (centered) 4.5f else 6f).dp.toPx(), white.copy(alpha = 0.9f)) }
            }
            Motif.SPARK -> if (centered) {
                drawCompassRose(Offset(w * 0.5f, h * 0.5f), h * 0.32f, white.copy(alpha = 0.85f), rotation = 8f)
            } else {
                drawCompassRose(Offset(w * 0.70f, h * 0.38f), h * 0.26f, white.copy(alpha = 0.85f), rotation = 8f)
            }
        }
        // véu escuro embaixo, para o texto por cima ficar legível
        if (!centered) drawRect(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.38f)))
    }
}

/** Duas cores de capa por tema. */
object CoverColors {
    val mind = listOf(Color(0xFF5B4BD6), Color(0xFF1F1A5E))
    val science = listOf(Color(0xFF138A93), Color(0xFF0B2F52))
    val body = listOf(Color(0xFFC2546B), Color(0xFF4A1438))
    val support = listOf(Color(0xFFD08A2E), Color(0xFF5A2A12))
    val relations = listOf(Color(0xFF2E9E6E), Color(0xFF0E3A3A))
    val breathe = listOf(Color(0xFF3A57C9), Color(0xFF121A52))
    val blocks = listOf(Color(0xFFE58A3A), Color(0xFF7A2A1E))
    val merge = listOf(Color(0xFF8E5BE0), Color(0xFF2E1A6B))
    val echo = listOf(Color(0xFFE0507E), Color(0xFF4A1A55))
}

/** Pontinho com brilho (marcador de "novo" ou de estado). */
@Composable
fun GlowDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Canvas(modifier.size(size * 3)) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent)), radius = this.size.minDimension / 2f)
        drawCircle(color, radius = size.toPx() / 2f)
    }
}

/** Espaço para o conteúdo não ficar atrás da barra flutuante. */
@Composable
fun BottomBarSpacer() = Spacer(Modifier.height(FloatingTabBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))

// ---------- Marca ----------

/** O ícone do app desenhado na tela (mesmo desenho do ícone do celular). */
@Composable
fun AppIconTile(modifier: Modifier = Modifier, size: Dp = 84.dp, round: Boolean = false) {
    Canvas(modifier.size(size).clip(if (round) CircleShape else RoundedCornerShape(size * 0.26f))) {
        val w = this.size.width
        val u = w / 72f
        // coordenadas do desenho do ícone (0..108) para a área visível do quadrado
        fun p(x: Float, y: Float) = Offset(w / 2f + (x - 54f) * u, w / 2f + (y - 54f) * u)
        drawRect(
            Brush.radialGradient(
                0f to Color(0xFF1C2B6E), 0.55f to Color(0xFF0B1234), 1f to Color(0xFF050817),
                center = p(54f, 36f), radius = 86f * u,
            ),
        )
        drawCircle(
            Brush.radialGradient(
                0f to Color(0x6643D9C8), 0.6f to Color(0x1A43D9C8), 1f to Color(0x0043D9C8),
                center = p(54f, 54f), radius = 34f * u,
            ),
            radius = 34f * u,
            center = p(54f, 54f),
        )
        for ((x, y, a) in IconStars) drawCircle(Color.White.copy(alpha = a), radius = 0.7f * u, center = p(x, y))
        drawAstroMark(p(54f, 54f), u)
    }
}

/** As estrelinhas do fundo do ícone (mesmas posições do vetor). */
private val IconStars = listOf(
    Triple(39.91f, 26.07f, 0.38f), Triple(23.90f, 31.86f, 0.77f), Triple(25.54f, 23.42f, 0.71f),
    Triple(57.82f, 19.02f, 0.44f), Triple(56.02f, 84.01f, 0.48f), Triple(84.04f, 39.10f, 0.62f),
    Triple(79.75f, 36.77f, 0.65f), Triple(81.20f, 89.57f, 0.65f), Triple(18.64f, 54.59f, 0.55f),
    Triple(17.14f, 67.46f, 0.61f), Triple(18.85f, 70.12f, 0.80f), Triple(92.41f, 23.45f, 0.69f),
)

/** Esfera + rosa dos ventos + órbita, em unidades do ícone (108 = largura). */
fun DrawScope.drawAstroMark(center: Offset, u: Float, tint: Color = Color(0xFF43D9C8)) {
    val ring = 23f * u
    drawCircle(
        Brush.linearGradient(listOf(Color(0x559D8CFF), tint), start = center + Offset(-ring, ring), end = center + Offset(ring, -ring)),
        radius = ring,
        center = center,
        style = Stroke(1.6f * u),
    )
    val orb = 15.5f * u
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFEFFFFC), 0.45f to tint, 1f to Color(0xFF1A2F6E),
            center = center + Offset(-5f * u, -6.5f * u), radius = 23f * u,
        ),
        radius = orb,
        center = center,
    )
    drawCircle(Color.White.copy(alpha = 0.25f), radius = orb, center = center, style = Stroke(0.6f * u))
    drawCompassRose(center, orb * 0.66f, Color.White.copy(alpha = 0.95f))
    val ang = Math.toRadians(-35.0)
    val dot = center + Offset((cos(ang) * ring).toFloat(), (sin(ang) * ring).toFloat())
    drawCircle(tint.copy(alpha = 0.33f), radius = 3.6f * u, center = dot)
    drawCircle(Color.White, radius = 1.9f * u, center = dot)
}

// ---------- Esfera com ícone ----------

/** Desenha uma esfera luminosa com halo (a mesma luz do astro). */
fun DrawScope.drawOrb(center: Offset, sphereR: Float, haloR: Float, tint: Color, dark: Boolean) {
    drawCircle(
        Brush.radialGradient(
            0f to tint.copy(alpha = if (dark) 0.40f else 0.28f),
            0.55f to tint.copy(alpha = if (dark) 0.13f else 0.09f),
            1f to Color.Transparent,
            center = center,
            radius = haloR,
        ),
        radius = haloR,
        center = center,
    )
    drawCircle(
        Brush.radialGradient(
            0f to Color.White.mix(tint, 0.35f),
            0.45f to tint,
            1f to tint.mix(Color(0xFF0A0F2E), if (dark) 0.62f else 0.38f),
            center = Offset(center.x - sphereR * 0.32f, center.y - sphereR * 0.38f),
            radius = sphereR * 1.45f,
        ),
        radius = sphereR,
        center = center,
    )
    drawCircle(Color.White.copy(alpha = 0.22f), radius = sphereR, center = center, style = Stroke(1.dp.toPx()))
}

/** Esfera luminosa com um ícone no meio (ilustrações da apresentação, resumo, modo foco). */
@Composable
fun GlowOrb(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 170.dp, pulse: Float = 0f) {
    val c = Ios.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            drawOrb(center, r * (0.50f + 0.03f * pulse), r * (0.98f + 0.04f * pulse), tint, c.dark)
        }
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.30f))
    }
}

/** Pontinhos de página: o atual vira uma pílula na cor de destaque. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val w by animateFloatAsState(if (i == current) 22f else 7f, tween(260), label = "pagina")
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = w.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(if (i == current) c.accent else c.label.copy(alpha = 0.22f)),
            )
        }
    }
}
