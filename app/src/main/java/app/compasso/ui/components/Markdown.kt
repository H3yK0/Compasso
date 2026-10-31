package app.compasso.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/** Converte **negrito** e *itálico* do texto dos artigos em texto formatado. */
fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var bold = false
    var italic = false
    var i = 0
    val chunk = StringBuilder()

    fun flush() {
        if (chunk.isEmpty()) return
        val style = SpanStyle(
            fontWeight = if (bold) FontWeight.SemiBold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
        )
        if (bold || italic) {
            pushStyle(style)
            append(chunk.toString())
            pop()
        } else {
            append(chunk.toString())
        }
        chunk.clear()
    }

    while (i < text.length) {
        val ch = text[i]
        when {
            ch == '*' && i + 1 < text.length && text[i + 1] == '*' -> {
                flush()
                bold = !bold
                i += 2
            }
            ch == '*' && (italic || (i + 1 < text.length && !text[i + 1].isWhitespace())) -> {
                flush()
                italic = !italic
                i += 1
            }
            else -> {
                chunk.append(ch)
                i += 1
            }
        }
    }
    flush()
}
