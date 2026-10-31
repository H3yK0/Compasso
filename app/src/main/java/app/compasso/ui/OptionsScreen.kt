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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.compasso.BuildConfig
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

    LargeTitleList("Ajustes", bottomPadding = bottomPadding, onBack = onBack, backLabel = "Hoje") {
        if (tip != null) {
            item(key = "dica") {
                TipCard(tip.title, tip.message, Icons.Rounded.Settings, onClose = { actions.onTipClose(tip) }, Modifier.padding(top = 6.dp, bottom = 4.dp))
            }
        }
        section(key = "perfil") {
            IosRow(
                title = codename.name,
                subtitle = "${level.tier.label} · codinome anônimo e cores",
                titleStyle = Ios.type.headline,
                leading = { CodenameAvatar(codename.seed, 46.dp, ring = tierColor(level.tier)) },
                chevron = true,
                onClick = actions.onProfile,
            )
        }
        section(header = "Controle inteligente", footer = pacingFooter(pacing, now), key = "controle") {
            SwitchRow(
                "Controle inteligente",
                req.enabled,
                { onPacing(req.copy(enabled = it)) },
                icon = RowIcon(Icons.Rounded.HourglassTop, c.orange),
            )
            if (req.enabled) {
                RowDivider(IconInset)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                    Text("Intervalo entre liberações", style = Ios.type.body, color = c.label)
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
                IosRow("Cancelar mudança agendada", titleColor = c.accent, onClick = onCancelPending)
            }
        }

        section(header = "Modo foco", footer = focusFooter(focus, now), key = "foco") {
            SwitchRow(
                "Modo foco",
                focus.requested,
                { on -> if (on) actions.onFocus(true) else confirmFocusOff = true },
                subtitle = "Tela fixada e sem saídas durante a sessão",
                icon = RowIcon(Icons.Rounded.PhonelinkLock, c.indigo),
            )
            if (focus.pendingOff) {
                RowDivider()
                IosRow("Cancelar desligamento", titleColor = c.accent, onClick = { actions.onFocus(true) })
            }
        }

        section(header = "Durante a sessão", key = "sessao") {
            SwitchRow(
                "Respiração guiada", settings.breathGuide, { v -> onChange { it.copy(breathGuide = v) } },
                subtitle = "Ondas e texto para respirar nas pausas",
                icon = RowIcon(Icons.Rounded.Air, c.indigo),
            )
            RowDivider(IconInset)
            SwitchRow(
                "Vibrar na respiração", settings.vibrateBreath, { v -> onChange { it.copy(vibrateBreath = v) } },
                subtitle = "Toque leve ao inspirar e ao soltar o ar",
                icon = RowIcon(Icons.Rounded.Vibration, c.purple),
                enabled = settings.breathGuide,
            )
            RowDivider(IconInset)
            SwitchRow(
                "Trilha nas pausas", settings.pauseSound, { v -> onChange { it.copy(pauseSound = v) } },
                subtitle = "Som calmo que entra quando a tela manda parar",
                icon = RowIcon(Icons.Rounded.Spa, c.teal),
            )
            RowDivider(IconInset)
            SwitchRow(
                "Contagem inicial", settings.countdown, { v -> onChange { it.copy(countdown = v) } },
                subtitle = "3 segundos antes da primeira rodada",
                icon = RowIcon(Icons.Rounded.Timer, c.blue),
            )
        }

        section(header = "Sons e vibração", key = "sons") {
            SwitchRow(
                "Som da batida", settings.beatSound, { v -> onChange { it.copy(beatSound = v) } },
                subtitle = "Um “tic” a cada batida do ritmo",
                icon = RowIcon(Icons.Rounded.MusicNote, c.pink),
            )
            RowDivider(IconInset)
            SwitchRow(
                "Som nas trocas", settings.phaseSound, { v -> onChange { it.copy(phaseSound = v) } },
                subtitle = "Aviso ao parar, ao voltar e no final",
                icon = RowIcon(Icons.Rounded.NotificationsActive, c.red),
            )
            RowDivider(IconInset)
            VolumeRow(settings.volume, onVolume = { v -> onChange { it.copy(volume = v) } }, onTest = onTestSound)
            RowDivider(IconInset)
            SwitchRow(
                "Vibrar nas trocas", settings.vibratePhase, { v -> onChange { it.copy(vibratePhase = v) } },
                icon = RowIcon(Icons.Rounded.Vibration, c.purple),
            )
            RowDivider(IconInset)
            SwitchRow(
                "Vibrar na batida", settings.vibrateBeat, { v -> onChange { it.copy(vibrateBeat = v) } },
                icon = RowIcon(Icons.Rounded.GraphicEq, c.teal),
            )
        }

        section(
            header = "Trilha sonora",
            footer = "A trilha é gerada pelo próprio app, ao vivo: acordes lentos, um mar suave que cresce quando você inspira e sinos raros. Usa o volume de mídia do celular.",
            key = "trilha",
        ) {
            SwitchRow(
                "Trilha no Respirar", settings.breatheSound, { v -> onChange { it.copy(breatheSound = v) } },
                icon = RowIcon(Icons.Rounded.Air, c.indigo),
            )
            RowDivider(IconInset)
            AmbientVolumeRow(settings.ambientVolume) { v -> onChange { it.copy(ambientVolume = v) } }
        }

        section(
            header = "Privacidade",
            footer = "Mesmo com o histórico desligado ou apagado, o app guarda, só neste celular, a data e o tipo das sessões dos " +
                "últimos 8 dias (para o controle inteligente) e a data dos “não segurei” e das sessões interrompidas " +
                "(para a sequência e o nível Invicto).",
            key = "privacidade",
        ) {
            SwitchRow(
                "Senha ao abrir", hasPin, { on -> if (on) onSetPin() else onRemovePin() },
                subtitle = if (hasPin) "Pede a senha ao abrir e ao voltar depois de 30 s" else null,
                icon = RowIcon(Icons.Rounded.Lock, c.gray),
            )
            if (hasPin) {
                RowDivider(IconInset)
                IosRow("Trocar senha", icon = RowIcon(Icons.Rounded.Key, c.gray), chevron = true, onClick = onSetPin)
            }
            RowDivider(IconInset)
            SwitchRow(
                "Modo discreto", settings.discreet, { v -> onChange { it.copy(discreet = v) } },
                subtitle = "Esconde o app nos recentes e bloqueia prints",
                icon = RowIcon(Icons.Rounded.VisibilityOff, c.indigo),
            )
            RowDivider(IconInset)
            SwitchRow(
                "Guardar histórico", settings.keepHistory, { v -> onChange { it.copy(keepHistory = v) } },
                icon = RowIcon(Icons.Rounded.History, c.blue),
            )
        }

        if (historyCount > 0) {
            section(key = "apagar") {
                IosRow(
                    "Apagar histórico",
                    titleColor = c.red,
                    value = if (historyCount == 1) "1 sessão" else "$historyCount sessões",
                    onClick = { confirmClear = true },
                )
            }
        }

        section(header = "Aparência", key = "aparencia") {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                Text("Visual", style = Ios.type.body, color = c.label)
                Spacer(Modifier.height(10.dp))
                IosSegmented(
                    listOf("Automático", "Dia", "Noite"),
                    ThemeMode.entries.indexOf(settings.theme),
                    { i -> onChange { it.copy(theme = ThemeMode.entries[i]) } },
                )
            }
            RowDivider()
            IosRow(
                "Cor de destaque",
                icon = RowIcon(Icons.Rounded.Palette, c.accent),
                value = settings.accent.label,
                chevron = true,
                onClick = actions.onProfile,
            )
        }

        section(header = "Ajuda", key = "ajuda") {
            IosRow("Como usar o Compasso", icon = RowIcon(Icons.AutoMirrored.Rounded.HelpOutline, c.blue), chevron = true, onClick = actions.onGuide)
            RowDivider(IconInset)
            IosRow("Rever a apresentação", icon = RowIcon(Icons.Rounded.PlayCircle, c.teal), chevron = true, onClick = actions.onReplayIntro)
            RowDivider(IconInset)
            SwitchRow(
                "Dicas pelo caminho", settings.showTips, { v -> onChange { it.copy(showTips = v) } },
                subtitle = "Mostra uma dica na primeira visita a cada parte",
                icon = RowIcon(Icons.Rounded.Lightbulb, c.yellow),
            )
            RowDivider(IconInset)
            IosRow("Mostrar as dicas de novo", icon = RowIcon(Icons.Rounded.AutoAwesome, c.orange), onClick = actions.onResetTips)
        }

        section(
            header = "Sobre",
            footer = "O app não tem acesso à internet. Tudo fica guardado só neste celular.",
            key = "sobre",
        ) {
            IosRow("Versão", value = BuildConfig.VERSION_NAME)
            RowDivider()
            IosRow("Licença e créditos", icon = RowIcon(Icons.Rounded.Gavel, c.gray), chevron = true, onClick = actions.onAbout)
        }
    }

    if (confirmClear) {
        IosAlert(
            title = "Apagar histórico?",
            message = "As sessões salvas serão apagadas. Continuam guardadas só a data das sessões dos últimos 8 dias (controle inteligente) " +
                "e a dos “não segurei” e sessões interrompidas (sequência e Invicto).",
            actions = listOf(
                AlertAction("Cancelar", { confirmClear = false }),
                AlertAction("Apagar", { confirmClear = false; onClearHistory() }, destructive = true),
            ),
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmFocusOff) {
        IosAlert(
            title = "Desligar o modo foco?",
            message = "Ele desliga só daqui a 24 h, pra uma vontade do momento não desfazer o que você decidiu com calma. Sem ele, as sessões não valem pontos.",
            actions = listOf(
                AlertAction("Manter ligado", { confirmFocusOff = false }, bold = true),
                AlertAction("Desligar em 24 h", { confirmFocusOff = false; actions.onFocus(false) }, destructive = true),
            ),
            onDismiss = { confirmFocusOff = false },
        )
    }
}

private fun focusFooter(f: FocusSettings, now: Long): String = when {
    f.pendingOff -> "O modo foco desliga ${formatUntil(f.offAt, now)}. Até lá, continua valendo."
    f.enabled -> "Antes de cada sessão o app pede para fixar a tela (recurso do Android): não dá para abrir outro app, a tela dividida e as notificações ficam bloqueadas e vídeos flutuantes são fechados. Janelas de outros apps somem. Sair por mais de 10 s, ou 3 vezes, encerra a sessão como interrompida."
    else -> "Desligado: a sessão só pausa quando você sai do app, e as sessões não valem pontos."
}

private fun pacingFooter(p: PacingSettings, now: Long): String {
    val rule = "Deixar o controle mais leve só vale 24 h depois, pra uma vontade do momento não desfazer o que você decidiu com calma. Mais firme vale na hora."
    val pending = p.pending
    return when {
        pending != null -> {
            val what = if (!pending.target.enabled) "O controle desliga" else "O intervalo passa para ${pending.target.intervalHours} h"
            "$what ${formatUntil(pending.effectiveAt, now)}. Até lá, vale o intervalo de ${p.config.intervalHours} h. $rule"
        }
        p.config.enabled -> "Liberação no máximo a cada ${p.config.intervalHours} h, com regras extras para sessões seguidas e semana cheia. $rule"
        else -> "Desligado: cada sessão segue o nível escolhido."
    }
}

@Composable
private fun AmbientVolumeRow(volume: Float, onVolume: (Float) -> Unit) {
    val c = Ios.colors
    var v by remember(volume) { mutableFloatStateOf(volume) }
    Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 4.dp)) {
        Text("Volume da trilha", style = Ios.type.body, color = c.label)
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
            Text("Volume dos sons", style = Ios.type.body, color = c.label, modifier = Modifier.weight(1f))
            NavTextButton("Testar", onTest)
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
