package app.compasso.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.LooksTwo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.compasso.games.GameKind
import app.compasso.model.Tip
import app.compasso.ui.components.CoverArt
import app.compasso.ui.components.CoverColors
import app.compasso.ui.components.Motif
import app.compasso.ui.components.RoundPlayButton
import app.compasso.ui.components.SectionTitle
import app.compasso.ui.components.SkyBackground
import app.compasso.ui.components.TipCard
import app.compasso.ui.theme.Ios

fun gameIcon(kind: GameKind): ImageVector = when (kind) {
    GameKind.BLOCKS -> Icons.Rounded.Extension
    GameKind.MERGE -> Icons.Rounded.LooksTwo
    GameKind.ECHO -> Icons.Rounded.Psychology
}

@Composable
fun gameTint(kind: GameKind): Color = when (kind) {
    GameKind.BLOCKS -> Ios.colors.orange
    GameKind.MERGE -> Ios.colors.purple
    GameKind.ECHO -> Ios.colors.pink
}

fun gameCover(kind: GameKind): Pair<List<Color>, Motif> = when (kind) {
    GameKind.BLOCKS -> CoverColors.blocks to Motif.BLOCKS
    GameKind.MERGE -> CoverColors.merge to Motif.TILES
    GameKind.ECHO -> CoverColors.echo to Motif.PADS
}

/** Aba Refúgio: o lugar seguro para quando a vontade apertar. */
@Composable
fun RefugeScreen(
    best: Map<GameKind, Int>,
    tip: Tip?,
    onTipClose: (Tip) -> Unit,
    onBreathe: () -> Unit,
    onGame: (GameKind) -> Unit,
    onQuickGame: () -> Unit,
    onRead: () -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    val c = Ios.colors
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    SkyBackground(Modifier.fillMaxSize(), glow = c.lavender) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = top + 10.dp, bottom = bottomPadding + 16.dp),
        ) {
            Text("Refúgio", style = Ios.type.display, color = c.label, modifier = Modifier.padding(horizontal = 22.dp))
            Text(
                "A vontade vem em ondas e passa. Escolha uma saída e fique aqui até ela baixar.",
                style = Ios.type.callout,
                color = c.secondary,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 4.dp),
            )
            if (tip != null) {
                Spacer(Modifier.height(14.dp))
                TipCard(tip.title, tip.message, Icons.Rounded.Spa, onClose = { onTipClose(tip) })
            }

            // respirar: o cartão principal
            Spacer(Modifier.height(18.dp))
            CoverCard(
                colors = CoverColors.breathe,
                motif = Motif.WAVES,
                height = 190.dp,
                onClick = onBreathe,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Respirar", style = Ios.type.title1, color = Color.White)
                        Text("2 minutos com trilha calma", style = Ios.type.subhead, color = Color.White.copy(alpha = 0.85f))
                    }
                    RoundPlayButton(Icons.Rounded.PlayArrow, "Respirar agora", onBreathe, size = 60.dp, color = Color.White.copy(alpha = 0.95f))
                }
            }

            // jogos
            SectionTitle("Jogos", action = "Surpresa", onAction = onQuickGame)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameKind.entries.take(2).forEach { kind ->
                    GameCard(kind, best[kind] ?: 0, { onGame(kind) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val kind = GameKind.ECHO
                GameCard(kind, best[kind] ?: 0, { onGame(kind) }, Modifier.weight(1f))
                CoverCard(
                    colors = CoverColors.mind,
                    motif = Motif.STARS,
                    height = 168.dp,
                    onClick = onRead,
                    modifier = Modifier.weight(1f),
                    seed = 7,
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                        Text("Ler algo bom", style = Ios.type.headline, color = Color.White)
                        Text("Artigos curtos, com fontes", style = Ios.type.footnote, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            Text(
                "Num estudo de campo (Skorka-Brown e colegas, 2015), jogar Tetris por 3 minutos reduziu em média 13,9 pontos percentuais a força de vontades de comida, bebida, cigarro, café e também de sexo. Os jogos daqui seguem a mesma ideia: ocupar a cabeça com algo visual até a onda passar.",
                style = Ios.type.footnote,
                color = c.secondary,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp),
            )
        }
    }
}

/** Cartão com capa desenhada pelo app e conteúdo por cima. */
@Composable
fun CoverCard(
    colors: List<Color>,
    motif: Motif,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    seed: Int = 1,
    content: @Composable () -> Unit,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(stiffness = 1400f), label = "capa")
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(0.75.dp, c.glassBorder, shape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
    ) {
        CoverArt(colors, motif, Modifier.matchParentSize(), seed)
        content()
    }
}

@Composable
private fun GameCard(kind: GameKind, record: Int, onClick: () -> Unit, modifier: Modifier) {
    val (colors, motif) = gameCover(kind)
    CoverCard(colors, motif, 168.dp, onClick, modifier, seed = kind.ordinal) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
            Text(kind.title, style = Ios.type.title3, color = Color.White)
            Text(
                if (record > 0) "Recorde ${formatScore(record)}" else kind.subtitle,
                style = Ios.type.footnote,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
            )
        }
    }
}

/** Pontuação com separador de milhar ("12.480"). */
fun formatScore(v: Int): String = String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,d", v)
