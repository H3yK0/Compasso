package app.compasso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.compasso.model.GateRule
import app.compasso.model.Pacing
import app.compasso.model.PacingSettings
import app.compasso.model.PacingSnapshot
import app.compasso.model.ReleaseGate
import app.compasso.ui.components.IconInset
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios

/** Detalhes do controle inteligente: estado agora e as três regras, sem segredo. */
@Composable
fun PacingInfoScreen(
    snapshot: PacingSnapshot,
    pacing: PacingSettings,
    onDone: () -> Unit,
    onSettings: () -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    val c = Ios.colors
    val gate = snapshot.gate
    val look = gateLook(gate, now)
    val hours = pacing.config.intervalHours
    val rules = (gate as? ReleaseGate.Closed)?.rules.orEmpty()
    val busy = when (gate) {
        is ReleaseGate.Open -> gate.busyWeek
        is ReleaseGate.Closed -> gate.busyWeek
        ReleaseGate.Off -> false
    }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Box(Modifier.fillMaxWidth().height(52.dp)) {
            Text("Controle inteligente", style = Ios.type.headline, color = c.label, modifier = Modifier.align(Alignment.Center))
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) { NavTextButton("OK", onDone, bold = true) }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 28.dp),
        ) {
            item(key = "estado") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(look.tint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(look.icon, contentDescription = null, tint = look.tint, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(look.title, style = Ios.type.title2, color = c.label, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(look.subtitle, style = Ios.type.body, color = c.secondary, textAlign = TextAlign.Center)
                    if (gate is ReleaseGate.Closed) {
                        Spacer(Modifier.height(6.dp))
                        Text(gateReason(gate), style = Ios.type.footnote, color = c.secondary, textAlign = TextAlign.Center)
                    }
                }
            }
            section(header = "Agora", key = "agora") {
                IosRow("Última liberação", value = snapshot.lastRelease?.let { formatPast(it, now) } ?: "nenhuma recente")
                RowDivider()
                IosRow("Sessões em 7 dias", value = "${snapshot.sessionsThisWeek}")
                RowDivider()
                IosRow(
                    "Intervalo atual",
                    value = if (gate == ReleaseGate.Off) "desligado" else if (busy) "${hours * 2} h" else "$hours h",
                )
            }
            section(
                header = "As regras",
                footer = "As regras são fixas e sempre visíveis, pra você saber o que esperar. A sessão nunca é cancelada: com a liberação fechada, ela vira treino de controle.",
                key = "regras",
            ) {
                RuleRow(
                    RowIcon(Icons.Rounded.HourglassTop, c.orange), "Intervalo",
                    "Depois de liberar (ou de um “não segurei”, ou de uma sessão interrompida), a próxima liberação só vem depois de $hours h.",
                    active = GateRule.INTERVAL in rules,
                )
                RowDivider(IconInset)
                RuleRow(
                    RowIcon(Icons.Rounded.Bolt, c.pink), "Sem pressa",
                    "Sessão começada menos de 2 h depois da anterior é só de controle.",
                    active = GateRule.RUSH in rules,
                )
                RowDivider(IconInset)
                RuleRow(
                    RowIcon(Icons.Rounded.CalendarMonth, c.blue), "Semana cheia",
                    "Com ${Pacing.BUSY_WEEK_SESSIONS} sessões ou mais em 7 dias, o intervalo dobra.",
                    active = busy,
                )
            }
            section(key = "ajustar") {
                IosRow("Ajustar o controle", titleColor = c.accent, chevron = true, onClick = onSettings)
            }
        }
    }
}

@Composable
private fun RuleRow(icon: RowIcon, title: String, text: String, active: Boolean) {
    val c = Ios.colors
    IosRow(
        title = title,
        subtitle = text,
        icon = icon,
        trailing = if (active) {
            {
                Text(
                    "Ativa",
                    style = Ios.type.caption1.copy(fontWeight = FontWeight.SemiBold),
                    color = c.orange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.orange.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        } else {
            null
        },
    )
}
