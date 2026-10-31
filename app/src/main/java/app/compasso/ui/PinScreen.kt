package app.compasso.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.theme.Ios
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 4
private val KEY_LETTERS = mapOf(
    '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL",
    '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ",
)

@Composable
fun PinPad(
    title: String,
    subtitle: String?,
    error: String?,
    errorTick: Int,
    onComplete: (String) -> Unit,
    enabled: Boolean = true,
    bottom: @Composable () -> Unit = {},
) {
    val c = Ios.colors
    var entered by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    // Senha errada: os pontos tremem, como no iPhone.
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            for (x in listOf(-18f, 16f, -12f, 9f, -5f, 2f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    fun type(ch: Char) {
        if (!enabled || entered.length >= PIN_LENGTH) return
        entered += ch
        if (entered.length == PIN_LENGTH) {
            val pin = entered
            scope.launch {
                delay(140) // deixa o 4º ponto aparecer antes de conferir
                entered = ""
                onComplete(pin)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.label, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = Ios.type.title3.copy(fontWeight = FontWeight.Normal), color = c.label, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = Ios.type.footnote, color = c.secondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.graphicsLayer { translationX = shake.value.dp.toPx() },
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            repeat(PIN_LENGTH) { i ->
                val filled = i < entered.length
                Box(
                    Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .then(if (filled) Modifier.background(c.label) else Modifier.border(1.5.dp, c.label, CircleShape))
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(error ?: " ", style = Ios.type.footnote, color = c.red, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Keypad(onDigit = ::type, onBackspace = { entered = entered.dropLast(1) }, showDelete = entered.isNotEmpty())
        Spacer(Modifier.height(12.dp))
        bottom()
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, showDelete: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                row.forEach { ch -> Key(ch) { onDigit(ch) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(78.dp))
            Key('0') { onDigit('0') }
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                if (showDelete) NavTextButton("Apagar", onBackspace)
            }
        }
    }
}

@Composable
private fun Key(ch: Char, onClick: () -> Unit) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(if (pressed) c.pressed else c.key)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                ch.toString(),
                style = Ios.type.figure.copy(fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 38.sp),
                color = c.label,
            )
            val letters = KEY_LETTERS[ch]
            if (letters != null) {
                Text(letters, style = Ios.type.caption2.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.18.em), color = c.label)
            }
        }
    }
}

@Composable
fun PinUnlockScreen(onTry: (String) -> Boolean) {
    var error by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var waiting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    PinPad(
        title = "Digite sua senha",
        subtitle = null,
        error = if (waiting) "Muitas tentativas. Aguarde 30 segundos." else error,
        errorTick = errorTick,
        enabled = !waiting,
        onComplete = { pin ->
            if (onTry(pin)) {
                error = null
                attempts = 0
            } else {
                attempts++
                error = "Senha incorreta"
                errorTick++
                if (attempts % 5 == 0) {
                    waiting = true
                    scope.launch {
                        delay(30_000)
                        waiting = false
                        error = null
                    }
                }
            }
        },
    )
}

@Composable
fun PinSetupScreen(firstRun: Boolean, onDone: (String) -> Unit, onSkip: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableIntStateOf(0) }
    val c = Ios.colors
    PinPad(
        title = if (first == null) "Crie uma senha" else "Confirme a senha",
        subtitle = if (first == null) "4 dígitos. O app vai pedir essa senha sempre que for aberto." else "Digite a mesma senha de novo.",
        error = error,
        errorTick = errorTick,
        onComplete = { pin ->
            val f = first
            when {
                f == null -> {
                    first = pin
                    error = null
                }
                f == pin -> onDone(pin)
                else -> {
                    first = null
                    error = "As senhas não batem. Comece de novo."
                    errorTick++
                }
            }
        },
        bottom = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NavTextButton(if (firstRun) "Agora não" else "Cancelar", onSkip)
                Text(
                    if (firstRun) "Dá pra ativar depois em Ajustes. Se esquecer a senha, só apagando os dados do app."
                    else "Se esquecer a senha, só apagando os dados do app.",
                    style = Ios.type.caption1,
                    color = c.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}
