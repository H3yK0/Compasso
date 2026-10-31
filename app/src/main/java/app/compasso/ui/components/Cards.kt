package app.compasso.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.compasso.model.Tier
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.tierColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------- Dicas ("ir descobrindo") ----------

/** Cartão de dica, como as dicas do iPhone: ícone, título, texto e um X para dispensar. */
@Composable
fun TipCard(title: String, message: String, icon: ImageVector, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.card)
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(c.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 1.dp)) {
            Text(title, style = Ios.type.headline, color = c.label)
            Spacer(Modifier.height(2.dp))
            Text(message, style = Ios.type.subhead, color = c.secondary)
        }
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Dispensar dica", tint = c.tertiary, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- Avatar do codinome ----------

/**
 * Emblema gerado a partir da semente do codinome: uma rosa dos ventos com cores e pétalas
 * próprias. Ninguém precisa de foto. [ring] desenha o anel do nível em volta.
 */
@Composable
fun CodenameAvatar(seed: Long, size: Dp, modifier: Modifier = Modifier, ring: Color? = null) {
    val look = remember(seed) { AvatarLook.from(seed) }
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val ringWidth = if (ring != null) r * 0.075f else 0f
        val inner = r - if (ring != null) ringWidth * 2.2f else 0f
        if (ring != null) drawCircle(ring, radius = r - ringWidth / 2f, style = Stroke(ringWidth))
        drawCircle(
            Brush.linearGradient(listOf(look.a, look.b), start = Offset(0f, 0f), end = Offset(this.size.width, this.size.height)),
            radius = inner,
        )
        // pétalas
        val petalLen = inner * look.petalLength
        for (k in 0 until look.petals) {
            rotate(look.rotation + 360f * k / look.petals, pivot = center) {
                drawOval(
                    Color.White.copy(alpha = 0.20f),
                    topLeft = Offset(center.x - inner * look.petalWidth / 2f, center.y - petalLen),
                    size = Size(inner * look.petalWidth, petalLen),
                )
            }
        }
        // anel interno e estrela de 4 pontas (o "compasso")
        drawCircle(Color.White.copy(alpha = 0.28f), radius = inner * 0.36f, style = Stroke(inner * 0.035f))
        val star = androidx.compose.ui.graphics.Path().apply {
            val big = inner * 0.30f
            val small = inner * 0.09f
            for (i in 0 until 8) {
                val ang = (look.rotation * PI / 180.0) + i * PI / 4 - PI / 2
                val len = if (i % 2 == 0) big else small
                val x = center.x + (cos(ang) * len).toFloat()
                val y = center.y + (sin(ang) * len).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, Color.White.copy(alpha = 0.92f))
    }
}

private data class AvatarLook(
    val a: Color,
    val b: Color,
    val petals: Int,
    val petalWidth: Float,
    val petalLength: Float,
    val rotation: Float,
) {
    companion object {
        fun from(seed: Long): AvatarLook {
            val r = Random(seed)
            val h1 = r.nextFloat() * 360f
            val h2 = (h1 + 35f + r.nextFloat() * 90f) % 360f
            return AvatarLook(
                a = Color.hsv(h1, 0.50f + r.nextFloat() * 0.2f, 0.70f),
                b = Color.hsv(h2, 0.62f + r.nextFloat() * 0.2f, 0.36f),
                petals = 5 + r.nextInt(5),
                petalWidth = 0.24f + r.nextFloat() * 0.14f,
                petalLength = 0.74f + r.nextFloat() * 0.16f,
                rotation = r.nextFloat() * 360f,
            )
        }
    }
}

// ---------- Medalhas do ranking pessoal ----------

fun tierIcon(t: Tier): ImageVector = when (t) {
    Tier.START -> Icons.Rounded.Explore
    Tier.BRONZE, Tier.SILVER, Tier.GOLD -> Icons.Rounded.MilitaryTech
    Tier.PLATINUM -> Icons.Rounded.WorkspacePremium
    Tier.DIAMOND -> Icons.Rounded.Diamond
    Tier.UNBEATEN -> Icons.Rounded.EmojiEvents
}

@Composable
fun TierMedal(tier: Tier, size: Dp, modifier: Modifier = Modifier, locked: Boolean = false) {
    val color = if (locked) Ios.colors.fill else tierColor(tier)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (locked) Brush.linearGradient(listOf(color, color))
                else Brush.linearGradient(listOf(color.copy(alpha = 0.95f), color.copy(red = color.red * 0.75f, green = color.green * 0.75f, blue = color.blue * 0.75f))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            tierIcon(tier),
            contentDescription = tier.label,
            tint = if (locked) Ios.colors.tertiary else Color.White,
            modifier = Modifier.size(size * 0.58f),
        )
    }
}

/** Barra de progresso fina e arredondada. */
@Composable
fun ProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier, track: Color = Ios.colors.fill, height: Dp = 6.dp) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(this.size.height / 2f)
        drawRoundRect(track, cornerRadius = radius)
        val w = this.size.width * progress.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(color, size = Size(w.coerceAtLeast(this.size.height), this.size.height), cornerRadius = radius)
    }
}
