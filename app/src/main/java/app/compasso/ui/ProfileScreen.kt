package app.compasso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.compasso.model.Accent
import app.compasso.model.Codename
import app.compasso.model.LevelStatus
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.CodenameAvatar
import app.compasso.ui.components.GroupCard
import app.compasso.ui.components.GroupFooter
import app.compasso.ui.components.GroupHeader
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.TierMedal
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.accentColors
import app.compasso.ui.theme.tierColor

/** A cor escolhida só vale se o nível atual a libera; senão, volta para a padrão. */
fun effectiveAccent(chosen: Accent, level: LevelStatus): Accent =
    if (chosen.unlock.ordinal <= level.tier.ordinal) chosen else Accent.TEAL

/** Codinome anônimo e cores de destaque (as regalias de cada nível). */
@Composable
fun ProfileScreen(
    codename: Codename,
    level: LevelStatus,
    accent: Accent,
    onNewCodename: () -> Unit,
    onAccent: (Accent) -> Unit,
    onBack: () -> Unit,
    backLabel: String = "Voltar",
) {
    val c = Ios.colors
    val dark = c.dark
    LargeTitleList("Perfil", onBack = onBack, backLabel = backLabel) {
        item(key = "cabecalho") {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CodenameAvatar(codename.seed, 116.dp, ring = tierColor(level.tier))
                Spacer(Modifier.height(14.dp))
                Text(codename.name, style = Ios.type.title2, color = c.label, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierMedal(level.tier, 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("${level.tier.label} · ${level.windowPoints} pts em 28 dias", style = Ios.type.subhead, color = c.secondary)
                }
                Spacer(Modifier.height(14.dp))
                IosButton(
                    "Sortear outro codinome", onNewCodename,
                    kind = ButtonKind.Tinted, height = 40.dp, shape = CircleShape, icon = Icons.Rounded.Shuffle,
                )
                GroupFooter(
                    "O codinome e o emblema são criados pelo app e não identificam você. Ficam só neste celular.",
                    Modifier.padding(top = 4.dp),
                )
            }
        }
        item(key = "cores") {
            Column {
                GroupHeader("Cores de destaque")
                GroupCard {
                    val rows = Accent.entries.chunked(4)
                    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        rows.forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                row.forEach { a ->
                                    val unlocked = a.unlock.ordinal <= level.tier.ordinal
                                    val selected = a == accent && unlocked
                                    val col = accentColors(a).let { if (dark) it.dark else it.light }
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                enabled = unlocked,
                                                role = Role.RadioButton,
                                            ) { onAccent(a) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Box(
                                            Modifier
                                                .size(48.dp)
                                                .then(if (selected) Modifier.border(2.5.dp, col, CircleShape).padding(5.dp) else Modifier)
                                                .clip(CircleShape)
                                                .background(if (unlocked) col else col.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            when {
                                                selected -> Icon(Icons.Rounded.Check, contentDescription = "Escolhida", tint = Color.White, modifier = Modifier.size(20.dp))
                                                !unlocked -> Icon(Icons.Rounded.Lock, contentDescription = "Bloqueada", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(a.label, style = Ios.type.caption1, color = if (unlocked) c.label else c.tertiary)
                                        Text(a.unlock.label, style = Ios.type.caption2, color = c.secondary)
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                GroupFooter(
                    "Cada nível libera uma cor enquanto você estiver nele. Se o nível cair, o app volta para a cor padrão até você subir de novo.",
                )
            }
        }
    }
}
