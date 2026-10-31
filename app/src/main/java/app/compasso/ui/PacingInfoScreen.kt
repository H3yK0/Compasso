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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.compasso.R
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
            Text(stringResource(R.string.smart_control), style = Ios.type.headline, color = c.label, modifier = Modifier.align(Alignment.Center))
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) { NavTextButton(stringResource(R.string.action_ok), onDone, bold = true) }
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
            section(header = { stringResource(R.string.pacing_now) }, key = "agora") {
                IosRow(stringResource(R.string.pacing_last_release), value = snapshot.lastRelease?.let { formatPast(it, now) } ?: stringResource(R.string.pacing_none_recent))
                RowDivider()
                IosRow(stringResource(R.string.pacing_week_sessions), value = "${snapshot.sessionsThisWeek}")
                RowDivider()
                IosRow(
                    stringResource(R.string.pacing_current_interval),
                    value = if (gate == ReleaseGate.Off) stringResource(R.string.pacing_off) else if (busy) "${hours * 2} h" else "$hours h",
                )
            }
            section(
                header = { stringResource(R.string.pacing_rules) },
                footer = { stringResource(R.string.pacing_rules_footer) },
                key = "regras") {
                RuleRow(
                    RowIcon(Icons.Rounded.HourglassTop, c.orange), stringResource(R.string.pacing_rule_interval),
                    stringResource(R.string.pacing_rule_interval_text, hours),
                    active = GateRule.INTERVAL in rules,
                )
                RowDivider(IconInset)
                RuleRow(
                    RowIcon(Icons.Rounded.Bolt, c.pink), stringResource(R.string.pacing_rule_rush),
                    stringResource(R.string.pacing_rule_rush_text),
                    active = GateRule.RUSH in rules,
                )
                RowDivider(IconInset)
                RuleRow(
                    RowIcon(Icons.Rounded.CalendarMonth, c.blue), stringResource(R.string.pacing_rule_busy),
                    stringResource(R.string.pacing_rule_busy_text, Pacing.BUSY_WEEK_SESSIONS),
                    active = busy,
                )
            }
            section(key = "ajustar") {
                IosRow(stringResource(R.string.pacing_adjust), titleColor = c.accent, chevron = true, onClick = onSettings)
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
                    stringResource(R.string.pacing_active),
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
