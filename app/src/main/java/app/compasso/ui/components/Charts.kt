package app.compasso.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.compasso.model.Bar
import app.compasso.ui.theme.Ios
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Número em pt-BR: 1 casa decimal quando não é inteiro e é menor que 100 ("4,5", "12,5"). */
fun formatNumber(v: Float): String {
    val r = Math.round(v * 10f) / 10f
    return if (r != floor(r) && r < 100f) String.format(Locale.forLanguageTag("pt-BR"), "%.1f", r) else Math.round(v).toString()
}

/** Topo "redondo" do eixo (10, 20, 25, 40, 50…) para as linhas de grade caírem em números fáceis. */
private fun niceMax(v: Float): Float {
    if (v <= 0f) return 1f
    val exp = floor(log10(v.toDouble()))
    val base = 10.0.pow(exp)
    val f = v / base
    val nice = listOf(1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0).first { f <= it + 1e-9 }
    return (nice * base).toFloat()
}

/**
 * Gráfico de barras no estilo do app Saúde do iPhone: o valor da barra escolhida aparece em
 * cima, grade discreta com números redondos à direita e linha tracejada da média.
 * Toque ou arraste sobre as barras para ver cada período.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    color: Color,
    unit: String,
    caption: (Bar) -> String,
    emptyText: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 168.dp,
    showAverage: Boolean = true,
) {
    val c = Ios.colors
    val measurer = rememberTextMeasurer()
    val hasData = bars.any { it.value > 0f }
    val lastWithData = bars.indexOfLast { it.value > 0f }.let { if (it < 0) bars.lastIndex else it }
    var selected by remember(bars) { mutableIntStateOf(lastWithData) }
    val maxV = niceMax(bars.maxOfOrNull { it.value } ?: 0f)
    val nonZero = bars.filter { it.value > 0f }
    val average = if (nonZero.isEmpty()) 0f else nonZero.sumOf { it.value.toDouble() }.toFloat() / nonZero.size
    val axisStyle = Ios.type.caption2.copy(color = c.secondary)
    val labelStyle = Ios.type.caption2.copy(color = c.secondary, textAlign = TextAlign.Center)
    val sel = bars.getOrNull(selected)

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // valor em destaque
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (hasData && sel != null) formatNumber(sel.value) else "—",
                style = Ios.type.figure.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = c.label,
            )
            Spacer(Modifier.width(4.dp))
            Text(unit, style = Ios.type.subhead, color = c.secondary, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(
            if (hasData && sel != null) caption(sel) else emptyText,
            style = Ios.type.footnote,
            color = c.secondary,
        )
        Spacer(Modifier.height(12.dp))
        val description = if (bars.isEmpty()) emptyText else bars.joinToString { "${it.label}: ${formatNumber(it.value)} $unit" }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .semantics { contentDescription = description }
                .pointerInput(bars) {
                    // sem barras (nenhuma sessão ainda) não há o que escolher: tocar não pode quebrar o app
                    if (bars.isEmpty()) return@pointerInput
                    detectTapGestures { pos ->
                        val axisW = 30.dp.toPx()
                        val slot = (size.width - axisW) / bars.size
                        selected = (pos.x / slot).toInt().coerceIn(0, bars.lastIndex)
                    }
                }
                .pointerInput(bars) {
                    if (bars.isEmpty()) return@pointerInput
                    detectHorizontalDragGestures { change, _ ->
                        val axisW = 30.dp.toPx()
                        val slot = (size.width - axisW) / bars.size
                        selected = (change.position.x / slot).toInt().coerceIn(0, bars.lastIndex)
                    }
                },
        ) {
            if (bars.isEmpty()) return@Canvas
            val axisW = 30.dp.toPx()
            val labelH = 18.dp.toPx()
            val plotW = size.width - axisW
            val plotH = size.height - labelH
            val slot = plotW / bars.size
            // barra fina: no máximo 20 dp, nunca enche a faixa (o resto é respiro)
            val barW = (slot * 0.58f).coerceAtMost(20.dp.toPx()).coerceAtMost(slot - 2.dp.toPx())
            val grid = c.separator.copy(alpha = 0.7f)

            // período escolhido: faixa discreta atrás da barra (as barras ficam todas com contraste)
            if (hasData && selected in bars.indices) {
                drawRoundRect(
                    c.fill.copy(alpha = c.fill.alpha * 0.7f),
                    topLeft = Offset(slot * selected + 1.dp.toPx(), 0f),
                    size = Size(slot - 2.dp.toPx(), plotH),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }

            // grade: 0, metade e topo, linhas finas e contínuas, números à direita
            for (k in 0..2) {
                val v = maxV * k / 2f
                val y = plotH - plotH * (v / maxV)
                drawLine(grid, Offset(0f, y), Offset(plotW, y), strokeWidth = 1f)
                val t = measurer.measure(formatNumber(v), axisStyle)
                drawText(t, topLeft = Offset(plotW + 6.dp.toPx(), (y - t.size.height / 2f).coerceIn(0f, plotH - t.size.height)))
            }

            // barras: ponta arredondada (4 dp), base reta na linha do zero
            val r = 4.dp.toPx()
            bars.forEachIndexed { i, b ->
                val h = if (b.value <= 0f) 0f else (plotH * (b.value / maxV)).coerceAtLeast(3.dp.toPx())
                val x = slot * i + (slot - barW) / 2f
                val isSel = i == selected
                if (h > 0f) {
                    val rr = CornerRadius(minOf(r, h, barW / 2f))
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = x, top = plotH - h, right = x + barW, bottom = plotH,
                                topLeftCornerRadius = rr, topRightCornerRadius = rr,
                                bottomRightCornerRadius = CornerRadius.Zero, bottomLeftCornerRadius = CornerRadius.Zero,
                            ),
                        )
                    }
                    drawPath(path, color)
                }
                // rótulos do eixo x: todos se couberem, senão um sim, um não (sempre o escolhido)
                val step = if (bars.size > 8) 2 else 1
                if (isSel || (bars.size - 1 - i) % step == 0) {
                    val t = measurer.measure(
                        b.label,
                        if (isSel) labelStyle.copy(color = c.label) else labelStyle,
                    )
                    drawText(t, topLeft = Offset(slot * i + (slot - t.size.width) / 2f, plotH + 4.dp.toPx()))
                }
            }

            // média (linha tracejada)
            if (showAverage && hasData && nonZero.size >= 2) {
                val y = plotH - plotH * (average / maxV)
                drawLine(
                    c.label.copy(alpha = 0.45f),
                    Offset(0f, y),
                    Offset(plotW, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
            }

            if (!hasData) {
                val t = measurer.measure("Sem dados ainda", TextStyle(color = c.tertiary, fontSize = axisStyle.fontSize))
                drawText(t, topLeft = Offset((plotW - t.size.width) / 2f, (plotH - t.size.height) / 2f))
            }
        }
        if (showAverage && nonZero.size >= 2) {
            Spacer(Modifier.height(6.dp))
            Text(
                "– – média dos períodos com registro: ${formatNumber(average)} $unit",
                style = Ios.type.caption1,
                color = c.secondary,
            )
        }
    }
}

/** Arredonda para cima em passos de 0,5 (para rótulos de minutos). */
fun roundHalfUp(v: Float): Float = ceil(v * 2f) / 2f
