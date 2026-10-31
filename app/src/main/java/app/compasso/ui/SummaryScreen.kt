package app.compasso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import app.compasso.ui.components.GlassCard
import app.compasso.ui.components.GlowOrb
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.SkyBackground
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.compasso.model.ReleaseGate
import app.compasso.model.Tip
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.TipCard
import app.compasso.ui.components.GroupCard
import app.compasso.ui.components.GroupFooter
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.theme.Ios

@Composable
fun SummaryScreen(
    summary: SummaryUi,
    onHome: () -> Unit,
    onRepeat: () -> Unit,
    now: Long = System.currentTimeMillis(),
    tip: Tip? = null,
    onTipClose: (Tip) -> Unit = {},
) {
    val c = Ios.colors
    val r = summary.record
    val look = outcomeLook(r.outcome, r.controlOnly)
    SkyBackground(Modifier.fillMaxSize(), glow = look.color, glowStrength = 1.2f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            GlowOrb(look.icon, look.color, Modifier.semantics { contentDescription = look.short }, size = 170.dp)
            Text(
                r.levelName,
                style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = c.label,
                modifier = Modifier.clip(CircleShape).background(c.fill).padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(look.title, style = Ios.type.title1, color = c.label, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(6.dp))
            Text(look.message, style = Ios.type.body, color = c.secondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(22.dp))
            // os números da sessão em quatro quadros
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Tempo de treino", formatClock(r.durationMs), Modifier.weight(1f))
                    StatTile("Rodadas", "${r.roundsDone} de ${r.roundsTotal}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Chegou perto", "${r.closeCount}×", Modifier.weight(1f))
                    StatTile("Ritmo máximo", if (r.maxBpm > 0) "${r.maxBpm} bpm" else "—", Modifier.weight(1f))
                }
            }
            when (val g = summary.gate) {
                is ReleaseGate.Open -> {
                    Spacer(Modifier.height(14.dp))
                    GroupCard { IosRow("Próxima liberação", icon = RowIcon(Icons.Rounded.CheckCircle, c.green), value = "Disponível") }
                }
                is ReleaseGate.Closed -> {
                    Spacer(Modifier.height(14.dp))
                    GroupCard { IosRow("Próxima liberação", icon = RowIcon(Icons.Rounded.HourglassTop, c.amber), value = formatUntil(g.until, now)) }
                }
                ReleaseGate.Off -> Unit
            }
            if (summary.saved) GroupFooter("Salvo no histórico")
            val pts = summary.points
            if (pts != null || summary.noPoints != null) {
                Spacer(Modifier.height(14.dp))
                GroupCard {
                    IosRow(
                        "Pontos de constância",
                        icon = RowIcon(Icons.Rounded.Insights, if (pts != null) c.amber else c.gray),
                        value = if (pts != null) "+${pts.points}" else "0",
                    )
                }
                summary.noPoints?.let { GroupFooter(it) }
            }
            if (tip != null) {
                Spacer(Modifier.height(16.dp))
                TipCard(tip.title, tip.message, Icons.Rounded.Insights, onClose = { onTipClose(tip) })
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PrimaryButton("Concluído", onHome, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                NavTextButton("Repetir este nível", onRepeat)
            }
        }
    }
}

/** Um número da sessão num quadro de vidro. */
@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Ios.colors
    GlassCard(modifier, radius = 20.dp, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Text(value, style = Ios.type.title2, color = c.label)
        Text(label, style = Ios.type.footnote, color = c.secondary)
    }
}
