package app.compasso.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.compasso.ui.theme.Ios

data class TabItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon)

/** Altura da barra de abas (sem contar a barra de navegação do sistema). */
val TabBarHeight = 50.dp

/** Barra de abas no estilo do iPhone: ícone + rótulo, a aba atual na cor de destaque. */
@Composable
fun IosTabBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val haptic = LocalHapticFeedback.current
    Column(modifier.fillMaxWidth().background(c.navBar.copy(alpha = 1f))) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
        Row(Modifier.fillMaxWidth().height(TabBarHeight), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, item ->
                val isSel = i == selected
                val tint = if (isSel) c.accent else c.gray
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSel,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) {
                            if (!isSel) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        }
                        .padding(top = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    Icon(if (isSel) item.selectedIcon else item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        style = Ios.type.caption2.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
