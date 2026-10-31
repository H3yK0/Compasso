package app.compasso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.compasso.R
import app.compasso.model.Level
import app.compasso.model.ReleaseGate
import app.compasso.model.ReleaseMode
import app.compasso.ui.components.GroupCard
import app.compasso.ui.components.IconSquare
import app.compasso.ui.components.IosButton
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.IosRangeSlider
import app.compasso.ui.components.IosSegmented
import app.compasso.ui.components.IosSlider
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios
import kotlin.math.roundToInt

private val RELEASE_ORDER = listOf(ReleaseMode.ALWAYS, ReleaseMode.CHANCE, ReleaseMode.NEVER)

@Composable
fun CustomLevelScreen(
    initial: Level,
    onSave: (Level) -> Unit,
    onStart: (Level) -> Unit,
    onBack: () -> Unit,
) {
    val c = Ios.colors
    var rounds by rememberSaveable { mutableFloatStateOf(initial.rounds.toFloat()) }
    var goMin by rememberSaveable { mutableFloatStateOf(initial.goMinSec.toFloat()) }
    var goMax by rememberSaveable { mutableFloatStateOf(initial.goMaxSec.toFloat()) }
    var stopMin by rememberSaveable { mutableFloatStateOf(initial.stopMinSec.toFloat()) }
    var stopMax by rememberSaveable { mutableFloatStateOf(initial.stopMaxSec.toFloat()) }
    var bpmLow by rememberSaveable { mutableFloatStateOf(initial.bpmStart.toFloat()) }
    var bpmHigh by rememberSaveable { mutableFloatStateOf(initial.bpmEnd.toFloat()) }
    var release by rememberSaveable { mutableIntStateOf(initial.release.ordinal) }

    val level = initial.copy(
        rounds = rounds.roundToInt(),
        goMinSec = goMin.roundToInt(),
        goMaxSec = goMax.roundToInt(),
        stopMinSec = stopMin.roundToInt(),
        stopMaxSec = stopMax.roundToInt(),
        bpmStart = bpmLow.roundToInt(),
        bpmEnd = bpmHigh.roundToInt(),
        release = ReleaseMode.entries[release],
    ).sanitized()

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LargeTitleList(
            title = stringResource(R.string.level_custom),
            onBack = onBack,
            backLabel = stringResource(R.string.tab_today),
            actions = { NavTextButton(stringResource(R.string.action_save), { onSave(level); onBack() }, bold = true) },
            bottomPadding = 84.dp,
        ) {
            item(key = "resumo") {
                GroupCard(Modifier.padding(top = 6.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconSquare(RowIcon(Icons.Rounded.Tune, c.gray), size = 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.today_fact_minutes, level.estimatedMinutes), style = Ios.type.title2, color = c.label)
                            Text(levelSummary(level, ReleaseGate.Off, withTime = false), style = Ios.type.footnote, color = c.secondary)
                        }
                    }
                }
            }
            section(header = { stringResource(R.string.summary_rounds) }, footer = { stringResource(R.string.custom_rounds_footer) }, key = "rodadas") {
                SliderBlock(roundsText(level.rounds)) {
                    IosSlider(rounds, { rounds = it }, 1f..20f, steps = 18)
                }
            }
            section(header = { stringResource(R.string.custom_go) }, footer = { stringResource(R.string.custom_go_footer) }, key = "ritmo") {
                SliderBlock("${level.goMinSec}–${level.goMaxSec} s") {
                    IosRangeSlider(goMin..goMax, { goMin = it.start; goMax = it.endInclusive }, 10f..180f, steps = 33)
                }
            }
            section(header = { stringResource(R.string.custom_stop) }, footer = { stringResource(R.string.custom_stop_footer) }, key = "pausa") {
                SliderBlock("${level.stopMinSec}–${level.stopMaxSec} s") {
                    IosRangeSlider(stopMin..stopMax, { stopMin = it.start; stopMax = it.endInclusive }, 5f..90f, steps = 16)
                }
            }
            section(
                header = { stringResource(R.string.custom_speed) },
                footer = { stringResource(R.string.custom_speed_footer) },
                key = "velocidade") {
                SliderBlock("${level.bpmStart}–${level.bpmEnd} bpm") {
                    IosRangeSlider(bpmLow..bpmHigh, { bpmLow = it.start; bpmHigh = it.endInclusive }, 40f..200f, steps = 31)
                }
            }
            section(
                header = { stringResource(R.string.custom_end) },
                footer = { when (ReleaseMode.entries[release]) {
                    ReleaseMode.ALWAYS -> stringResource(R.string.custom_end_always)
                    ReleaseMode.CHANCE -> stringResource(R.string.custom_end_chance, level.releaseChance)
                    ReleaseMode.NEVER -> stringResource(R.string.custom_end_never)
                } },
                key = "final") {
                Box(Modifier.padding(12.dp)) {
                    IosSegmented(
                        listOf(stringResource(R.string.custom_release), stringResource(R.string.custom_draw), stringResource(R.string.custom_no_release)),
                        RELEASE_ORDER.indexOf(ReleaseMode.entries[release]),
                        { release = RELEASE_ORDER[it].ordinal },
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(c.navBar)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PrimaryButton(stringResource(R.string.custom_save_start), { onSave(level); onStart(level) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SliderBlock(value: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp)) {
        Text(value, style = Ios.type.headline, color = Ios.colors.accent)
        Spacer(Modifier.height(2.dp))
        content()
    }
}
