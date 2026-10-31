package app.compasso

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import app.compasso.data.ActiveSession
import app.compasso.data.Store
import app.compasso.model.Outcome
import app.compasso.model.PointKind
import app.compasso.model.SessionRecord
import app.compasso.ui.AppViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Fluxos completos com o app de verdade (Activity + ViewModel + sons + vibração + fixar tela).
 * Cada teste limpa os dados antes de abrir o app, como uma instalação nova.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "pt-rBR-w392dp-h850dp-xhdpi")
class FlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun freshApp(): Context {
        val app: Context = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("compasso", Context.MODE_PRIVATE).edit().clear().commit()
        return app
    }

    /** Instalação que já passou pela apresentação (para testar o resto do app direto). */
    private fun readyApp(): Context {
        val app = freshApp()
        Store(app).apply {
            pinOffered = true
            seenVersion = AppViewModel.CURRENT_VERSION
        }
        return app
    }

    /** Avança o relógio do Compose e o do app (o timer da sessão roda no looper principal). */
    private fun tick(ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.waitForIdle()
    }

    private fun typePin(pin: String) {
        pin.forEach { c ->
            compose.onNodeWithText(c.toString()).performClick()
            tick(30)
        }
        tick(500)
    }

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        tick(300)
    }

    /** Começa o Fácil: com o modo foco, o app pede para fixar a tela e começa sozinho. */
    private fun startEasy() {
        compose.onNodeWithText("Começar Fácil").performScrollTo().performClick()
        tick(1_000)
    }

    /** Tela Hoje aberta (a semana em estrelas fica no fim dela). */
    private fun assertToday() {
        compose.onNodeWithText("Sua semana").assertExists()
    }

    /** Primeira abertura → boas-vindas → apresentação → senha → Fácil (tela fixada) → … → histórico. */
    @Test
    fun fullSessionFlow() {
        val app = freshApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Um refúgio para retomar o controle").assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_0_boas_vindas.png")
            compose.onNodeWithText("Começar").performClick()
            tick(800)
            compose.onNodeWithText("Um refúgio, não uma prisão").assertExists()
            compose.onNodeWithText("Pular").performClick()
            tick(800)
            compose.onNodeWithText("Crie uma senha").assertExists()
            typePin("2580")
            compose.onNodeWithText("Confirme a senha").assertExists()
            typePin("2580")
            tick(800)

            assertToday()
            compose.onNodeWithText("Liberação disponível").assertExists()
            compose.onNodeWithText("Comece pelo Fácil").assertExists() // dica da primeira visita
            compose.onRoot().captureRoboImage("build/shots/flow_0b_hoje.png")
            startEasy()
            compose.onNodeWithText("Começa em").assertExists()

            tick(4_000)
            compose.onNodeWithText("Ritmo").assertExists()
            compose.onNodeWithText("Rodada 1 de 4", substring = true).assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_1_ritmo.png")

            // "Estou perto": para na hora e começa a respiração guiada
            compose.onNodeWithText("Estou perto").performClick()
            tick(1_000)
            compose.onNodeWithText("Pare").assertExists()
            compose.onNodeWithText("Inspire").assertExists()
            compose.onNodeWithText("Ainda perto · +10 s").assertExists()
            tick(1_500)
            compose.onRoot().captureRoboImage("build/shots/flow_2_pare_respira.png")
            tick(3_000) // passou dos 4 s: hora de soltar o ar
            compose.onNodeWithText("Solte o ar").assertExists()

            // pausa extra do Fácil: máx(15, 20) + 10 = 30 s
            tick(20_000)
            compose.onNodeWithText("Pare").assertExists()
            tick(5_000)
            compose.onNodeWithText("Ritmo").assertExists()
            compose.onNodeWithText("Rodada 2 de 4", substring = true).assertExists()

            // pausar congela o tempo
            compose.onNodeWithContentDescription("Pausar").performClick()
            tick(300)
            compose.onNodeWithText("Pausado").assertExists()
            tick(60_000)
            compose.onNodeWithText("Rodada 2 de 4", substring = true).assertExists()
            compose.onNodeWithText("Continuar").performClick()
            tick(300)

            // encerrar marcando "Não aguentei"
            compose.onNodeWithContentDescription("Encerrar sessão").performClick()
            tick(600)
            compose.onNodeWithText("Encerrar a sessão?").assertExists()
            compose.onNodeWithText("Não aguentei").performClick()
            tick(800)
            compose.onNodeWithText("Não foi dessa vez").assertExists()
            compose.onNodeWithText("Salvo no histórico").assertExists()
            compose.onNodeWithText("Próxima liberação").assertExists() // "não aguentei" conta como liberação
            compose.onNodeWithText("+3").assertExists() // honestidade vale pontos (sessão em foco)
            compose.onRoot().captureRoboImage("build/shots/flow_3_resumo.png")

            compose.onNodeWithText("Concluído").performScrollTo().performClick()
            tick(800)
            compose.onNodeWithText("Só treino de controle").assertExists()
            val history: List<SessionRecord> = Store(app).loadHistory()
            assertTrue(history.single().focused)

            compose.onNodeWithText("Evolução").performClick()
            tick(800)
            scrollTo("Histórico de sessões")
            compose.onNodeWithText("Histórico de sessões").performClick()
            tick(800)
            compose.onNodeWithText("Não aguentou").assertExists()
            compose.onNodeWithText("perto 1×", substring = true).assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_4_historico.png")
        }
    }

    /** Com senha salva, o app abre travado; senha errada é recusada, a certa libera. */
    @Test
    fun opensLockedWithSavedPin() {
        Store(readyApp()).setPin("1357")
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Digite sua senha").assertExists()
            typePin("0000")
            compose.onNodeWithText("Senha incorreta").assertExists()
            typePin("1357")
            assertToday()
        }
    }

    /** Fácil até o fim libera e rende pontos; logo depois, a próxima vira só treino. */
    @Test
    fun releaseThenNextSessionIsControlOnly() {
        val app = readyApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            startEasy()
            // Fácil: no máximo 3 s + 4×40 s de ritmo + 3×15 s de pausa = 208 s
            repeat(22) { tick(10_000) }
            compose.onNodeWithText("Liberado").assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_5_liberado.png")
            compose.onNodeWithText("Concluir").performClick()
            tick(800)
            compose.onNodeWithText("Sessão concluída").assertExists()
            compose.onNodeWithText("4 de 4").assertExists()
            compose.onNodeWithText("+10").assertExists()
            assertEquals(PointKind.SESSION, Store(app).loadPoints().single().kind)

            compose.onNodeWithText("Concluído").performScrollTo().performClick()
            tick(800)
            compose.onNodeWithText("Só treino de controle").assertExists()

            // tentar outro nível (deslizando os cartões): o app avisa e oferece respirar
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
            tick(800)
            compose.onNodeWithText("Começar Médio").performScrollTo().performClick()
            tick(600)
            compose.onNodeWithText("Respirar 2 min").assertExists()
            compose.onNodeWithText("Começar treino").performClick()
            tick(1_000)
            compose.onNodeWithText("Hoje vai até o fim sem liberação.").assertExists()
            compose.onNodeWithText("só controle", substring = true).assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_6_controle.png")
        }
    }

    /** Sair do app no meio da sessão (modo foco): mais de 10 s fora encerra como interrompida. */
    @Test
    fun leavingTheAppInterruptsTheSession() {
        val app = readyApp()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            tick(500)
            startEasy()
            tick(5_000)
            compose.onNodeWithText("Ritmo").assertExists()

            scenario.moveToState(Lifecycle.State.CREATED) // outro app na frente
            tick(11_000)
            scenario.moveToState(Lifecycle.State.RESUMED)
            tick(800)
            compose.onNodeWithText("Sessão interrompida").assertExists()
            compose.onNodeWithText("Sessão interrompida não vale pontos e quebra a sequência de hoje.").assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_7_interrompida.png")
            assertEquals(Outcome.INTERRUPTED, Store(app).loadHistory().single().outcome)
            assertTrue(Store(app).loadPoints().isEmpty())
            assertTrue(Store(app).loadPacingLog().single().released) // conta como liberação no controle
        }
    }

    /** Saída curta: a sessão pausa, avisa quantas saídas restam e continua. */
    @Test
    fun shortExitPausesWithWarning() {
        readyApp()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            tick(500)
            startEasy()
            tick(5_000)
            scenario.moveToState(Lifecycle.State.CREATED)
            tick(3_000)
            scenario.moveToState(Lifecycle.State.RESUMED)
            tick(800)
            compose.onNodeWithText("Pausado").assertExists()
            compose.onNodeWithText("Restam 2 saídas", substring = true).assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_8_saida_curta.png")
            compose.onNodeWithText("Continuar").performClick()
            tick(500)
            compose.onNodeWithText("Ritmo").assertExists()
        }
    }

    /** Sessão que ficou aberta quando o app foi fechado vira "interrompida" na próxima abertura. */
    @Test
    fun orphanSessionIsRecordedAsInterrupted() {
        val app = readyApp()
        val t = System.currentTimeMillis() - 3_600_000L
        Store(app).saveActive(ActiveSession(t, "facil", "Fácil", 4, true, false, t + 90_000L, 85_000L, 2, 1, 110))
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            val h = Store(app).loadHistory().single()
            assertEquals(Outcome.INTERRUPTED, h.outcome)
            assertEquals(85_000L, h.durationMs)
            assertFalse(h.focused)
            assertEquals(null, Store(app).loadActive())
        }
    }

    /** Tela Respirar: 2 minutos guiados, rende pontos e volta. */
    @Test
    fun breatheScreen() {
        val app = readyApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Vontade apertou?").performScrollTo().performClick()
            tick(119_000)
            tick(2_000) // o bloco de 2 min termina aqui; o aviso de pontos fica 2,6 s na tela
            compose.onNodeWithText("Pronto").assertExists()
            compose.onNodeWithText("+5 pontos").assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_8b_respirar_pronto.png")
            compose.onNodeWithText("Voltar").performClick()
            tick(3_000)
            assertToday()
            assertEquals(PointKind.BREATHE, Store(app).loadPoints().single().kind)
        }
    }

    /** Abas: Refúgio, um jogo, check-in na Evolução e um artigo lido até o fim. */
    @Test
    fun tabsGamesCheckInAndReading() {
        val app = readyApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Refúgio").performClick()
            tick(500)
            compose.onNodeWithText("Ler algo bom").assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_9_refugio.png")
            compose.onNodeWithText("Dobra").performClick()
            tick(800)
            compose.onNodeWithText("Deslize para qualquer lado. Iguais se juntam.").assertExists()
            compose.onNodeWithText("Fechar").performClick()
            tick(800)

            compose.onNodeWithText("Evolução").performClick()
            tick(500)
            compose.onNodeWithText("Sim, fiquei longe · +10").performClick()
            tick(500)
            compose.onNodeWithText("Check-in feito hoje").assertExists()
            compose.onRoot().captureRoboImage("build/shots/flow_10_evolucao.png")

            compose.onNodeWithText("Ler").performClick()
            tick(800)
            compose.onNodeWithText("A vontade vem em ondas").performClick()
            tick(800)
            compose.onNodeWithText("Mente e hábitos").assertExists()
            scrollTo("Fontes")
            tick(33_000)
            tick(2_000) // tempo mínimo de leitura (36 s) termina aqui
            compose.onNodeWithText("+5 pontos").assertExists()
            assertTrue("ondas" in Store(app).loadRead())
            val kinds = Store(app).loadPoints().map { it.kind }.toSet()
            assertEquals(setOf(PointKind.CHECKIN, PointKind.ARTICLE), kinds)
        }
    }

    /** Senha: os erros ficam gravados (fechar e abrir o app não zera a espera). */
    @Test
    fun wrongPinWaitSurvivesRestart() {
        val app = readyApp()
        Store(app).setPin("1357")
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            repeat(5) { typePin("0000") }
            compose.onNodeWithText("Muitas tentativas", substring = true).assertExists()
        }
        assertEquals(5, Store(app).pinFails)
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Muitas tentativas", substring = true).assertExists()
            typePin("1357") // ainda esperando: nem confere
            compose.onNodeWithText("Digite sua senha").assertExists()
            tick(31_000)
            tick(2_000) // a tela confere a espera de segundo em segundo
            typePin("1357")
            assertToday()
            assertEquals(0, Store(app).pinFails)
        }
    }

    /** Trocar ou remover a senha pede a senha atual (quem pegou o celular destravado não consegue). */
    @Test
    fun changingOrRemovingPinAsksForTheCurrentOne() {
        val app = readyApp()
        Store(app).setPin("1357")
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            typePin("1357")
            compose.onNodeWithContentDescription("Ajustes e perfil").performClick()
            tick(800)
            scrollTo("Trocar senha")
            compose.onNodeWithText("Trocar senha").performClick()
            tick(800)
            compose.onNodeWithText("Digite a senha atual").assertExists()
            typePin("2468")
            compose.onNodeWithText("Senha incorreta").assertExists()
            typePin("1357")
            tick(800)
            compose.onNodeWithText("Crie uma senha").assertExists()
            compose.onNodeWithText("Cancelar").performClick()
            tick(800)

            scrollTo("Senha ao abrir")
            compose.onNodeWithText("Senha ao abrir").performClick()
            tick(800)
            compose.onNodeWithText("Para remover a senha, confirme a atual.").assertExists()
            typePin("1357")
            tick(800)
            assertFalse(Store(app).hasPin())
        }
    }

    /** Com o histórico desligado, "não aguentei" não aparece na lista, mas continua valendo para o Invicto. */
    @Test
    fun lapseIsKeptEvenWithHistoryOff() {
        val app = readyApp()
        Store(app).saveSettings(Store(app).loadSettings().copy(keepHistory = false))
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            startEasy()
            tick(5_000)
            compose.onNodeWithContentDescription("Encerrar sessão").performClick()
            tick(600)
            compose.onNodeWithText("Não aguentei").performClick()
            tick(800)
            assertTrue(Store(app).loadHistory().isEmpty())
            assertEquals(Outcome.LOST_CONTROL, Store(app).loadLapses().single().outcome)
        }
    }

    /** Gráfico de sessões sem nenhuma sessão: tocar nele não pode fechar o app. */
    @Test
    fun tappingAnEmptyChartDoesNotCrash() {
        readyApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Evolução").performClick()
            tick(500)
            scrollTo("Sessões")
            compose.onNodeWithText("Sessões").performClick()
            tick(300)
            compose.onNodeWithContentDescription("Seus tempos de treino aparecem aqui").performClick()
            tick(300)
            compose.onNodeWithText("Tempo de controle").assertExists()
        }
    }
}
