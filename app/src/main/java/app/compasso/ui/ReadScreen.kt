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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import app.compasso.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.compasso.model.Article
import app.compasso.model.Block
import app.compasso.model.Theme
import app.compasso.model.ReadingAdvice
import app.compasso.audio.Track
import app.compasso.ui.components.NavIconButton
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import app.compasso.model.Tip
import app.compasso.ui.components.IosNavBar
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.TipCard
import app.compasso.ui.components.inlineMarkdown
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Capa de cada artigo: cores do tema e um desenho que lembra o assunto. */
fun articleCover(a: Article): Pair<List<Color>, Motif> {
    val colors = when (a.theme) {
        Theme.MIND -> CoverColors.mind
        Theme.SCIENCE -> CoverColors.science
        Theme.BODY -> CoverColors.body
        Theme.SUPPORT -> CoverColors.support
        Theme.RELATIONS -> CoverColors.relations
    }
    val motif = when (a.id) {
        "ondas", "corpo", "erecao" -> Motif.WAVES
        "gatilhos", "ajuda" -> Motif.SPARK
        "recaida", "cerebro", "precoce" -> Motif.ORBITS
        "dopamina" -> Motif.STARS
        // os mais novos alternam os desenhos pela ordem
        else -> listOf(Motif.WAVES, Motif.ORBITS, Motif.SPARK, Motif.STARS)[a.order % 4]
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
    advice: ReadingAdvice? = null,
    music: Boolean = false,
    track: Track = Track.RAIN,
    onMusic: (Boolean) -> Unit = {},
    onTrack: (Track) -> Unit = {},
) {
    val c = Ios.colors
    // em destaque: a leitura sugerida pelo histórico (ou a primeira, para quem está chegando)
    val featured = advice?.let { a -> articles.firstOrNull { it.id == a.articleId } } ?: articles.firstOrNull()
    val groups = remember(articles, featured) { articles.filter { it.id != featured?.id }.groupBy { it.category } }
    var picking by remember { mutableStateOf(false) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val readCount = articles.count { it.id in read }
    SkyBackground(Modifier.fillMaxSize(), glow = c.blue, glowStrength = 0.8f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = top + 10.dp, bottom = bottomPadding + 16.dp),
        ) {
            Text(stringResource(R.string.tab_read), style = Ios.type.display, color = c.label, modifier = Modifier.padding(horizontal = 22.dp))
            Text(
                if (articles.isEmpty()) stringResource(R.string.read_intro_empty) else stringResource(R.string.read_intro, readCount, articles.size),
                style = Ios.type.callout,
                color = c.secondary,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp),
            )
            if (tip != null) {
                Spacer(Modifier.height(14.dp))
                TipCard(stringResource(tip.title), stringResource(tip.message), Icons.AutoMirrored.Rounded.MenuBook, onClose = { onTipClose(tip) })
            }
            Spacer(Modifier.height(14.dp))
            ReadingMusicCard(music, track, onToggle = onMusic, onPickTrack = { picking = true })
            if (featured != null) {
                Spacer(Modifier.height(14.dp))
                val label = if (advice != null && advice.articleId == featured.id) stringResource(R.string.for_you) else stringResource(R.string.read_start_here)
                FeaturedArticle(featured, featured.id in read, label) { onOpen(featured.id) }
                if (advice != null && advice.articleId == featured.id) {
                    Text(
                        signalText(advice.reason),
                        style = Ios.type.footnote,
                        color = c.secondary,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp),
                    )
                }
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
                stringResource(R.string.read_footer),
                style = Ios.type.footnote,
                color = c.tertiary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp),
            )
        }
    }
    if (picking) TrackPicker(track, onPick = onTrack, onDismiss = { picking = false })
}

@Composable
private fun FeaturedArticle(a: Article, isRead: Boolean, label: String, onClick: () -> Unit) {
    val (colors, motif) = articleCover(a)
    // o destaque pode ser qualquer artigo: o desenho fica na faixa de cima e um véu protege o texto
    CoverCard(colors, motif, 236.dp, onClick, Modifier.padding(horizontal = 16.dp), seed = 3, motifArea = 0.5f) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.35f to Color.Transparent, 1f to colors.last().copy(alpha = 0.7f)),
            ),
        )
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Bottom) {
            Text(
                label,
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
                Text(stringResource(R.string.read_minutes, a.minutes), style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = Color.White.copy(alpha = 0.9f))
                if (isRead) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.read_done), tint = Color.White, modifier = Modifier.size(16.dp))
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
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.read_done), tint = colors.last(), modifier = Modifier.size(13.dp))
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
                if (isRead) stringResource(R.string.read_minutes_done, a.minutes) else stringResource(R.string.duration_minutes, a.minutes),
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
fun ReaderScreen(
    article: Article,
    isRead: Boolean,
    onRead: () -> Unit,
    onBack: () -> Unit,
    music: Boolean = false,
    track: Track = Track.RAIN,
    onMusic: (Boolean) -> Unit = {},
    onTrack: (Track) -> Unit = {},
    onOpenChange: (Boolean) -> Unit = {},
) {
    val c = Ios.colors
    var picking by remember { mutableStateOf(false) }
    // a música de leitura toca só com o leitor aberto
    val openChange by rememberUpdatedState(onOpenChange)
    DisposableEffect(Unit) {
        openChange(true)
        onDispose { openChange(false) }
    }
    val state = rememberLazyListState()
    val uri = LocalUriHandler.current
    val rangePx = with(LocalDensity.current) { 60.dp.toPx() }
    val collapsed by remember(state) {
        derivedStateOf { if (state.firstVisibleItemIndex > 0) 1f else (state.firstVisibleItemScrollOffset / rangePx).coerceIn(0f, 1f) }
    }
    var openedFor by remember { mutableLongStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // tempo mínimo: 30% do tempo estimado (pelo menos 20 s), só contando com o app na frente
    val minMs = (article.minutes * 60_000L * 3 / 10).coerceAtLeast(20_000L)
    LaunchedEffect(article.id) {
        if (isRead) return@LaunchedEffect
        while (openedFor < minMs) {
            delay(1_000)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) openedFor += 1_000
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
                        Text(stringResource(R.string.read_minutes, article.minutes), style = Ios.type.footnote, color = c.secondary)
                        if (isRead) {
                            Text(" · ", style = Ios.type.footnote, color = c.secondary)
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.green, modifier = Modifier.size(14.dp))
                            Text(" " + stringResource(R.string.read_done), style = Ios.type.footnote, color = c.green)
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
                        Text(stringResource(R.string.read_sources), style = Ios.type.headline, color = c.label)
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
                    stringResource(R.string.read_article_footer),
                    style = Ios.type.caption1,
                    color = c.tertiary,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp),
                )
            }
        }
        IosNavBar(
            article.title,
            collapsed,
            onBack = onBack,
            backLabel = stringResource(R.string.tab_read),
            progress = progress,
            actions = {
                if (music) NavIconButton(Icons.Rounded.MusicNote, stringResource(R.string.track_change), { picking = true })
                NavIconButton(
                    if (music) Icons.Rounded.Headphones else Icons.Rounded.HeadsetOff,
                    stringResource(if (music) R.string.read_music_turn_off else R.string.read_music_turn_on),
                    { onMusic(!music) },
                    tint = if (music) c.accent else c.secondary,
                )
            },
        )
    }
    if (picking) TrackPicker(track, onPick = onTrack, onDismiss = { picking = false })
}
