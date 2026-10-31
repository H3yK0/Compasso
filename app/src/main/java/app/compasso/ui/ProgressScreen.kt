package app.compasso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.compasso.ui.components.GlassCard
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.SectionTitle
import app.compasso.ui.components.drawCompassRose
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.compasso.model.Accent
import app.compasso.model.Codename
import app.compasso.model.LevelStatus
import app.compasso.model.PointEntry
import app.compasso.model.PointKind
import app.compasso.model.Progress
import app.compasso.model.SessionRecord
import app.compasso.model.Span
import app.compasso.model.Tier
import app.compasso.model.Tip
import app.compasso.model.WeekScore
import app.compasso.ui.components.BarChart
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.CodenameAvatar
import app.compasso.ui.components.GroupCard
import app.compasso.ui.components.GroupFooter
import app.compasso.ui.components.GroupHeader
import app.compasso.ui.components.IconInset
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.IosSegmented
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.ProgressBar
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.TierMedal
import app.compasso.ui.components.TipCard
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.tierColor
import java.time.format.DateTimeFormatter

private val dayMonth = DateTimeFormatter.ofPattern("d/M")

/** Aba Evolução: nível, constância, gráficos, pódio, ranking pessoal e recordes. */
@Composable
fun ProgressScreen(
    level: LevelStatus,
    codename: Codename,
    points: List<PointEntry>,
    history: List<SessionRecord>,
    tip: Tip?,
    onTipClose: (Tip) -> Unit,
    onCheckIn: () -> Unit,
    onProfile: () -> Unit,
    onHistory: () -> Unit,
    bottomPadding: Dp = 0.dp,
    now: Long = System.currentTimeMillis(),
) {
    val c = Ios.colors
    var span by rememberSaveable { mutableStateOf(Span.DAYS) }
    val streak = remember(points, history, now / 60_000) { Progress.streak(points, history, now) }
    val today = remember(points, now / 60_000) { Progress.pointsToday(points, now) }
    val checkedIn = remember(points, now / 60_000) { Progress.doneToday(points, PointKind.CHECKIN, now) }
    val podium = remember(points) { Progress.podium(points) }
    val records = remember(points, history) { Progress.records(points, history) }
    val control = remember(history, span, now / 60_000) { Progress.controlSeries(history, span, now) }
    val pts = remember(points, span, now / 60_000) { Progress.pointsSeries(points, span, now) }

    val month = remember(points, now / 60_000) { monthSky(points, now) }

    LargeTitleList("Evolução", bottomPadding = bottomPadding, rootTab = true, glow = c.lavender) {
        if (tip != null) {
            item(key = "dica") {
                TipCard(tip.title, tip.message, Icons.Rounded.Insights, onClose = { onTipClose(tip) }, Modifier.padding(top = 6.dp, bottom = 4.dp))
            }
        }
        item(key = "perfil") { LevelCard(level, codename, streak, today, onProfile) }
        item(key = "checkin") { CheckInCard(checkedIn, onCheckIn) }

        item(key = "mes") {
            Column {
                SectionTitle("Seu céu do mês")
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp)) {
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.Bottom) {
                        Text(month.title, style = Ios.type.headline, color = c.label, modifier = Modifier.weight(1f))
                        Text(
                            when (month.litDays) {
                                0 -> "Nenhuma estrela ainda"
                                1 -> "1 dia com pontos"
                                else -> "${month.litDays} dias com pontos"
                            },
                            style = Ios.type.subhead,
                            color = c.secondary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    MonthConstellation(month)
                }
                GroupFooter("Cada dia com pontos vira uma estrela, maior quanto mais perto dos ${Progress.DAILY_CAP}. Dias seguidos se ligam.")
            }
        }

        item(key = "graficos") {
            Column {
                SectionTitle("Tempo de controle")
                Box(Modifier.padding(horizontal = 16.dp)) {
                    IosSegmented(
                        listOf("Sessões", "Dias", "Semanas", "Meses"),
                        Span.entries.indexOf(span),
                        { span = Span.entries[it] },
                    )
                }
                Spacer(Modifier.height(10.dp))
                GroupCard {
                    BarChart(
                        bars = control,
                        // cores validadas para contraste (≥ 3:1) e croma no fundo claro e no escuro
                        color = if (c.dark) Color(0xFF1A98AE) else Color(0xFF0A7FA0),
                        unit = "min",
                        caption = { b -> "Maior tempo de treino · ${spanCaption(span, b.label)}" },
                        emptyText = "Seus tempos de treino aparecem aqui",
                    )
                }
                GroupFooter("Quanto tempo você conseguiu treinar, parando e recomeçando, antes do fim ou da liberação. Sessões interrompidas ficam de fora.")
                SectionTitle("Pontos de constância")
                GroupCard {
                    BarChart(
                        bars = pts,
                        color = if (c.dark) Color(0xFFD77A12) else Color(0xFFD96C00),
                        unit = "pts",
                        caption = { b -> spanCaption(if (span == Span.SESSIONS) Span.DAYS else span, b.label) },
                        emptyText = "Seus pontos aparecem aqui",
                    )
                }
            }
        }

        item(key = "podio") {
            Column {
                SectionTitle("Suas melhores semanas")
                GroupCard { Podium(podium) }
                GroupFooter("Você compete com você mesmo: as três semanas com mais pontos de constância ficam no pódio.")
            }
        }

        section(
            header = "Ranking pessoal",
            footer = "O nível conta só os pontos dos últimos 28 dias: subir é difícil e, para ficar, é preciso manter. " +
                "Invicto pede também 14 dias seguidos de constância e 14 dias sem “não segurei” nem sessão interrompida. " +
                "Cada nível libera uma cor para o app, enquanto você estiver nele.",
            key = "ranking",
        ) {
            Tier.entries.reversed().forEachIndexed { i, t ->
                if (i > 0) RowDivider(IconInset)
                val reached = t.ordinal <= level.tier.ordinal
                val current = t == level.tier
                val accent = Accent.entries.firstOrNull { it.unlock == t }
                IosRow(
                    title = t.label,
                    subtitle = buildString {
                        append(if (t.minPoints == 0) "Ponto de partida" else "${t.minPoints} pontos em 28 dias")
                        if (accent != null && t != Tier.START) append(" · cor ${accent.label}")
                    },
                    leading = { TierMedal(t, 30.dp, locked = !reached) },
                    titleStyle = if (current) Ios.type.headline else null,
                    trailing = {
                        when {
                            current -> Text("Você", style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = Color.White,
                                modifier = Modifier.clip(CircleShape).background(tierColor(t)).padding(horizontal = 9.dp, vertical = 3.dp))
                            !reached -> Icon(Icons.Rounded.Lock, contentDescription = "Bloqueado", tint = c.tertiary, modifier = Modifier.size(16.dp))
                            else -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Alcançado", tint = c.green, modifier = Modifier.size(18.dp))
                        }
                    },
                )
            }
        }

        section(header = "Recordes", key = "recordes") {
            IosRow("Maior tempo de controle", icon = RowIcon(Icons.Rounded.Timer, c.teal), value = if (records.bestControlMs > 0) formatDuration(records.bestControlMs) else "—")
            RowDivider(IconInset)
            IosRow("Maior sequência", icon = RowIcon(Icons.Rounded.LocalFireDepartment, c.orange), value = if (records.bestStreak > 0) daysLabel(records.bestStreak) else "—")
            RowDivider(IconInset)
            IosRow("Melhor semana", icon = RowIcon(Icons.Rounded.EmojiEvents, c.yellow), value = if (records.bestWeekPoints > 0) "${records.bestWeekPoints} pts" else "—")
            RowDivider(IconInset)
            IosRow("Sessões concluídas", icon = RowIcon(Icons.Rounded.Verified, c.green), value = "${records.completedSessions}")
            RowDivider(IconInset)
            IosRow("Sem liberação", icon = RowIcon(Icons.Rounded.Today, c.indigo), value = "${records.controlSessions}")
        }

        section(
            header = "Como ganhar pontos",
            footer = "No máximo ${Progress.DAILY_CAP} pontos por dia. Sessões só valem com o modo foco ligado e a tela fixada, e só a primeira do dia conta.",
            key = "regras",
        ) {
            PointKind.entries.forEachIndexed { i, k ->
                if (i > 0) RowDivider()
                IosRow(
                    title = k.label,
                    value = "+${k.base}" + if (k.maxPerDay > 1) " · ${k.maxPerDay}×/dia" else " · 1×/dia",
                )
            }
        }

        section(key = "historico") {
            IosRow("Histórico de sessões", icon = RowIcon(Icons.Rounded.History, c.blue), value = "${history.size}", chevron = true, onClick = onHistory)
        }
    }
}

private fun spanCaption(span: Span, label: String): String = when (span) {
    Span.SESSIONS -> "sessão do dia $label"
    Span.DAYS -> "dia $label"
    Span.WEEKS -> "semana de $label"
    Span.MONTHS -> label
}

private fun daysLabel(n: Int) = if (n == 1) "1 dia" else "$n dias"

@Composable
private fun LevelCard(level: LevelStatus, codename: Codename, streak: Int, today: Int, onProfile: () -> Unit) {
    val c = Ios.colors
    val tc = tierColor(level.tier)
    GlassCard(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth()
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(tc.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width * 0.12f, 0f), radius = size.width * 0.7f),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.12f, 0f),
                )
            },
        onClick = onProfile,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CodenameAvatar(codename.seed, 64.dp, ring = tc)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(codename.name, style = Ios.type.headline, color = c.label)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierMedal(level.tier, 22.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(level.tier.label, style = Ios.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = tc)
                    Text(" · ${level.windowPoints} pts em 28 dias", style = Ios.type.subhead, color = c.secondary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ProgressBar(level.progress, tc)
        Spacer(Modifier.height(6.dp))
        val next = level.next
        Text(
            when {
                level.missing.isNotEmpty() -> "Pontos de Invicto alcançados. Falta: ${level.missing.joinToString(" e ")}."
                next == null -> "Você é Invicto. Para continuar, mantenha a constância."
                else -> "Faltam ${level.toNext} pontos para ${next.label}"
            },
            style = Ios.type.footnote,
            color = c.secondary,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(Icons.Rounded.LocalFireDepartment, c.orange, if (streak == 1) "1 dia" else "$streak dias", "seguidos", Modifier.weight(1f))
            MiniStat(Icons.Rounded.Today, c.accent, "$today de ${Progress.DAILY_CAP}", "pontos hoje", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: String, label: String, modifier: Modifier) {
    val c = Ios.colors
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.fill.copy(alpha = c.fill.alpha * 0.6f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, style = Ios.type.headline, color = c.label)
            Text(label, style = Ios.type.caption1, color = c.secondary)
        }
    }
}

@Composable
private fun CheckInCard(done: Boolean, onCheckIn: () -> Unit) {
    val c = Ios.colors
    GlassCard(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Text("Check-in de hoje", style = Ios.type.headline, color = c.label)
        Spacer(Modifier.height(2.dp))
        Text(
            if (done) "Feito. Um dia de cada vez." else "Até agora, hoje, você ficou longe da pornografia? Marque com honestidade: é só pra você.",
            style = Ios.type.subhead,
            color = c.secondary,
        )
        Spacer(Modifier.height(12.dp))
        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.green, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Check-in feito hoje", style = Ios.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = c.green)
            }
        } else {
            PrimaryButton("Sim, fiquei longe · +10", onCheckIn, Modifier.fillMaxWidth(), height = 50.dp)
        }
    }
}

/** Pódio das 3 melhores semanas: 2º à esquerda, 1º no meio (mais alto), 3º à direita. */
@Composable
private fun Podium(weeks: List<WeekScore>) {
    val c = Ios.colors
    val medal = listOf(Color(0xFFE2B33C), Color(0xFFA7B0BC), Color(0xFFC07A45))
    val heights = listOf(92.dp, 70.dp, 54.dp)
    val order = listOf(1, 0, 2)
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        order.forEach { rank ->
            val w = weeks.getOrNull(rank)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (w != null) "${w.points} pts" else "—",
                    style = Ios.type.headline,
                    color = if (w != null) c.label else c.tertiary,
                )
                Text(
                    if (w != null) "semana de ${w.start.format(dayMonth)}" else "a conquistar",
                    style = Ios.type.caption1,
                    color = c.secondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(heights[rank])
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(medal[rank].copy(alpha = if (w != null) 0.9f else 0.25f), medal[rank].copy(alpha = if (w != null) 0.55f else 0.12f)),
                            ),
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        "${rank + 1}º",
                        style = Ios.type.title2,
                        color = Color.White.copy(alpha = if (w != null) 1f else 0.6f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/** Um dia no céu do mês (null = espaço antes do dia 1). */
data class MonthDay(val day: Int, val points: Int, val fraction: Float, val isToday: Boolean, val isFuture: Boolean)

data class MonthSky(val title: String, val cells: List<MonthDay?>, val litDays: Int)

/** O mês atual em semanas (domingo a sábado), com os pontos de cada dia. */
fun monthSky(points: List<PointEntry>, now: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): MonthSky {
    val today = Progress.day(now, zone)
    val first = today.withDayOfMonth(1)
    val byDay = points.groupBy { Progress.day(it.at, zone) }.mapValues { (_, l) -> l.sumOf { it.points } }
    val lead = first.dayOfWeek.value % 7 // domingo = 0
    val cells = buildList<MonthDay?> {
        repeat(lead) { add(null) }
        for (d in 1..today.lengthOfMonth()) {
            val date = first.withDayOfMonth(d)
            val pts = byDay[date] ?: 0
            add(MonthDay(d, pts, (pts / Progress.DAILY_CAP.toFloat()).coerceIn(0f, 1f), date == today, date.isAfter(today)))
        }
        while (size % 7 != 0) add(null)
    }
    val name = today.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, java.util.Locale.forLanguageTag("pt-BR"))
        .replaceFirstChar { it.uppercase() }
    return MonthSky(name, cells, cells.count { it != null && it.points > 0 })
}

private val WEEK_INITIALS = listOf("D", "S", "T", "Q", "Q", "S", "S")

/** O mês como constelação: estrelas nos dias com pontos, ligadas quando seguidas. */
@Composable
private fun MonthConstellation(month: MonthSky) {
    val c = Ios.colors
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            WEEK_INITIALS.forEach {
                Text(it, style = Ios.type.caption1.copy(fontWeight = FontWeight.SemiBold), color = c.tertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        month.cells.chunked(7).forEach { week ->
            Box(Modifier.fillMaxWidth().height(50.dp)) {
                // ligações entre dias seguidos com pontos (dentro da semana)
                Canvas(Modifier.fillMaxSize()) {
                    val slot = size.width / 7f
                    val cy = 17.dp.toPx()
                    for (i in 0 until 6) {
                        val a = week[i]
                        val b = week[i + 1]
                        if (a != null && b != null && a.points > 0 && b.points > 0) {
                            drawLine(c.accent.copy(alpha = 0.4f), Offset(slot * i + slot / 2f, cy), Offset(slot * (i + 1) + slot / 2f, cy), strokeWidth = 1.2.dp.toPx())
                        }
                    }
                }
                Row(Modifier.fillMaxSize()) {
                    week.forEach { d ->
                        Column(
                            Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) {
                                if (d != null) contentDescription = "Dia ${d.day}: ${d.points} pontos"
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (d != null) {
                                Canvas(Modifier.size(34.dp)) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    if (d.isToday) drawCircle(c.label.copy(alpha = 0.4f), radius = 15.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                                    when {
                                        d.points > 0 -> {
                                            val rr = (3.5f + 7f * d.fraction).dp.toPx()
                                            drawCircle(
                                                Brush.radialGradient(listOf(c.accent.copy(alpha = 0.42f), Color.Transparent), center = center, radius = rr * 2.3f),
                                                radius = rr * 2.3f,
                                                center = center,
                                            )
                                            drawCompassRose(center, rr, androidx.compose.ui.graphics.lerp(c.accent, Color.White, 0.35f))
                                        }
                                        d.isFuture -> drawCircle(c.label.copy(alpha = 0.10f), radius = 1.6.dp.toPx(), center = center)
                                        else -> drawCircle(c.tertiary.copy(alpha = 0.7f), radius = 1.8.dp.toPx(), center = center)
                                    }
                                }
                                Text(
                                    "${d.day}",
                                    style = Ios.type.caption2.copy(fontWeight = if (d.isToday) FontWeight.Bold else FontWeight.Normal),
                                    color = if (d.isToday) c.label else c.tertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
