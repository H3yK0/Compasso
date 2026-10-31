package app.compasso.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.BackHand
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import app.compasso.R
import app.compasso.model.Progress
import app.compasso.ui.components.Chevron
import app.compasso.ui.components.IconInset
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.PhaseColors

private data class Topic(@StringRes val title: Int, val icon: ImageVector, val tint: @Composable () -> Color, @StringRes val text: Int)

private val topics = listOf(
    Topic(R.string.guide_training, Icons.Rounded.GraphicEq, { PhaseColors.go[0] }, R.string.guide_training_text),
    Topic(R.string.guide_close, Icons.Rounded.BackHand, { PhaseColors.stop[0] }, R.string.guide_close_text),
    Topic(R.string.guide_levels, Icons.Rounded.Layers, { Ios.colors.teal }, R.string.guide_levels_text),
    Topic(R.string.smart_control, Icons.Rounded.HourglassTop, { Ios.colors.orange }, R.string.guide_pacing_text),
    Topic(R.string.focus_title, Icons.Rounded.PhonelinkLock, { Ios.colors.indigo }, R.string.guide_focus_text),
    Topic(R.string.guide_refuge, Icons.Rounded.Air, { Ios.colors.indigo }, R.string.guide_refuge_text),
    Topic(R.string.guide_points, Icons.Rounded.EmojiEvents, { Ios.colors.yellow }, R.string.guide_points_text),
    Topic(R.string.guide_reading, Icons.AutoMirrored.Rounded.MenuBook, { Ios.colors.blue }, R.string.guide_reading_text),
    Topic(R.string.settings_privacy, Icons.Rounded.Shield, { Ios.colors.gray }, R.string.guide_privacy_text),
)

/** "Como usar": sempre disponível nos ajustes. */
@Composable
fun GuideScreen(onReplayIntro: () -> Unit, onBack: () -> Unit, backLabel: String = stringResource(R.string.nav_settings)) {
    val c = Ios.colors
    var open by rememberSaveable { mutableStateOf(-1) }
    LargeTitleList(stringResource(R.string.guide_title), onBack = onBack, backLabel = backLabel) {
        section(key = "apresentacao") {
            IosRow(
                stringResource(R.string.settings_replay_intro),
                subtitle = stringResource(R.string.guide_replay_sub),
                icon = RowIcon(Icons.Rounded.PlayCircle, c.teal),
                chevron = true,
                onClick = onReplayIntro,
            )
        }
        section(header = { stringResource(R.string.guide_header) }, footer = { stringResource(R.string.guide_footer) }, key = "temas") {
            topics.forEachIndexed { i, t ->
                if (i > 0) RowDivider(IconInset)
                val expanded = open == i
                val angle by animateFloatAsState(if (expanded) 90f else 0f, label = "seta")
                IosRow(
                    stringResource(t.title),
                    icon = RowIcon(t.icon, t.tint()),
                    onClick = { open = if (expanded) -1 else i },
                    trailing = { Chevron(c.tertiary, Modifier.size(width = 8.dp, height = 13.dp).rotate(angle)) },
                )
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(Modifier.padding(start = IconInset, end = 16.dp, bottom = 14.dp)) {
                        Text(stringResource(t.text, Progress.DAILY_CAP), style = Ios.type.subhead.copy(lineHeight = 21.sp), color = c.secondary)
                    }
                }
            }
        }
    }
}
