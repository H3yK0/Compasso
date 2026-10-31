package app.compasso.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.compasso.BuildConfig
import app.compasso.R
import app.compasso.data.AppLanguage
import app.compasso.data.Settings
import app.compasso.data.ThemeMode
import app.compasso.model.Codename
import app.compasso.model.FocusSettings
import app.compasso.model.LevelStatus
import app.compasso.model.Pacing
import app.compasso.model.PacingConfig
import app.compasso.model.PacingSettings
import app.compasso.model.Tip
import app.compasso.ui.components.CodenameAvatar
import app.compasso.ui.components.TipCard
import app.compasso.ui.theme.tierColor
import app.compasso.ui.components.AlertAction
import app.compasso.ui.components.IconInset
import app.compasso.ui.components.IosAlert
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.IosSegmented
import app.compasso.ui.components.IosSlider
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.SwitchRow
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios

/** Ações da aba Ajustes (agrupadas para a assinatura não ficar enorme). */
class OptionsActions(
    val onChange: ((Settings) -> Settings) -> Unit = {},
    val onPacing: (PacingConfig) -> Unit = {},
    val onCancelPending: () -> Unit = {},
    val onFocus: (Boolean) -> Unit = {},
    val onTestSound: () -> Unit = {},
    val onSetPin: () -> Unit = {},
    val onRemovePin: () -> Unit = {},
    val onClearHistory: () -> Unit = {},
    val onGuide: () -> Unit = {},
    val onReplayIntro: () -> Unit = {},
    val onResetTips: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onAbout: () -> Unit = {},
    val onTipClose: (Tip) -> Unit = {},
)

@Composable
fun OptionsScreen(
    settings: Settings,
    pacing: PacingSettings,
    focus: FocusSettings,
    hasPin: Boolean,
    historyCount: Int,
    codename: Codename,
    level: LevelStatus,
    tip: Tip?,
    actions: OptionsActions,
    bottomPadding: Dp = 0.dp,
    now: Long = System.currentTimeMillis(),
    onBack: (() -> Unit)? = null,
) {
    val c = Ios.colors
    var confirmClear by remember { mutableStateOf(false) }
    var confirmFocusOff by remember { mutableStateOf(false) }
    val req = pacing.requested
    val onChange = actions.onChange
    val onPacing = actions.onPacing
    val onCancelPending = actions.onCancelPending
    val onTestSound = actions.onTestSound
    val onSetPin = actions.onSetPin
    val onRemovePin = actions.onRemovePin
    val onClearHistory = actions.onClearHistory

    LargeTitleList(stringResource(R.string.nav_settings), bottomPadding = bottomPadding, onBack = onBack, backLabel = stringResource(R.string.tab_today)) {
        if (tip != null) {
            item(key = "dica") {
                TipCard(stringResource(tip.title), stringResource(tip.message), Icons.Rounded.Settings, onClose = { actions.onTipClose(tip) }, Modifier.padding(top = 6.dp, bottom = 4.dp))
            }
        }
        section(key = "perfil") {
            IosRow(
                title = codenameText(codename),
                subtitle = stringResource(R.string.settings_profile_sub, stringResource(level.tier.label)),
                titleStyle = Ios.type.headline,
                leading = { CodenameAvatar(codename.seed, 46.dp, ring = tierColor(level.tier)) },
                chevron = true,
                onClick = actions.onProfile,
            )
        }
        section(header = { stringResource(R.string.smart_control) }, footer = { pacingFooter(pacing, now) }, key = "controle") {
            SwitchRow(
                stringResource(R.string.smart_control),
                req.enabled,
                { onPacing(req.copy(enabled = it)) },
                icon = RowIcon(Icons.Rounded.HourglassTop, c.orange),
            )
            if (req.enabled) {
                RowDivider(IconInset)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                    Text(stringResource(R.string.settings_interval), style = Ios.type.body, color = c.label)
                    Spacer(Modifier.height(10.dp))
                    IosSegmented(
                        Pacing.INTERVAL_OPTIONS.map { "$it h" },
                        Pacing.INTERVAL_OPTIONS.indexOf(req.intervalHours),
                        { onPacing(req.copy(intervalHours = Pacing.INTERVAL_OPTIONS[it])) },
                    )
                }
            }
            if (pacing.pending != null) {
                RowDivider()
                IosRow(stringResource(R.string.settings_cancel_pending), titleColor = c.accent, onClick = onCancelPending)
            }
        }

        section(header = { stringResource(R.string.focus_title) }, footer = { focusFooter(focus, now) }, key = "foco") {
            SwitchRow(
                stringResource(R.string.focus_title),
                focus.requested,
                { on -> if (on) actions.onFocus(true) else confirmFocusOff = true },
                subtitle = stringResource(R.string.settings_focus_sub),
                icon = RowIcon(Icons.Rounded.PhonelinkLock, c.indigo),
            )
            if (focus.pendingOff) {
                RowDivider()
                IosRow(stringResource(R.string.settings_cancel_off), titleColor = c.accent, onClick = { actions.onFocus(true) })
            }
        }

        section(header = { stringResource(R.string.settings_during) }, key = "sessao") {
            SwitchRow(
                stringResource(R.string.settings_breath_guide), settings.breathGuide, { v -> onChange { it.copy(breathGuide = v) } },
                subtitle = stringResource(R.string.settings_breath_guide_sub),
                icon = RowIcon(Icons.Rounded.Air, c.indigo),
            )
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_vibrate_breath), settings.vibrateBreath, { v -> onChange { it.copy(vibrateBreath = v) } },
                subtitle = stringResource(R.string.settings_vibrate_breath_sub),
                icon = RowIcon(Icons.Rounded.Vibration, c.purple),
                enabled = settings.breathGuide,
            )
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_pause_sound), settings.pauseSound, { v -> onChange { it.copy(pauseSound = v) } },
                subtitle = stringResource(R.string.settings_pause_sound_sub),
                icon = RowIcon(Icons.Rounded.Spa, c.teal),
            )
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_countdown), settings.countdown, { v -> onChange { it.copy(countdown = v) } },
                subtitle = stringResource(R.string.settings_countdown_sub),
                icon = RowIcon(Icons.Rounded.Timer, c.blue),
            )
        }

        section(header = { stringResource(R.string.settings_sounds) }, key = "sons") {
            SwitchRow(
                stringResource(R.string.settings_beat_sound), settings.beatSound, { v -> onChange { it.copy(beatSound = v) } },
                subtitle = stringResource(R.string.settings_beat_sound_sub),
                icon = RowIcon(Icons.Rounded.MusicNote, c.pink),
            )
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_phase_sound), settings.phaseSound, { v -> onChange { it.copy(phaseSound = v) } },
                subtitle = stringResource(R.string.settings_phase_sound_sub),
                icon = RowIcon(Icons.Rounded.NotificationsActive, c.red),
            )
            RowDivider(IconInset)
            VolumeRow(settings.volume, onVolume = { v -> onChange { it.copy(volume = v) } }, onTest = onTestSound)
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_vibrate_phase), settings.vibratePhase, { v -> onChange { it.copy(vibratePhase = v) } },
                icon = RowIcon(Icons.Rounded.Vibration, c.purple),
            )
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_vibrate_beat), settings.vibrateBeat, { v -> onChange { it.copy(vibrateBeat = v) } },
                icon = RowIcon(Icons.Rounded.GraphicEq, c.teal),
            )
        }

        section(
            header = { stringResource(R.string.settings_soundtrack) },
            footer = { stringResource(R.string.settings_soundtrack_footer) },
            key = "trilha") {
            SwitchRow(
                stringResource(R.string.settings_breathe_sound), settings.breatheSound, { v -> onChange { it.copy(breatheSound = v) } },
                icon = RowIcon(Icons.Rounded.Air, c.indigo),
            )
            RowDivider(IconInset)
            AmbientVolumeRow(settings.ambientVolume) { v -> onChange { it.copy(ambientVolume = v) } }
        }

        section(
            header = { stringResource(R.string.settings_privacy) },
            footer = { stringResource(R.string.settings_privacy_footer) },
            key = "privacidade") {
            SwitchRow(
                stringResource(R.string.settings_pin), hasPin, { on -> if (on) onSetPin() else onRemovePin() },
                subtitle = if (hasPin) stringResource(R.string.settings_pin_sub) else null,
                icon = RowIcon(Icons.Rounded.Lock, c.gray),
            )
            if (hasPin) {
                RowDivider(IconInset)
                IosRow(stringResource(R.string.settings_change_pin), icon = RowIcon(Icons.Rounded.Key, c.gray), chevron = true, onClick = onSetPin)
            }
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_discreet), settings.discreet, { v -> onChange { it.copy(discreet = v) } },
                subtitle = stringResource(R.string.settings_discreet_sub),
                icon = RowIcon(Icons.Rounded.VisibilityOff, c.indigo),
            )
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_keep_history), settings.keepHistory, { v -> onChange { it.copy(keepHistory = v) } },
                icon = RowIcon(Icons.Rounded.History, c.blue),
            )
        }

        if (historyCount > 0) {
            section(key = "apagar") {
                IosRow(
                    stringResource(R.string.settings_clear_history),
                    titleColor = c.red,
                    value = pluralStringResource(R.plurals.sessions_count, historyCount, historyCount),
                    onClick = { confirmClear = true },
                )
            }
        }

        section(header = { stringResource(R.string.settings_appearance) }, key = "aparencia") {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                Text(stringResource(R.string.settings_look), style = Ios.type.body, color = c.label)
                Spacer(Modifier.height(10.dp))
                IosSegmented(
                    listOf(stringResource(R.string.settings_look_auto), stringResource(R.string.settings_look_day), stringResource(R.string.settings_look_night)),
                    ThemeMode.entries.indexOf(settings.theme),
                    { i -> onChange { it.copy(theme = ThemeMode.entries[i]) } },
                )
            }
            RowDivider()
            IosRow(
                stringResource(R.string.settings_accent),
                icon = RowIcon(Icons.Rounded.Palette, c.accent),
                value = stringResource(settings.accent.label),
                chevron = true,
                onClick = actions.onProfile,
            )
        }

        section(header = { stringResource(R.string.settings_language) }, footer = { stringResource(R.string.settings_language_footer) }, key = "idioma") {
            AppLanguage.entries.forEachIndexed { i, lang ->
                if (i > 0) RowDivider()
                IosRow(
                    languageName(lang),
                    trailing = if (settings.language == lang) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp)) }
                    } else {
                        null
                    },
                    onClick = { onChange { it.copy(language = lang) } },
                )
            }
        }

        section(header = { stringResource(R.string.settings_help) }, key = "ajuda") {
            IosRow(stringResource(R.string.settings_guide), icon = RowIcon(Icons.AutoMirrored.Rounded.HelpOutline, c.blue), chevron = true, onClick = actions.onGuide)
            RowDivider(IconInset)
            IosRow(stringResource(R.string.settings_replay_intro), icon = RowIcon(Icons.Rounded.PlayCircle, c.teal), chevron = true, onClick = actions.onReplayIntro)
            RowDivider(IconInset)
            SwitchRow(
                stringResource(R.string.settings_tips), settings.showTips, { v -> onChange { it.copy(showTips = v) } },
                subtitle = stringResource(R.string.settings_tips_sub),
                icon = RowIcon(Icons.Rounded.Lightbulb, c.yellow),
            )
            RowDivider(IconInset)
            IosRow(stringResource(R.string.settings_reset_tips), icon = RowIcon(Icons.Rounded.AutoAwesome, c.orange), onClick = actions.onResetTips)
        }

        section(
            header = { stringResource(R.string.settings_about) },
            footer = { stringResource(R.string.settings_about_footer) },
            key = "sobre") {
            IosRow(stringResource(R.string.settings_version), value = BuildConfig.VERSION_NAME)
            RowDivider()
            IosRow(stringResource(R.string.about_title), icon = RowIcon(Icons.Rounded.Gavel, c.gray), chevron = true, onClick = actions.onAbout)
        }
    }

    if (confirmClear) {
        IosAlert(
            title = stringResource(R.string.clear_title),
            message = stringResource(R.string.clear_text),
            actions = listOf(
                AlertAction(stringResource(R.string.action_cancel), { confirmClear = false }),
                AlertAction(stringResource(R.string.clear_confirm), { confirmClear = false; onClearHistory() }, destructive = true),
            ),
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmFocusOff) {
        IosAlert(
            title = stringResource(R.string.focus_off_title),
            message = stringResource(R.string.focus_off_text),
            actions = listOf(
                AlertAction(stringResource(R.string.focus_off_keep), { confirmFocusOff = false }, bold = true),
                AlertAction(stringResource(R.string.focus_off_confirm), { confirmFocusOff = false; actions.onFocus(false) }, destructive = true),
            ),
            onDismiss = { confirmFocusOff = false },
        )
    }
}

@Composable
private fun focusFooter(f: FocusSettings, now: Long): String = when {
    f.pendingOff -> stringResource(R.string.focus_footer_pending, formatUntil(f.offAt, now))
    f.enabled -> stringResource(R.string.focus_footer_on)
    else -> stringResource(R.string.focus_footer_off)
}

@Composable
private fun pacingFooter(p: PacingSettings, now: Long): String {
    val rule = stringResource(R.string.pacing_footer_rule)
    val pending = p.pending
    return when {
        pending != null -> {
            val `when` = formatUntil(pending.effectiveAt, now)
            val what = if (!pending.target.enabled) {
                stringResource(R.string.pacing_footer_off_at, `when`)
            } else {
                stringResource(R.string.pacing_footer_interval_at, pending.target.intervalHours, `when`)
            }
            stringResource(R.string.pacing_footer_pending, what, p.config.intervalHours, rule)
        }
        p.config.enabled -> stringResource(R.string.pacing_footer_on, p.config.intervalHours, rule)
        else -> stringResource(R.string.gate_off_text) + "."
    }
}

@Composable
fun languageName(lang: AppLanguage): String = when (lang) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    // cada idioma aparece no próprio nome, para quem não lê o idioma atual achar o seu
    AppLanguage.PT -> "Português"
    AppLanguage.EN -> "English"
    AppLanguage.ES -> "Español"
}

@Composable
private fun AmbientVolumeRow(volume: Float, onVolume: (Float) -> Unit) {
    val c = Ios.colors
    var v by remember(volume) { mutableFloatStateOf(volume) }
    Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 4.dp)) {
        Text(stringResource(R.string.settings_ambient_volume), style = Ios.type.body, color = c.label)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.VolumeMute, contentDescription = null, tint = c.secondary, modifier = Modifier.size(18.dp))
            IosSlider(
                value = v,
                onValueChange = { v = it },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                onValueChangeFinished = { onVolume(v) },
            )
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = c.secondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun VolumeRow(volume: Float, onVolume: (Float) -> Unit, onTest: () -> Unit) {
    val c = Ios.colors
    var v by remember(volume) { mutableFloatStateOf(volume) }
    Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_volume), style = Ios.type.body, color = c.label, modifier = Modifier.weight(1f))
            NavTextButton(stringResource(R.string.settings_test), onTest)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.VolumeMute, contentDescription = null, tint = c.secondary, modifier = Modifier.size(18.dp))
            IosSlider(
                value = v,
                onValueChange = { v = it },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                onValueChangeFinished = { onVolume(v) },
            )
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = c.secondary, modifier = Modifier.size(20.dp))
        }
    }
}
