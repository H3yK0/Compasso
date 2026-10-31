package app.compasso.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.compasso.ui.theme.Ios

data class RowIcon(val vector: ImageVector, val tint: Color)

data class AlertAction(
    val label: String,
    val onClick: () -> Unit,
    val bold: Boolean = false,
    val destructive: Boolean = false,
)

enum class ButtonKind { Filled, Tinted, Plain, White, Glass }

/** Recuo da linha divisória em linhas com ícone (alinha com o texto, como no iOS). */
val IconInset = 62.dp

private val IosCurve = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

// ---------- Setas ----------

@Composable
fun Chevron(color: Color, modifier: Modifier = Modifier, pointsLeft: Boolean = false, stroke: Dp = 2.dp) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (pointsLeft) {
                moveTo(w * 0.8f, h * 0.08f); lineTo(w * 0.2f, h * 0.5f); lineTo(w * 0.8f, h * 0.92f)
            } else {
                moveTo(w * 0.2f, h * 0.08f); lineTo(w * 0.8f, h * 0.5f); lineTo(w * 0.2f, h * 0.92f)
            }
        }
        drawPath(path, color, style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ---------- Barra de navegação ----------

@Composable
fun NavIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Ios.colors.accent) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .size(44.dp)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint.copy(alpha = if (pressed) 0.35f else 1f), modifier = Modifier.size(25.dp))
    }
}

@Composable
fun NavTextButton(text: String, onClick: () -> Unit, bold: Boolean = false, enabled: Boolean = true) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Text(
        text,
        style = if (bold) Ios.type.headline else Ios.type.body,
        color = (if (enabled) c.accent else c.tertiary).copy(alpha = if (pressed) 0.35f else 1f),
        modifier = Modifier
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}

@Composable
private fun BackButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val tint = Ios.colors.accent.copy(alpha = if (pressed) 0.35f else 1f)
    Row(
        modifier
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chevron(tint, Modifier.size(width = 12.dp, height = 21.dp), pointsLeft = true, stroke = 2.6.dp)
        Spacer(Modifier.width(6.dp))
        Text(label, style = Ios.type.body, color = tint)
    }
}

/** Barra fina do topo: o título pequeno aparece quando o título grande sai da tela. */
@Composable
fun IosNavBar(
    title: String,
    collapsed: Float,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Hoje",
    actions: @Composable RowScope.() -> Unit = {},
    progress: Float? = null,
) {
    val c = Ios.colors
    Column(Modifier.fillMaxWidth().background(c.navBar.copy(alpha = c.navBar.alpha * collapsed))) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            if (onBack != null) BackButton(backLabel, onBack, Modifier.align(Alignment.CenterStart))
            Text(
                title,
                style = Ios.type.headline,
                color = c.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 100.dp)
                    .graphicsLayer { alpha = collapsed }
                    .then(if (collapsed < 0.5f) Modifier.clearAndSetSemantics { } else Modifier),
            )
            Row(
                Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        if (progress != null) {
            // barra de leitura: quanto do texto já passou
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.label.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp).background(c.accent))
            }
        } else {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator.copy(alpha = collapsed)))
        }
    }
}

/** Tela de lista com título grande (como Ajustes do iPhone). */
@Composable
fun LargeTitleList(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Hoje",
    actions: @Composable RowScope.() -> Unit = {},
    bottomPadding: Dp = 0.dp,
    state: LazyListState = rememberLazyListState(),
    rootTab: Boolean = false,
    glow: Color = Ios.colors.accent,
    content: LazyListScope.() -> Unit,
) {
    val c = Ios.colors
    val rangePx = with(LocalDensity.current) { 36.dp.toPx() }
    val collapsed by remember(state) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f else (state.firstVisibleItemScrollOffset / rangePx).coerceIn(0f, 1f)
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    SkyBackground(modifier.fillMaxSize(), glow = glow, glowStrength = if (rootTab) 0.9f else 0.7f) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = top + if (rootTab) 10.dp else 44.dp, bottom = bottom + 28.dp + bottomPadding),
        ) {
            item(key = "titulo-grande") {
                // nas abas, o título grande na serifa de destaque (como Hoje, Refúgio e Ler)
                Text(
                    title,
                    style = if (rootTab) Ios.type.display else Ios.type.largeTitle,
                    color = c.label,
                    modifier = Modifier.padding(start = if (rootTab) 22.dp else 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                )
            }
            content()
        }
        IosNavBar(title, collapsed, onBack, backLabel, actions)
    }
}

// ---------- Listas agrupadas ----------

fun LazyListScope.section(
    header: String? = null,
    footer: String? = null,
    key: Any? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    item(key = key) {
        Column(Modifier.fillMaxWidth()) {
            if (header != null) GroupHeader(header) else Spacer(Modifier.height(24.dp))
            GroupCard(content = content)
            if (footer != null) GroupFooter(footer)
        }
    }
}

@Composable
fun GroupHeader(text: String) {
    Text(
        text,
        style = Ios.type.subhead.copy(fontWeight = FontWeight.SemiBold),
        color = Ios.colors.secondary,
        modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 26.dp, bottom = 8.dp),
    )
}

@Composable
fun GroupFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Ios.type.footnote,
        color = Ios.colors.secondary,
        modifier = modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 7.dp),
    )
}

@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Ios.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(0.75.dp, c.glassBorder, shape),
        content = content,
    )
}

@Composable
fun RowDivider(inset: Dp = 16.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.5.dp).background(Ios.colors.separator))
}

@Composable
fun IconSquare(icon: RowIcon, size: Dp = 32.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(icon.tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon.vector, contentDescription = null, tint = icon.tint, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: RowIcon? = null,
    value: String? = null,
    chevron: Boolean = false,
    titleColor: Color = Color.Unspecified,
    titleStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    subtitleLines: Int = Int.MAX_VALUE,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val click = if (onClick != null) {
        Modifier.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier
            .then(click)
            .background(if (pressed) c.pressed else Color.Transparent)
            .fillMaxWidth()
            .heightIn(min = if (subtitle != null) 58.dp else 44.dp)
            .padding(start = 16.dp, end = if (chevron) 14.dp else 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        } else if (icon != null) {
            IconSquare(icon)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = titleStyle ?: Ios.type.body,
                color = if (titleColor == Color.Unspecified) c.label else titleColor,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = Ios.type.footnote,
                    color = c.secondary,
                    maxLines = subtitleLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(10.dp))
            Text(value, style = Ios.type.body, color = c.secondary, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
        if (chevron) {
            Spacer(Modifier.width(10.dp))
            Chevron(c.tertiary, Modifier.size(width = 8.dp, height = 13.dp))
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: RowIcon? = null,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    IosRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        titleColor = if (enabled) Color.Unspecified else Ios.colors.tertiary,
        modifier = Modifier.toggleable(
            value = checked,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(it)
        },
        trailing = { IosSwitch(checked, enabled = enabled) },
    )
}

// ---------- Controles ----------

@Composable
fun IosSwitch(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Ios.colors
    val track by animateColorAsState(if (checked) c.accent else c.switchOff, tween(220), label = "trilho")
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.72f, stiffness = 700f), label = "botao")
    Box(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .size(width = 51.dp, height = 31.dp)
            .background(track, CircleShape),
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(27.dp)
                .shadow(2.5.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun IosSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(c.fill, RoundedCornerShape(9.dp))
            .padding(2.dp),
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(
            segment * selected.coerceIn(0, options.lastIndex),
            spring(dampingRatio = 0.82f, stiffness = 520f),
            label = "segmento",
        )
        if (selected in options.indices) {
            Box(
                Modifier
                    .offset(x = x)
                    .width(segment)
                    .fillMaxHeight()
                    .shadow(1.5.dp, RoundedCornerShape(7.dp), ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
                    .background(c.segment, RoundedCornerShape(7.dp)),
            )
        }
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = i == selected, interactionSource = source, indication = null, role = Role.Tab) {
                            if (i != selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(i)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = Ios.type.footnote.copy(fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = c.label,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Filled,
    color: Color = Ios.colors.accent,
    contentColor: Color? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    icon: ImageVector? = null,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, spring(stiffness = 1500f), label = "escala")
    val colors: Pair<Color, Color> = when (kind) {
        ButtonKind.Filled -> Pair(
            if (enabled) color else c.fill,
            contentColor ?: if (!enabled) c.tertiary else if (color == c.accent) c.onAccent else Color.White,
        )
        ButtonKind.Tinted -> Pair(color.copy(alpha = 0.15f), contentColor ?: color)
        ButtonKind.Plain -> Pair(Color.Transparent, contentColor ?: color)
        ButtonKind.White -> Pair(
            if (enabled) Color.White else Color.White.copy(alpha = 0.22f),
            if (enabled) (contentColor ?: Color.Black) else Color.White.copy(alpha = 0.6f),
        )
        ButtonKind.Glass -> Pair(Color.White.copy(alpha = if (pressed) 0.3f else 0.2f), contentColor ?: Color.White)
    }
    val pressedAlpha = if (kind == ButtonKind.Plain) 0.35f else 0.85f
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed && enabled) pressedAlpha else 1f
            }
            .height(height)
            .clip(shape)
            .background(colors.first)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.second, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Ios.type.headline, color = colors.second, maxLines = 1)
    }
}

@Composable
fun GlassCircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (pressed) 0.32f else 0.2f))
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

// ---------- Sliders ----------

private fun fraction(v: Float, r: ClosedFloatingPointRange<Float>): Float {
    val span = r.endInclusive - r.start
    return if (span <= 0f) 0f else ((v - r.start) / span).coerceIn(0f, 1f)
}

@Composable
private fun SliderThumb() {
    Box(
        Modifier
            .size(28.dp)
            .shadow(3.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .background(Color.White, CircleShape),
    )
}

@Composable
private fun SliderTrack(start: Float, end: Float) {
    val c = Ios.colors
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(c.sliderTrack, cornerRadius = r)
        val x0 = size.width * start
        val x1 = size.width * end
        if (x1 > x0) drawRoundRect(c.accent, topLeft = Offset(x0, 0f), size = Size(x1 - x0, size.height), cornerRadius = r)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        thumb = { SliderThumb() },
        track = { state -> SliderTrack(0f, fraction(state.value, state.valueRange)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    RangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        startThumb = { SliderThumb() },
        endThumb = { SliderThumb() },
        track = { state ->
            SliderTrack(fraction(state.activeRangeStart, state.valueRange), fraction(state.activeRangeEnd, state.valueRange))
        },
    )
}

// ---------- Alertas ----------

@Composable
private fun DialogDim(amount: Float) {
    val view = LocalView.current
    SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(amount) }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ios.colors.separator))
}

@Composable
private fun AlertButton(action: AlertAction, modifier: Modifier, textStyle: TextStyle) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .background(if (pressed) c.pressed.copy(alpha = 0.6f) else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = action.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            action.label,
            style = textStyle.copy(fontWeight = if (action.bold) FontWeight.SemiBold else FontWeight.Normal),
            color = if (action.destructive) c.red else c.accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun AlertCard(title: String, message: String?, actions: List<AlertAction>, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Column(modifier.width(270.dp).clip(RoundedCornerShape(14.dp)).background(c.alert)) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = Ios.type.headline, color = c.label, textAlign = TextAlign.Center)
            if (message != null) {
                Spacer(Modifier.height(4.dp))
                Text(message, style = Ios.type.footnote, color = c.label, textAlign = TextAlign.Center)
            }
        }
        if (actions.size == 2) {
            Hairline()
            Row(Modifier.fillMaxWidth().height(44.dp)) {
                AlertButton(actions[0], Modifier.weight(1f).fillMaxHeight(), Ios.type.body)
                Box(Modifier.width(0.5.dp).fillMaxHeight().background(c.separator))
                AlertButton(actions[1], Modifier.weight(1f).fillMaxHeight(), Ios.type.body)
            }
        } else {
            actions.forEach { a ->
                Hairline()
                AlertButton(a, Modifier.fillMaxWidth().height(44.dp), Ios.type.body)
            }
        }
    }
}

@Composable
fun IosAlert(title: String, message: String?, actions: List<AlertAction>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogDim(0.3f)
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
        AlertCard(
            title, message, actions,
            Modifier.graphicsLayer {
                val s = 1.08f - 0.08f * appear.value
                scaleX = s
                scaleY = s
                alpha = appear.value
            },
        )
    }
}

@Composable
fun ActionSheetContent(
    title: String?,
    message: String?,
    actions: List<AlertAction>,
    cancel: AlertAction,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val optionStyle = Ios.type.title3.copy(fontWeight = FontWeight.Normal)
    Column(
        modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.alert)) {
            val hasHeader = title != null || message != null
            if (hasHeader) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (title != null) {
                        Text(title, style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = c.secondary, textAlign = TextAlign.Center)
                    }
                    if (message != null) {
                        Text(message, style = Ios.type.footnote, color = c.secondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            actions.forEachIndexed { i, a ->
                if (i > 0 || hasHeader) Hairline()
                AlertButton(a, Modifier.fillMaxWidth().height(56.dp), optionStyle)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.alertCancel)) {
            AlertButton(cancel.copy(bold = true), Modifier.fillMaxWidth().height(56.dp), optionStyle)
        }
    }
}

@Composable
fun IosActionSheet(
    title: String?,
    message: String?,
    actions: List<AlertAction>,
    cancel: AlertAction,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogDim(0.35f)
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, tween(320, easing = IosCurve)) }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ActionSheetContent(
                title, message, actions, cancel,
                Modifier.graphicsLayer { translationY = (1f - appear.value) * size.height },
            )
        }
    }
}
