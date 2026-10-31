package app.compasso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.compasso.model.Outcome
import app.compasso.model.SessionRecord
import app.compasso.ui.components.GroupFooter
import app.compasso.ui.components.IconInset
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios
import kotlin.math.roundToInt

data class HistoryStats(val total: Int, val held: Int, val lost: Int, val streak: Int) {
    val holdRateLabel: String
        get() = if (held + lost == 0) "—" else "${(held * 100f / (held + lost)).roundToInt()}%"

    companion object {
        /** [list] vem do mais novo para o mais antigo. */
        fun from(list: List<SessionRecord>): HistoryStats {
            val held = list.count { it.outcome == Outcome.RELEASED || it.outcome == Outcome.DENIED }
            val lost = list.count { it.outcome == Outcome.LOST_CONTROL }
            var streak = 0
            for (r in list) {
                when (r.outcome) {
                    Outcome.RELEASED, Outcome.DENIED -> streak++
                    Outcome.LOST_CONTROL, Outcome.INTERRUPTED -> break
                    Outcome.STOPPED -> continue
                }
            }
            return HistoryStats(list.size, held, lost, streak)
        }
    }
}

@Composable
fun HistoryScreen(
    records: List<SessionRecord>,
    onBack: () -> Unit,
    now: Long = System.currentTimeMillis(),
    backLabel: String = "Evolução",
) {
    val c = Ios.colors
    val stats = remember(records) { HistoryStats.from(records) }
    val days = remember(records) { records.groupBy { localDate(it.startedAt) } }
    LargeTitleList("Histórico", onBack = onBack, backLabel = backLabel) {
        if (records.isEmpty()) {
            item(key = "vazio") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = c.tertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhuma sessão ainda", style = Ios.type.title3, color = c.label)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Quando você concluir ou encerrar uma sessão, ela aparece aqui.",
                        style = Ios.type.subhead,
                        color = c.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            item(key = "resumo") {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatTile("Sessões", "${stats.total}", c.accent, Modifier.weight(1f))
                    StatTile("Segurou", stats.holdRateLabel, c.green, Modifier.weight(1f))
                    StatTile("Sequência", "${stats.streak}", c.orange, Modifier.weight(1f))
                }
            }
            item(key = "nota") {
                GroupFooter(
                    "“Segurou” compara as sessões concluídas com as marcadas como “não segurei”. " +
                        "A sequência conta as concluídas seguidas, das mais recentes pra trás."
                )
            }
            days.forEach { (date, list) ->
                section(header = dayHeader(date, now), key = "dia-$date") {
                    list.forEachIndexed { i, r ->
                        if (i > 0) RowDivider(IconInset)
                        HistoryRow(r)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(c.card).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = Ios.type.title2, color = color)
        Text(label, style = Ios.type.footnote, color = c.secondary)
    }
}

@Composable
private fun HistoryRow(r: SessionRecord) {
    val look = outcomeLook(r.outcome, r.controlOnly)
    IosRow(
        title = if (r.controlOnly) "${r.levelName} · só controle" else r.levelName,
        subtitle = "${timeOfDay(r.startedAt)} · ${formatDuration(r.durationMs)} · perto ${r.closeCount}×",
        icon = RowIcon(look.icon, look.color),
        value = look.short,
    )
}
