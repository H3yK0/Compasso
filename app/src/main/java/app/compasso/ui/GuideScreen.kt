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
import androidx.compose.ui.unit.sp
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

private data class Topic(val title: String, val icon: ImageVector, val tint: @Composable () -> Color, val text: String)

private val topics = listOf(
    Topic(
        "O treino: para e recomeça", Icons.Rounded.GraphicEq, { PhaseColors.go[0] },
        "Cada sessão alterna ritmo e pausa. No ritmo (tela azul), siga a batida: a velocidade muda a cada rodada. " +
            "Na pausa (tela vermelha), tire as mãos e respire acompanhando as ondas, até a contagem zerar. " +
            "No fim, a tela fica verde (liberado: termine no seu ritmo) ou roxa (fim sem liberação).",
    ),
    Topic(
        "“Estou perto”", Icons.Rounded.BackHand, { PhaseColors.stop[0] },
        "Sentiu que vai passar do ponto antes da hora? Toque em “Estou perto”: o app manda parar na hora e a pausa fica mais longa. " +
            "Se ainda estiver perto na pausa, toque de novo para ganhar mais 10 s. Usar cedo é sinal de controle, não de fraqueza.",
    ),
    Topic(
        "Os níveis", Icons.Rounded.Layers, { Ios.colors.teal },
        "Fácil é curto e termina com liberação. Médio e Difícil têm mais rodadas e ritmos mais rápidos. " +
            "Impossível vai até o fim sem liberação: puro treino de controle. No Personalizado você escolhe rodadas, tempos, ritmos e como termina. " +
            "Na tela Hoje, deslize os cartões para escolher o nível e toque em Começar.",
    ),
    Topic(
        "Controle inteligente", Icons.Rounded.HourglassTop, { Ios.colors.orange },
        "Para não alimentar a pressa, a liberação tem regras: depois de uma liberação (ou de um “não segurei”, ou de uma sessão interrompida), " +
            "a próxima só vem depois do intervalo escolhido (24 h por padrão). Sessão começada menos de 2 h depois da anterior é só de controle, " +
            "e com 5 sessões ou mais na semana o intervalo dobra. Quando está fechada, a sessão não é cancelada: vira treino de controle.",
    ),
    Topic(
        "Modo foco", Icons.Rounded.PhonelinkLock, { Ios.colors.indigo },
        "Antes de cada sessão o app pede para fixar a tela, um recurso do próprio Android: enquanto fixada, não dá para abrir outro app, " +
            "as notificações e a tela dividida ficam bloqueadas e vídeos flutuantes são fechados. No Android 12 ou mais novo, janelas flutuantes de outros apps também somem. " +
            "Se sair do app, a sessão pausa. Voltar em até 10 s continua; mais que isso, ou na terceira saída, ela é encerrada como interrompida. " +
            "Para desligar o modo foco, a mudança só vale 24 h depois.",
    ),
    Topic(
        "Refúgio: respirar, jogar, ler", Icons.Rounded.Air, { Ios.colors.indigo },
        "Quando a vontade apertar, abra o Refúgio. Respirar guia 2 minutos (inspira 4 s, solta 6 s) com uma trilha calma que acompanha o ar. " +
            "Os jogos ocupam a cabeça com algo visual por alguns minutos. As leituras explicam o que a ciência sabe, com as fontes no fim.",
    ),
    Topic(
        "Pontos, níveis e pódio", Icons.Rounded.EmojiEvents, { Ios.colors.yellow },
        "Os pontos premiam constância: check-in do dia, respirar, jogar, ler e a primeira sessão do dia (só com a tela fixada). " +
            "O teto é de ${Progress.DAILY_CAP} pontos por dia. O nível conta os últimos 28 dias: Bronze, Prata, Ouro, Platina, Diamante e Invicto. " +
            "Invicto também pede 14 dias seguidos de constância e 14 dias sem “não segurei” nem sessão interrompida. Cada nível libera uma cor para o app.",
    ),
    Topic(
        "Leituras", Icons.AutoMirrored.Rounded.MenuBook, { Ios.colors.blue },
        "Textos curtos sobre mente, corpo e sexualidade, escritos para informar sem assustar. Cada um cita os estudos usados. " +
            "Ler até o fim marca como lido e rende pontos uma vez por artigo.",
    ),
    Topic(
        "Privacidade", Icons.Rounded.Shield, { Ios.colors.gray },
        "O Compasso não tem acesso à internet: tudo fica guardado só neste celular, sem conta. Você pode pedir senha ao abrir, " +
            "esconder o app nos recentes (modo discreto) e apagar o histórico. O codinome é criado pelo app e não identifica você.",
    ),
)

/** "Como usar": sempre disponível nos ajustes. */
@Composable
fun GuideScreen(onReplayIntro: () -> Unit, onBack: () -> Unit, backLabel: String = "Ajustes") {
    val c = Ios.colors
    var open by rememberSaveable { mutableStateOf(-1) }
    LargeTitleList("Como usar", onBack = onBack, backLabel = backLabel) {
        section(key = "apresentacao") {
            IosRow(
                "Rever a apresentação",
                subtitle = "As telas do começo, em 1 minuto",
                icon = RowIcon(Icons.Rounded.PlayCircle, c.teal),
                chevron = true,
                onClick = onReplayIntro,
            )
        }
        section(header = "Guia", footer = "Toque num tema para abrir.", key = "temas") {
            topics.forEachIndexed { i, t ->
                if (i > 0) RowDivider(IconInset)
                val expanded = open == i
                val angle by animateFloatAsState(if (expanded) 90f else 0f, label = "seta")
                IosRow(
                    t.title,
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
                        Text(t.text, style = Ios.type.subhead.copy(lineHeight = 21.sp), color = c.secondary)
                    }
                }
            }
        }
    }
}
