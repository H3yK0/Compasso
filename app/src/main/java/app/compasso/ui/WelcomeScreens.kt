package app.compasso.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import app.compasso.R
import app.compasso.model.Breath
import app.compasso.model.Codename
import app.compasso.ui.components.Astro
import app.compasso.ui.components.CodenameAvatar
import app.compasso.ui.components.GlowOrb
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.components.PageDots
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.SkyBackground
import app.compasso.ui.theme.Ios
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Primeira tela: o astro respirando no céu, o nome e um botão. */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val c = Ios.colors
    val t = rememberInfiniteTransition(label = "boas-vindas")
    val ms by t.animateFloat(
        0f, Breath.CYCLE_MS.toFloat(),
        infiniteRepeatable(tween(Breath.CYCLE_MS.toInt(), easing = LinearEasing), RepeatMode.Restart),
        label = "respiracao",
    )
    val orbit by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(36_000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbita",
    )
    val level = Breath.level(ms.toLong())
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var logo by remember { mutableStateOf<LayoutCoordinates?>(null) }
    SkyBackground(Modifier.fillMaxSize().onGloballyPositioned { root = it }, glowStrength = 1.3f) {
        // ondas de respiração saindo do astro
        Canvas(Modifier.fillMaxSize()) {
            val r = root
            val l = logo
            val center = if (r != null && l != null && r.isAttached && l.isAttached) {
                r.localPositionOf(l, Offset(l.size.width / 2f, l.size.height / 2f))
            } else {
                Offset(size.width / 2f, size.height * 0.36f)
            }
            drawBreathWaves(center, 96.dp.toPx(), level * 0.45f)
        }
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(272.dp).onGloballyPositioned { logo = it }, contentAlignment = Alignment.Center) {
                // a órbita do ícone, com o pontinho dando a volta devagar
                Canvas(Modifier.fillMaxSize()) {
                    val ringR = size.minDimension / 2f * 0.86f
                    drawCircle(
                        Brush.linearGradient(
                            listOf(c.lavender.copy(alpha = 0.35f), c.accent.copy(alpha = 0.9f)),
                            start = Offset(center.x - ringR, center.y + ringR),
                            end = Offset(center.x + ringR, center.y - ringR),
                        ),
                        radius = ringR,
                        center = center,
                        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    val ang = Math.toRadians((orbit - 35f).toDouble())
                    val dot = Offset(center.x + (cos(ang) * ringR).toFloat(), center.y + (sin(ang) * ringR).toFloat())
                    drawCircle(c.accent.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = dot)
                    drawCircle(Color.White, radius = 3.5.dp.toPx(), center = dot)
                }
                Astro(progress = 0f, tint = c.accent, size = 214.dp, showRing = false, level = level)
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.app_name), style = Ios.type.display.copy(fontSize = 54.sp, lineHeight = 60.sp), color = c.label)
            Text(
                stringResource(R.string.welcome_tagline),
                style = Ios.type.poetic.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = c.label.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.welcome_text),
                style = Ios.type.callout,
                color = c.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1.1f))
            PrimaryButton(stringResource(R.string.action_start), onStart, Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.welcome_privacy),
                style = Ios.type.footnote,
                color = c.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class IntroPage(val icon: ImageVector, val tint: Color, val title: String, val body: String)

@Composable
private fun introPages(): List<IntroPage> {
    val c = Ios.colors
    return listOf(
        IntroPage(Icons.Rounded.Spa, c.accent, stringResource(R.string.intro_1_title), stringResource(R.string.intro_1_text)),
        IntroPage(Icons.Rounded.GraphicEq, Color(0xFFFF8FA3), stringResource(R.string.intro_2_title), stringResource(R.string.intro_2_text)),
        IntroPage(Icons.Rounded.PhonelinkLock, c.amber, stringResource(R.string.intro_3_title), stringResource(R.string.intro_3_text)),
        IntroPage(Icons.Rounded.Air, c.lavender, stringResource(R.string.intro_4_title), stringResource(R.string.intro_4_text)),
    )
}

/**
 * Apresentação guiada em páginas. A última mostra o codinome anônimo (dá para sortear outro).
 * [onDone] fecha a apresentação; [replay] = aberta de novo pelos ajustes.
 */
@Composable
fun IntroScreen(
    codename: Codename,
    onNewCodename: () -> Unit,
    onDone: () -> Unit,
    replay: Boolean = false,
    startPage: Int = 0,
) {
    val c = Ios.colors
    val pages = introPages()
    val total = pages.size + 1
    val pager = rememberPagerState(initialPage = startPage.coerceIn(0, total - 1)) { total }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == total - 1
    val glow by animateColorAsState(pages.getOrNull(pager.currentPage)?.tint ?: c.accent, tween(500), label = "brilho")
    SkyBackground(Modifier.fillMaxSize(), glow = glow, glowStrength = 1.2f) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if (!last) NavTextButton(stringResource(if (replay) R.string.action_close else R.string.intro_skip), onDone)
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (page < pages.size) {
                        val p = pages[page]
                        GlowOrb(p.icon, p.tint, size = 200.dp)
                        Spacer(Modifier.height(18.dp))
                        Text(p.title, style = Ios.type.title1, color = c.label, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(p.body, style = Ios.type.body.copy(lineHeight = 25.sp), color = c.secondary, textAlign = TextAlign.Center)
                    } else {
                        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(
                                    Brush.radialGradient(listOf(c.accent.copy(alpha = 0.32f), Color.Transparent), center = center, radius = size.minDimension / 2f),
                                    radius = size.minDimension / 2f,
                                )
                            }
                            CodenameAvatar(codename.seed, 112.dp)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(stringResource(R.string.intro_5_title), style = Ios.type.title1, color = c.label, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.intro_5_text),
                            style = Ios.type.body.copy(lineHeight = 25.sp),
                            color = c.secondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(codenameText(codename), style = Ios.type.title2, color = c.accent, textAlign = TextAlign.Center)
                        NavTextButton(stringResource(R.string.intro_new_codename), onNewCodename)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.intro_5_note),
                            style = Ios.type.footnote,
                            color = c.tertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            PageDots(total, pager.currentPage, Modifier.fillMaxWidth().padding(vertical = 16.dp))
            PrimaryButton(
                stringResource(if (last) (if (replay) R.string.action_finish else R.string.action_start) else R.string.action_continue),
                { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

