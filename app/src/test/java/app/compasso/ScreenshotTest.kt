package app.compasso

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.core.content.ContextCompat
import app.compasso.data.Settings
import app.compasso.games.GameKind
import app.compasso.model.Accent
import app.compasso.model.ArticleParser
import app.compasso.model.Codename
import app.compasso.model.FocusSettings
import app.compasso.model.GateRule
import app.compasso.model.Lapse
import app.compasso.model.Levels
import app.compasso.model.Outcome
import app.compasso.model.PacingConfig
import app.compasso.model.PacingSettings
import app.compasso.model.PacingSnapshot
import app.compasso.model.PendingChange
import app.compasso.model.PhaseType
import app.compasso.model.PointEntry
import app.compasso.model.PointKind
import app.compasso.model.Progress
import app.compasso.model.ReleaseGate
import app.compasso.model.SessionRecord
import app.compasso.model.Tip
import app.compasso.ui.AboutScreen
import app.compasso.ui.BreatheContent
import app.compasso.ui.CustomLevelScreen
import app.compasso.ui.FocusPrepScreen
import app.compasso.ui.GuideScreen
import app.compasso.ui.HistoryScreen
import app.compasso.ui.IntroScreen
import app.compasso.ui.OptionsActions
import app.compasso.ui.OptionsScreen
import app.compasso.ui.PacingInfoScreen
import app.compasso.ui.PinSetupScreen
import app.compasso.ui.ProfileScreen
import app.compasso.ui.ProgressScreen
import app.compasso.ui.ReadScreen
import app.compasso.ui.ReaderScreen
import app.compasso.ui.RefugeScreen
import app.compasso.ui.SessionScreen
import app.compasso.ui.SessionUi
import app.compasso.ui.SummaryScreen
import app.compasso.ui.SummaryUi
import app.compasso.ui.Tab
import app.compasso.ui.TodayScreen
import app.compasso.ui.WelcomeScreen
import app.compasso.ui.components.ActionSheetContent
import app.compasso.ui.components.AlertAction
import app.compasso.ui.components.AlertCard
import app.compasso.ui.components.AppIconTile
import app.compasso.ui.components.FloatingTabBar
import app.compasso.ui.components.FloatingTabBarSpace
import app.compasso.ui.games.BlocksScreen
import app.compasso.ui.games.EchoScreen
import app.compasso.ui.games.MergeScreen
import app.compasso.ui.tabItems
import app.compasso.ui.theme.CompassoTheme
import app.compasso.ui.theme.Ios
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Gera imagens das telas em app/build/shots para conferir o visual sem um celular. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w392dp-h850dp-xhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val now = System.currentTimeMillis()
    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun shot(name: String, dark: Boolean = true, accent: Accent = Accent.TEAL, content: @Composable () -> Unit) {
        compose.setContent {
            CompassoTheme(dark = dark, accent = accent) {
                Box(Modifier.fillMaxSize().background(Ios.colors.bg)) { content() }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/shots/$name.png")
    }

    /** Tela de aba com a barra de abas embaixo, como no app. */
    @Composable
    private fun WithTabs(tab: Tab, content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            content()
            FloatingTabBar(tabItems, tab.ordinal, {}, Modifier.align(Alignment.BottomCenter))
        }
    }

    private val open = ReleaseGate.Open(24, false)
    private val closed = ReleaseGate.Closed(now + 9 * hour + 20 * 60_000L, setOf(GateRule.INTERVAL), 24, false)
    private val codename = Codename("Coruja Serena 214", 42L)

    private fun ui(
        phase: PhaseType,
        remaining: Long = 23_000,
        duration: Long = 45_000,
        elapsedInPhase: Long = duration - remaining,
        paused: Boolean = false,
        controlOnly: Boolean = false,
        notice: String? = null,
    ) = SessionUi(
        level = Levels.MEDIUM,
        phase = phase,
        round = 3,
        totalRounds = 7,
        phaseRemainingMs = remaining,
        phaseDurationMs = duration,
        phaseElapsedMs = elapsedInPhase,
        bpm = 95,
        nextBpm = 105,
        beatCount = 0,
        closeCount = 1,
        elapsedMs = 3 * 60_000L + 12_000L,
        paused = paused,
        controlOnly = controlOnly,
        notice = notice,
        focused = true,
    )

    private fun rec(ago: Long, levelName: String, minutes: Int, close: Int, outcome: Outcome, done: Int, total: Int, controlOnly: Boolean = false) =
        SessionRecord(
            startedAt = now - ago,
            endedAt = now - ago + minutes * 60_000L,
            levelId = levelName.lowercase(),
            levelName = levelName,
            durationMs = minutes * 60_000L + 17_000L,
            roundsDone = done,
            roundsTotal = total,
            closeCount = close,
            maxBpm = 130,
            outcome = outcome,
            controlOnly = controlOnly,
            focused = true,
        )

    private val records = listOf(
        rec(2 * hour, "Médio", 8, 1, Outcome.DENIED, 7, 7, controlOnly = true),
        rec(5 * hour, "Fácil", 3, 1, Outcome.RELEASED, 4, 4),
        rec(day + hour, "Impossível", 21, 3, Outcome.DENIED, 12, 12),
        rec(2 * day, "Difícil", 9, 2, Outcome.LOST_CONTROL, 6, 10),
        rec(3 * day, "Médio", 8, 0, Outcome.RELEASED, 7, 7),
        rec(4 * day, "Fácil", 1, 0, Outcome.STOPPED, 1, 4),
        rec(5 * day, "Médio", 11, 1, Outcome.DENIED, 7, 7),
        rec(6 * day, "Médio", 4, 2, Outcome.INTERRUPTED, 3, 7),
        rec(8 * day, "Difícil", 13, 1, Outcome.DENIED, 10, 10),
        rec(10 * day, "Médio", 7, 0, Outcome.RELEASED, 7, 7),
        rec(12 * day, "Fácil", 5, 1, Outcome.RELEASED, 4, 4),
    )

    /** ~3 semanas de constância (um dia sem nada no meio). */
    private val points: List<PointEntry> = (0 until 22).filter { it != 9 }.flatMap { d ->
        val t = now - d * day - hour
        buildList {
            add(PointEntry(t, PointKind.CHECKIN, 10))
            if (d % 2 == 0) add(PointEntry(t, PointKind.CONTROL_SESSION, 15)) else add(PointEntry(t, PointKind.BREATHE, 5))
            if (d % 3 == 0) add(PointEntry(t, PointKind.GAME, 3))
            if (d % 5 == 0) add(PointEntry(t, PointKind.ARTICLE, 5))
        }
    }
    private val level = Progress.level(points, records.mapNotNull { Lapse.from(it) }, now)

    private val articles = File("src/main/assets/artigos").listFiles().orEmpty()
        .filter { it.name.endsWith(".md") }
        .map { ArticleParser.parse(it.readText()) }
        .sortedBy { it.order }

    private fun home(gate: ReleaseGate, tip: Tip? = null, focus: FocusSettings = FocusSettings(), initialLevel: Int = 0) = @Composable {
        WithTabs(Tab.TRAIN) {
            TodayScreen(
                gate, Levels.DEFAULT_CUSTOM, focus, codename, level, points, records.mapNotNull { Lapse.from(it) }, tip,
                {}, {}, {}, {}, {}, {}, {},
                bottomPadding = FloatingTabBarSpace, now = now, initialLevel = initialLevel,
            )
        }
    }

    // ---------- Boas-vindas e apresentação ----------

    @Test fun welcome() = shot("00_boas_vindas") { WelcomeScreen {} }

    @Test fun intro() = shot("00b_apresentacao", dark = false) { IntroScreen(codename, {}, {}) }

    @Test fun introCodename() = shot("00c_apresentacao_codinome") { IntroScreen(codename, {}, {}, startPage = 4) }

    // ---------- Treinar ----------

    @Test fun homeDark() = shot("01_hoje") { home(open, initialLevel = 1)() }

    @Test
    @Config(qualifiers = "w392dp-h1500dp-xhdpi")
    fun homeFull() = shot("01a_hoje_inteiro") { home(closed, Tip.TRAIN)() }

    @Test fun homeLight() = shot("01b_hoje_dia", dark = false) { home(closed, Tip.TRAIN)() }

    @Test fun homeClosed() = shot("01d_hoje_so_controle") { home(closed)() }

    @Test fun homeFocusOff() = shot("01c_hoje_foco_desligando") {
        home(ReleaseGate.Off, focus = FocusSettings(enabled = true, offAt = now + 20 * hour), initialLevel = 4)()
    }

    @Test
    fun logo() = shot("17b_logo") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppIconTile(size = 180.dp) }
    }

    @Test fun countdownControl() = shot("02_contagem_controle") {
        SessionScreen(ui(PhaseType.COUNTDOWN, 2_400, 3_000, controlOnly = true), true, {}, {}, {}, {})
    }

    @Test fun go() = shot("03_ritmo") { SessionScreen(ui(PhaseType.GO), true, {}, {}, {}, {}) }

    @Test fun stopInhaleHalf() = shot("04b_pare_inspira_meio") {
        SessionScreen(ui(PhaseType.STOP, 18_000, 20_000, elapsedInPhase = 2_000), true, {}, {}, {}, {})
    }

    @Test fun stopExhale() = shot("04d_pare_solta") {
        SessionScreen(ui(PhaseType.STOP, 12_500, 20_000, elapsedInPhase = 7_500), true, {}, {}, {}, {})
    }

    @Test fun release() = shot("05_liberado") {
        SessionScreen(ui(PhaseType.RELEASE, 0, 0).copy(round = 7, elapsedMs = 8 * 60_000L + 41_000L), true, {}, {}, {}, {})
    }

    @Test fun deniedControl() = shot("06_fim_controle") {
        SessionScreen(ui(PhaseType.DENIED, 0, 0, controlOnly = true).copy(round = 7), true, {}, {}, {}, {})
    }

    @Test fun pausedNotice() = shot("07_pausado_saiu_do_app") {
        SessionScreen(
            ui(
                PhaseType.GO, paused = true,
                notice = "Você saiu do app. Com o modo foco, sair por mais de 10 s encerra a sessão. Restam 2 saídas.",
            ),
            true, {}, {}, {}, {},
        )
    }

    @Test fun focusPrep() = shot("07b_modo_foco") { FocusPrepScreen({}, {}, {}) }

    @Test fun breathe() = shot("08_respirar") { BreatheContent(elapsedMs = 33_000, durationMs = 120_000, onClose = {}, onMore = {}) }

    @Test fun breatheDone() = shot("08b_respirar_pronto") { BreatheContent(elapsedMs = 120_000, durationMs = 120_000, onClose = {}, onMore = {}, sound = false) }

    @Test fun summary() = shot("09_resumo_pontos", dark = false) {
        SummaryScreen(
            SummaryUi(records[0], saved = true, gate = closed, points = PointEntry(now, PointKind.CONTROL_SESSION, 15)),
            {}, {}, now, tip = Tip.SUMMARY,
        )
    }

    @Test fun summaryInterrupted() = shot("09b_resumo_interrompida") {
        SummaryScreen(
            SummaryUi(records[7], saved = true, gate = closed, noPoints = "Sessão interrompida não vale pontos e quebra a sequência de hoje."),
            {}, {}, now,
        )
    }

    @Test fun history() = shot("10_historico") { HistoryScreen(records, {}, now) }

    @Test fun pacingInfo() = shot("12_controle_detalhes", dark = false) {
        PacingInfoScreen(PacingSnapshot(closed, now - 15 * hour, 3), PacingSettings(), {}, {}, now)
    }

    @Test fun custom() = shot("13_personalizado") { CustomLevelScreen(Levels.DEFAULT_CUSTOM, {}, {}, {}) }

    @Test fun pin() = shot("14_senha", dark = false) { PinSetupScreen(firstRun = true, onDone = {}, onSkip = {}) }

    @Test fun alert() = shot("15_alerta_bloqueado", dark = false) {
        home(closed)()
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            AlertCard(
                "Sessão só de controle",
                "A liberação volta hoje às 10:41. Ainda não passaram 24 h desde a última liberação. Esta sessão vai até o fim sem liberação.",
                listOf(AlertAction("Começar treino", {}, bold = true), AlertAction("Respirar 2 min", {}), AlertAction("Cancelar", {})),
            )
        }
    }

    @Test fun actionSheet() = shot("16_encerrar") {
        SessionScreen(ui(PhaseType.GO, paused = true), true, {}, {}, {}, {})
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.BottomCenter) {
            ActionSheetContent(
                "Encerrar a sessão?",
                "Se não deu pra segurar até o fim, marque abaixo. Isso conta no seu progresso.",
                listOf(AlertAction("Não segurei", {}), AlertAction("Só encerrar", {}, destructive = true)),
                AlertAction("Continuar sessão", {}),
            )
        }
    }

    // ---------- Refúgio, jogos ----------

    @Test fun refuge() = shot("18_refugio") {
        WithTabs(Tab.REFUGE) {
            RefugeScreen(mapOf(GameKind.BLOCKS to 1240, GameKind.ECHO to 9), Tip.REFUGE, {}, {}, {}, {}, {}, bottomPadding = FloatingTabBarSpace)
        }
    }

    @Test fun blocks() = shot("26_encaixe", dark = false) { BlocksScreen(860, {}, {}, {}, {}) }

    @Test fun merge() = shot("27_dobra") { MergeScreen(2048, {}, {}, {}, {}) }

    @Test fun echo() = shot("28_eco", dark = false) { EchoScreen(7, {}, {}, {}, {}) }

    // ---------- Evolução ----------

    @Test
    @Config(qualifiers = "w392dp-h2600dp-xhdpi")
    fun progress() = shot("19_evolucao_completa", dark = false) {
        ProgressScreen(level, codename, points, records, Tip.PROGRESS, {}, {}, {}, {}, now = now)
    }

    @Test fun progressDark() = shot("19b_evolucao_escuro", accent = Accent.GOLD) {
        WithTabs(Tab.PROGRESS) {
            ProgressScreen(level, codename, points, records, null, {}, {}, {}, {}, bottomPadding = FloatingTabBarSpace, now = now)
        }
    }

    @Test fun profile() = shot("23_perfil") { ProfileScreen(codename, level, Accent.GOLD, {}, {}, {}) }

    // ---------- Ler ----------

    @Test fun read() = shot("20_ler", dark = false) {
        WithTabs(Tab.READ) {
            ReadScreen(articles, setOf("gatilhos"), null, {}, {}, bottomPadding = FloatingTabBarSpace)
        }
    }

    @Test fun reader() = shot("21_artigo") { ReaderScreen(articles.first(), isRead = false, onRead = {}, onBack = {}) }

    @Test
    @Config(qualifiers = "w392dp-h3400dp-xhdpi")
    fun readerFull() = shot("21b_artigo_inteiro", dark = false) {
        ReaderScreen(articles.first { it.id == "precoce" }, isRead = true, onRead = {}, onBack = {})
    }

    // ---------- Ajustes ----------

    @Test
    @Config(qualifiers = "w392dp-h3000dp-xhdpi")
    fun options() = shot("11_ajustes_completo", dark = false) {
        OptionsScreen(Settings(), PacingSettings(PacingConfig(true, 24)), FocusSettings(), true, records.size, codename, level, null, OptionsActions(), now = now)
    }

    @Test fun optionsPending() = shot("11b_ajustes_mudanca_agendada") {
        val pending = PacingSettings(PacingConfig(true, 24), PendingChange(PacingConfig(true, 12), now + 20 * hour))
        OptionsScreen(Settings(), pending, FocusSettings(true, now + 20 * hour), false, 0, codename, level, Tip.SETTINGS, OptionsActions(), now = now)
    }

    @Test fun guide() = shot("29_como_usar", dark = false) { GuideScreen({}, {}) }

    @Test fun about() = shot("24_licenca_creditos", dark = false) { AboutScreen({}, {}) }

    /** Ícone do app como aparece na tela inicial (máscara redonda). */
    @Test
    fun icon() {
        val ctx: Context = RuntimeEnvironment.getApplication()
        val drawable = ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)!!
        val size = 432
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.clipPath(Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) })
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        File("build/shots").mkdirs()
        FileOutputStream(File("build/shots/17_icone.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
