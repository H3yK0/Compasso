package app.compasso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.text.style.TextOverflow
import app.compasso.ui.components.CoverArt
import app.compasso.ui.components.CoverColors
import app.compasso.ui.components.GroupCard
import app.compasso.ui.components.Motif
import app.compasso.ui.components.SectionTitle
import app.compasso.ui.components.SkyBackground
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.compasso.model.Article
import app.compasso.model.Block
import app.compasso.model.Tip
import app.compasso.ui.components.IosNavBar
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.TipCard
import app.compasso.ui.components.inlineMarkdown
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Capa de cada artigo: cores do tema e um desenho que lembra o assunto. */
fun articleCover(a: Article): Pair<List<Color>, Motif> {
    val colors = when (a.category) {
        "Mente e hábitos" -> CoverColors.mind
        "O que a ciência diz" -> CoverColors.science
        "Corpo e sexualidade" -> CoverColors.body
        else -> CoverColors.support
    }
    val motif = when (a.id) {
        "ondas", "corpo", "erecao" -> Motif.WAVES
        "gatilhos", "ajuda" -> Motif.SPARK
        "recaida", "cerebro", "precoce" -> Motif.ORBITS
        else -> Motif.STARS
    }
    return colors to motif
}

/** Aba Ler: artigos curtos, com fontes, agrupados por tema. */
@Composable
fun ReadScreen(
    articles: List<Article>,
    read: Set<String>,
    tip: Tip?,
    onTipClose: (Tip) -> Unit,
    onOpen: (String) -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    val c = Ios.colors
    val featured = articles.firstOrNull()
    val groups = remember(articles) { articles.drop(1).groupBy { it.category } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val readCount = articles.count { it.id in read }
    SkyBackground(Modifier.fillMaxSize(), glow = c.blue, glowStrength = 0.8f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = top + 10.dp, bottom = bottomPadding + 16.dp),
        ) {
            Text("Ler", style = Ios.type.display, color = c.label, modifier = Modifier.padding(horizontal = 22.dp))
            Text(
                if (articles.isEmpty()) "Leituras curtas, com fontes." else "$readCount de ${articles.size} lidos. Cada um leva 2 ou 3 minutos.",
                style = Ios.type.callout,
                color = c.secondary,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp),
            )
            if (tip != null) {
                Spacer(Modifier.height(14.dp))
                TipCard(tip.title, tip.message, Icons.AutoMirrored.Rounded.MenuBook, onClose = { onTipClose(tip) })
            }
            if (featured != null) {
                Spacer(Modifier.height(18.dp))
                FeaturedArticle(featured, featured.id in read) { onOpen(featured.id) }
            }
            groups.forEach { (category, list) ->
                SectionTitle(category)
                GroupCard {
                    list.forEachIndexed { i, a ->
                        if (i > 0) RowDivider(88.dp)
                        ArticleRow(a, a.id in read) { onOpen(a.id) }
                    }
                }
            }
            Text(
                "Textos do Compasso, revisados para não exagerar: cada um cita os estudos no final. Não substituem a orientação de um profissional de saúde. Licença CC BY-SA 4.0.",
                style = Ios.type.footnote,
                color = c.tertiary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp),
            )
        }
    }
}

@Composable
private fun FeaturedArticle(a: Article, isRead: Boolean, onClick: () -> Unit) {
    val (colors, motif) = articleCover(a)
    CoverCard(colors, motif, 236.dp, onClick, Modifier.padding(horizontal = 16.dp), seed = 3) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Bottom) {
            Text(
                "Comece por aqui",
                style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(a.title, style = Ios.type.title1, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(a.summary, style = Ios.type.subhead, color = Color.White.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${a.minutes} min de leitura", style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = Color.White.copy(alpha = 0.9f))
                if (isRead) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Lido", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Uma linha da lista: capinha, título, resumo e tempo de leitura. */
@Composable
private fun ArticleRow(a: Article, isRead: Boolean, onClick: () -> Unit) {
    val c = Ios.colors
    val (colors, motif) = articleCover(a)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp))) {
            CoverArt(colors, motif, Modifier.fillMaxSize(), seed = a.order, centered = true)
            if (isRead) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Lido", tint = colors.last(), modifier = Modifier.size(13.dp))
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, style = Ios.type.headline, color = c.label)
            Spacer(Modifier.height(2.dp))
            Text(a.summary, style = Ios.type.footnote, color = c.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                if (isRead) "${a.minutes} min · lido" else "${a.minutes} min",
                style = Ios.type.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = if (isRead) c.green else c.tertiary,
            )
        }
    }
}

/**
 * Leitura de um artigo. Chegar ao fim (e passar um tempo mínimo lendo) marca como lido e
 * rende pontos uma vez.
 */
@Composable
fun ReaderScreen(article: Article, isRead: Boolean, onRead: () -> Unit, onBack: () -> Unit) {
    val c = Ios.colors
    val state = rememberLazyListState()
    val uri = LocalUriHandler.current
    val rangePx = with(LocalDensity.current) { 60.dp.toPx() }
    val collapsed by remember(state) {
        derivedStateOf { if (state.firstVisibleItemIndex > 0) 1f else (state.firstVisibleItemScrollOffset / rangePx).coerceIn(0f, 1f) }
    }
    var openedFor by remember { mutableLongStateOf(0L) }
    // tempo mínimo: 30% do tempo estimado (pelo menos 20 s)
    val minMs = (article.minutes * 60_000L * 3 / 10).coerceAtLeast(20_000L)
    LaunchedEffect(article.id) {
        if (isRead) return@LaunchedEffect
        while (openedFor < minMs) {
            delay(1_000)
            openedFor += 1_000
        }
        snapshotFlow {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.layoutInfo.totalItemsCount - 2
        }.first { it }
        onRead()
    }

    val progress by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val total = info.totalItemsCount
            val visible = info.visibleItemsInfo
            if (total == 0 || visible.isEmpty()) {
                0f
            } else {
                val last = visible.last()
                if (last.index == total - 1 && last.offset + last.size <= info.viewportEndOffset) {
                    1f
                } else {
                    val first = visible.first()
                    val frac = (-first.offset).toFloat() / first.size.coerceAtLeast(1)
                    ((first.index + frac) / (total - visible.size + 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                }
            }
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bodyStyle = Ios.type.body.copy(lineHeight = 27.sp, color = c.label)
    val (coverColors, _) = articleCover(article)
    SkyBackground(Modifier.fillMaxSize(), glow = coverColors.first(), glowStrength = 0.9f, stars = false) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = top + 52.dp, bottom = bottom + 40.dp),
        ) {
            item(key = "cabecalho") {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        article.category,
                        style = Ios.type.subhead.copy(fontWeight = FontWeight.SemiBold),
                        color = c.accent,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(article.title, style = Ios.type.title1, color = c.label)
                    Spacer(Modifier.height(8.dp))
                    Text(article.summary, style = Ios.type.callout.copy(lineHeight = 23.sp), color = c.secondary)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${article.minutes} min de leitura", style = Ios.type.footnote, color = c.secondary)
                        if (isRead) {
                            Text(" · ", style = Ios.type.footnote, color = c.secondary)
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.green, modifier = Modifier.size(14.dp))
                            Text(" Lido", style = Ios.type.footnote, color = c.green)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
                    Spacer(Modifier.height(10.dp))
                }
            }
            article.blocks.forEachIndexed { i, b ->
                item(key = "b$i") {
                    Box(Modifier.padding(horizontal = 22.dp)) {
                        when (b) {
                            is Block.Heading -> Text(
                                inlineMarkdown(b.text),
                                style = Ios.type.serifSection.copy(fontSize = 24.sp, lineHeight = 29.sp),
                                color = c.label,
                                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                            )
                            is Block.Paragraph -> Text(inlineMarkdown(b.text), style = bodyStyle, modifier = Modifier.padding(vertical = 7.dp))
                            is Block.Quote -> Row(Modifier.padding(vertical = 10.dp).height(IntrinsicSize.Min)) {
                                Box(Modifier.width(3.dp).fillMaxHeight().clip(CircleShape).background(c.accent))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    inlineMarkdown(b.text),
                                    style = Ios.type.title3.copy(fontWeight = FontWeight.Medium, lineHeight = 27.sp),
                                    color = c.label,
                                )
                            }
                            is Block.Bullets -> Column(Modifier.padding(vertical = 6.dp)) {
                                b.items.forEach { item ->
                                    Row(Modifier.padding(vertical = 4.dp)) {
                                        Box(Modifier.padding(top = 10.dp, end = 12.dp).size(6.dp).clip(CircleShape).background(c.accent))
                                        Text(inlineMarkdown(item), style = bodyStyle)
                                    }
                                }
                            }
                            is Block.Numbered -> Column(Modifier.padding(vertical = 6.dp)) {
                                b.items.forEachIndexed { n, item ->
                                    Row(Modifier.padding(vertical = 4.dp)) {
                                        Text("${n + 1}.", style = bodyStyle.copy(fontWeight = FontWeight.SemiBold, color = c.accent), modifier = Modifier.width(26.dp))
                                        Text(inlineMarkdown(item), style = bodyStyle)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (article.sources.isNotEmpty()) {
                item(key = "fontes") {
                    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp)) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
                        Spacer(Modifier.height(16.dp))
                        Text("Fontes", style = Ios.type.headline, color = c.label)
                        Spacer(Modifier.height(6.dp))
                        article.sources.forEachIndexed { n, s ->
                            Row(Modifier.padding(vertical = 5.dp)) {
                                Text("${n + 1}.", style = Ios.type.footnote, color = c.secondary, modifier = Modifier.width(22.dp))
                                Column {
                                    Text(inlineMarkdown(s.text), style = Ios.type.footnote.copy(lineHeight = 19.sp), color = c.secondary)
                                    val url = s.url
                                    if (url != null) {
                                        Text(
                                            url,
                                            style = Ios.type.footnote.copy(textDecoration = TextDecoration.Underline),
                                            color = c.accent,
                                            modifier = Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                role = Role.Button,
                                            ) { runCatching { uri.openUri(url) } },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "licenca") {
                Text(
                    "Este texto informa e não substitui avaliação profissional. Compasso · licença CC BY-SA 4.0.",
                    style = Ios.type.caption1,
                    color = c.tertiary,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp),
                )
            }
        }
        IosNavBar(article.title, collapsed, onBack = onBack, backLabel = "Ler", progress = progress)
    }
}
