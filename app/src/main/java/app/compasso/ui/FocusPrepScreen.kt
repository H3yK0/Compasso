package app.compasso.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import app.compasso.ui.components.GlassCard
import app.compasso.ui.components.GlowOrb
import app.compasso.ui.components.NavTextButton
import app.compasso.ui.components.PrimaryButton
import app.compasso.ui.components.SkyBackground
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.compasso.ui.components.ButtonKind
import app.compasso.ui.components.GlassCircleButton
import app.compasso.ui.components.IosButton
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.PhaseColors

/**
 * Antes da sessão, com o modo foco: o Android pergunta se pode fixar a tela. Assim que a pessoa
 * confirma, a sessão começa sozinha.
 */
@Composable
fun FocusPrepScreen(onPin: () -> Unit, onWithoutPin: () -> Unit, onCancel: () -> Unit) {
    val c = Ios.colors
    val t = rememberInfiniteTransition(label = "foco")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulso")
    SkyBackground(Modifier.fillMaxSize(), glow = c.lavender, glowStrength = 1.4f) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                GlassCircleButton(Icons.Rounded.Close, "Cancelar", onCancel, size = 40.dp, iconSize = 20.dp)
            }
            Spacer(Modifier.weight(1f))
            GlowOrb(Icons.Rounded.PhonelinkLock, c.lavender, size = 180.dp, pulse = pulse)
            Spacer(Modifier.height(6.dp))
            Text("Modo foco", style = Ios.type.title1, color = c.label)
            Spacer(Modifier.height(8.dp))
            Text(
                "Para começar, fixe a tela. O Android vai perguntar se pode fixar o Compasso: confirme e a sessão começa sozinha.",
                style = Ios.type.body,
                color = c.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                FocusPoint(Icons.Rounded.Apps, "Outros apps, notificações e tela dividida ficam bloqueados.")
                FocusPoint(Icons.Rounded.PictureInPictureAlt, "Vídeos e janelas flutuantes são fechados ou escondidos.")
                FocusPoint(Icons.Rounded.LockOpen, "No fim da sessão, o app solta a tela sozinho.")
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton("Fixar a tela", onPin, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            NavTextButton("Começar sem fixar", onWithoutPin)
            Text(
                "Sem a tela fixada, a sessão vale como treino, mas não rende pontos.",
                style = Ios.type.footnote,
                color = c.tertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FocusPoint(icon: ImageVector, text: String) {
    val c = Ios.colors
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(c.lavender.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.lavender, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = Ios.type.subhead, color = c.label)
    }
}
