package app.compasso.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import app.compasso.model.Codename
import app.compasso.model.FocusSettings
import app.compasso.model.Lapse
import app.compasso.model.Level
import app.compasso.model.LevelStatus
import app.compasso.model.Levels
import app.compasso.model.PointEntry
import app.compasso.model.Progress
import app.compasso.model.ReleaseGate
import app.compasso.model.ReleaseMode
import app.compasso.model.Tip
import app.compasso.ui.components.Astro
import app.compasso.ui.components.CodenameAvatar
import app.compasso.ui.components.DayStar
import app.compasso.ui.components.GlassCard
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.SkyBackground
import app.compasso.ui.components.StarWeek
import app.compasso.ui.components.TipCard
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.tierColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

private val PT: Locale = Locale.forLanguageTag("pt-BR")
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT)

/** Saudação pela hora do dia. */
fun greeting(now: Long): String {
    val h = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
    return when (h) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
}

/** Cor do astro conforme a liberação: aberta, só controle ou controle desligado. */
@Composable
fun gateTint(gate: ReleaseGate): Color {
    val c = Ios.colors
    return when (gate) {
        is ReleaseGate.Open -> c.accent
        is ReleaseGate.Closed -> c.amber
        ReleaseGate.Off -> c.lavender
    }
}

/** Os últimos 7 dias como estrelas (o último é hoje). */
fun weekStars(points: List<PointEntry>, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<DayStar> {
    val today = Progress.day(now, zone)
    val byDay = points.groupBy { Progress.day(it.at, zone) }.mapValues { (_, l) -> l.sumOf { it.points } }
    return (6 downTo 0).map { i ->
        val d = today.minusDays(i.toLong())
        val pts = byDay[d] ?: 0
        val name = d.dayOfWeek.getDisplayName(TextStyle.SHORT, PT).trimEnd('.').replaceFirstChar { it.uppercase() }
        DayStar(
            label = name.take(3),
            fraction = (pts / Progress.DAILY_CAP.toFloat()).coerceIn(0f, 1f),
            isToday = i == 0,
            description = "${d.dayOfMonth}/${d.monthValue}: $pts pontos",
        )
    }
}

/**
 * Hoje: a tela principal. O astro mostra o estado (cor) e os pontos do dia (anel); embaixo,
 * escolher o nível deslizando e um único botão grande para começar.
 */
@Composable
fun TodayScreen(
    gate: ReleaseGate,
    customLevel: Level,
    focus: FocusSettings,
    codename: Codename,
    level: LevelStatus,
    points: List<PointEntry>,
    lapses: List<Lapse>,
    tip: Tip?,
    onTipClose: (Tip) -> Unit,
    onStart: (Level) -> Unit,
    onEditCustom: () -> Unit,
    onBreathe: () -> Unit,
    onGateInfo: () -> Unit,
    onSettings: () -> Unit,
    onProgress: () -> Unit,
    bottomPadding: Dp = 0.dp,
    now: Long = System.currentTimeMillis(),
    initialLevel: Int = 0,
) {
    val c = Ios.colors
    val levels = remember(customLevel) { Levels.presets + customLevel }
    var chosen by rememberSaveable { mutableIntStateOf(initialLevel.coerceIn(0, levels.lastIndex)) }
    val pager = rememberPagerState(initialPage = chosen) { levels.size }
    LaunchedEffect(pager.currentPage) { chosen = pager.currentPage }
    val scope = rememberCoroutineScope()
    val tint = gateTint(gate)
    val today = remember(points, now / 60_000) { Progress.pointsToday(points, now) }
    val week = remember(points, now / 60_000) { weekStars(points, now) }
    val streak = remember(points, lapses, now / 60_000) { Progress.streak(points, lapses, now) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val selected = levels[chosen]
    val screenH = LocalConfiguration.current.screenHeightDp
    val astroSize = (screenH * 0.30f - 20f).coerceIn(170f, 236f).dp

    SkyBackground(Modifier.fillMaxSize(), glow = tint) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = top + 10.dp, bottom = bottomPadding + 16.dp),
        ) {
            // cabeçalho: saudação e o emblema (abre os ajustes)
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${greeting(now)},", style = Ios.type.display.copy(lineHeight = Ios.type.display.fontSize), color = c.label)
                    Text(codename.name.substringBefore(' '), style = Ios.type.display, color = c.label)
                    Text(
                        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(dateFmt).replaceFirstChar { it.uppercase() },
                        style = Ios.type.subhead,
                        color = c.secondary,
                    )
                }
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onSettings)
                        .semantics { contentDescription = "Ajustes e perfil" },
                    contentAlignment = Alignment.Center,
                ) {
                    CodenameAvatar(codename.seed, 46.dp, ring = tierColor(level.tier))
                }
            }

            // o astro e o estado da liberação (menor em telas baixas, para o botão ficar à vista)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onGateInfo),
                contentAlignment = Alignment.Center,
            ) {
                Astro(progress = today / Progress.DAILY_CAP.toFloat(), tint = tint, size = astroSize)
            }
            val look = gateLook(gate, now)
            Text(
                look.title,
                style = Ios.type.title3,
                color = c.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            Text(
                look.subtitle,
                style = Ios.type.subhead,
                color = c.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
            )
            Text(
                "$today de ${Progress.DAILY_CAP} pontos hoje",
                style = Ios.type.footnote,
                color = c.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // escolher o nível deslizando
            Spacer(Modifier.height(18.dp))
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 44.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val l = levels[page]
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                LevelCard(
                    level = l,
                    gate = gate,
                    selected = page == pager.currentPage,
                    isCustom = page == levels.lastIndex,
                    onEdit = onEditCustom,
                    onClick = { scope.launch { pager.animateScrollToPage(page) } },
                    modifier = Modifier.graphicsLayer {
                        val s = lerp(1f, 0.92f, offset)
                        scaleX = s
                        scaleY = s
                        alpha = lerp(1f, 0.55f, offset)
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                "Começar ${selected.name}",
                { onStart(selected) },
                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                icon = Icons.Rounded.PlayArrow,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (focus.enabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = c.secondary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        focus.pendingOff -> "Modo foco ligado até ${formatUntil(focus.offAt, now)}"
                        focus.enabled -> "Modo foco: a tela fica fixada durante a sessão"
                        else -> "Modo foco desligado: sessões não valem pontos"
                    },
                    style = Ios.type.footnote,
                    color = c.secondary,
                )
            }

            if (tip != null) {
                Spacer(Modifier.height(16.dp))
                TipCard(tip.title, tip.message, Icons.Rounded.Air, onClose = { onTipClose(tip) })
            }

            // socorro rápido
            Spacer(Modifier.height(if (tip != null) 12.dp else 22.dp))
            GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onBreathe, contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(c.lavender.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Air, contentDescription = null, tint = c.lavender, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Vontade apertou?", style = Ios.type.headline, color = c.label)
                        Text("Respire 2 minutos comigo. Ela passa.", style = Ios.type.subhead, color = c.secondary)
                    }
                    app.compasso.ui.components.Chevron(c.tertiary, Modifier.size(width = 8.dp, height = 13.dp))
                }
            }

            // a semana em estrelas
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onProgress, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sua semana", style = Ios.type.headline, color = c.label, modifier = Modifier.weight(1f))
                    Text(
                        when (streak) {
                            0 -> "Comece hoje"
                            1 -> "1 dia seguido"
                            else -> "$streak dias seguidos"
                        },
                        style = Ios.type.subhead,
                        color = c.secondary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                StarWeek(week, Modifier.semantics { contentDescription = week.joinToString { it.description } })
            }
        }
    }
}

/** Cartão de um nível no carrossel. */
@Composable
private fun LevelCard(
    level: Level,
    gate: ReleaseGate,
    selected: Boolean,
    isCustom: Boolean,
    onEdit: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val tint = levelTint(level)
    val closed = gate is ReleaseGate.Closed
    val ending = when (level.release) {
        ReleaseMode.NEVER -> "Sem liberação"
        ReleaseMode.ALWAYS -> if (closed) "Só controle agora" else "Libera no fim"
        ReleaseMode.CHANCE -> if (closed) "Só controle agora" else "Sorteio de ${level.releaseChance}%"
    }
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) tint.copy(alpha = 0.16f) else c.glass)
            .border(if (selected) 1.dp else 0.75.dp, if (selected) tint.copy(alpha = 0.7f) else c.glassBorder, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(level.name, style = Ios.type.serifSection.copy(fontSize = Ios.type.serifSection.fontSize * 1.15f), color = c.label, modifier = Modifier.weight(1f))
            if (isCustom) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.fill)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onEdit)
                        .semantics { contentDescription = "Ajustar o Personalizado" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = c.label, modifier = Modifier.size(18.dp))
                }
            } else {
                DifficultyMeter(level.difficulty, tint)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Fact("≈ ${level.estimatedMinutes} min", "duração")
            Fact("${level.rounds}", if (level.rounds == 1) "rodada" else "rodadas")
            Fact("${level.bpmStart}–${level.bpmEnd}", "bpm")
        }
        Spacer(Modifier.height(10.dp))
        Text(ending, style = Ios.type.subhead, color = if (closed && level.release != ReleaseMode.NEVER) c.amber else c.secondary)
    }
}

@Composable
private fun Fact(value: String, label: String) {
    val c = Ios.colors
    Column {
        Text(value, style = Ios.type.headline, color = c.label)
        Text(label, style = Ios.type.caption1, color = c.secondary)
    }
}

/** Cor de cada nível (fácil → impossível: frio → quente). */
@Composable
fun levelTint(level: Level): Color {
    val c = Ios.colors
    return when (level.id) {
        Levels.EASY.id -> c.green
        Levels.MEDIUM.id -> c.accent
        Levels.HARD.id -> c.amber
        Levels.IMPOSSIBLE.id -> c.pink
        else -> c.lavender
    }
}

/** Quatro barrinhas: quantas acesas = dificuldade. */
@Composable
fun DifficultyMeter(difficulty: Int, tint: Color) {
    val c = Ios.colors
    Canvas(Modifier.size(width = 26.dp, height = 18.dp).semantics { contentDescription = "Dificuldade $difficulty de 4" }) {
        val n = 4
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val h = size.height * (0.35f + 0.65f * (i + 1) / n)
            drawRoundRect(
                color = if (i < difficulty) tint else c.label.copy(alpha = 0.16f),
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2f),
            )
        }
    }
}
