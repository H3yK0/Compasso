package app.compasso.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.compasso.BuildConfig
import app.compasso.Credits
import app.compasso.ui.components.AppIconTile
import app.compasso.ui.components.IconInset
import app.compasso.ui.components.IosNavBar
import app.compasso.ui.components.IosRow
import app.compasso.ui.components.LargeTitleList
import app.compasso.ui.components.RowDivider
import app.compasso.ui.components.RowIcon
import app.compasso.ui.components.section
import app.compasso.ui.theme.Ios

/** Arquivos de licença que o app mostra (assets). */
enum class LicenseDoc(val title: String, val file: String) {
    AGPL("GNU AGPL v3.0", "licencas/AGPL-3.0.txt"),
    FONT("Fonte Inter (OFL 1.1)", "inter-ofl.txt"),
    SERIF("Fonte Cormorant Garamond (OFL 1.1)", "cormorant-ofl.txt"),
}

/** Licença e créditos ("avisos legais apropriados" que a AGPL pede numa tela do app). */
@Composable
fun AboutScreen(onLicense: (LicenseDoc) -> Unit, onBack: () -> Unit) {
    val c = Ios.colors
    LargeTitleList("Licença e créditos", onBack = onBack, backLabel = "Ajustes") {
        item(key = "marca") {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIconTile(size = 84.dp)
                Spacer(Modifier.height(10.dp))
                Text("Compasso", style = Ios.type.serifSection, color = c.label)
                Text("Versão ${BuildConfig.VERSION_NAME}", style = Ios.type.subhead, color = c.secondary)
            }
        }
        section(
            header = "Licenças",
            footer = "Compasso © ${Credits.YEAR} ${Credits.COPYRIGHT_HOLDER}. Este programa é software livre: você pode redistribuí-lo " +
                "e/ou modificá-lo sob os termos da GNU Affero General Public License, versão 3. É distribuído na esperança de ser " +
                "útil, mas SEM NENHUMA GARANTIA.",
            key = "licencas",
        ) {
            IosRow(
                "Código: GNU AGPL v3.0",
                icon = RowIcon(Icons.Rounded.Code, c.indigo),
                chevron = true,
                onClick = { onLicense(LicenseDoc.AGPL) },
            )
            RowDivider(IconInset)
            IosRow("Artigos", subtitle = "Creative Commons BY-SA 4.0", icon = RowIcon(Icons.AutoMirrored.Rounded.MenuBook, c.blue))
            RowDivider(IconInset)
            IosRow("Fonte Inter", subtitle = "SIL Open Font License 1.1", icon = RowIcon(Icons.Rounded.FontDownload, c.teal), chevron = true, onClick = { onLicense(LicenseDoc.FONT) })
            RowDivider(IconInset)
            IosRow("Fonte Cormorant Garamond", subtitle = "SIL Open Font License 1.1", icon = RowIcon(Icons.Rounded.FontDownload, c.lavender), chevron = true, onClick = { onLicense(LicenseDoc.SERIF) })
            RowDivider(IconInset)
            IosRow("Sons e trilha", subtitle = "Gerados pelo próprio app", icon = RowIcon(Icons.Rounded.GraphicEq, c.pink))
        }
        section(
            header = "Código-fonte",
            footer = Credits.SOURCE_NOTE,
            key = "fonte",
        ) {
            IosRow("Versões modificadas", subtitle = "Devem avisar que foram alteradas e seguir na AGPL v3.0")
        }
    }
}

/** Texto de uma licença, em parágrafos (a AGPL é longa). */
@Composable
fun LicenseScreen(doc: LicenseDoc, onBack: () -> Unit) {
    val c = Ios.colors
    val context = LocalContext.current
    val paragraphs = remember(doc) {
        runCatching { context.assets.open(doc.file).bufferedReader(Charsets.UTF_8).use { it.readText() } }
            .getOrDefault("Arquivo não encontrado.")
            .replace("\r\n", "\n")
            .split(Regex("\n\\s*\n"))
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
    }
    val state = rememberLazyListState()
    val rangePx = with(LocalDensity.current) { 36.dp.toPx() }
    val collapsed by remember(state) {
        derivedStateOf { if (state.firstVisibleItemIndex > 0) 1f else (state.firstVisibleItemScrollOffset / rangePx).coerceIn(0f, 1f) }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().background(c.card)) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = top + 52.dp, bottom = bottom + 32.dp, start = 18.dp, end = 18.dp),
        ) {
            item { Text(doc.title, style = Ios.type.title2, color = c.label, modifier = Modifier.padding(bottom = 12.dp)) }
            items(paragraphs) { p ->
                Text(
                    p,
                    style = Ios.type.footnote.copy(fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = c.label,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
        IosNavBar(doc.title, collapsed, onBack = onBack, backLabel = "Licença")
    }
}
