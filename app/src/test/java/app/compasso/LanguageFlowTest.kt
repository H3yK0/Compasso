package app.compasso

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import app.compasso.data.AppLanguage
import app.compasso.data.Store
import app.compasso.ui.AppViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** O app inteiro em inglês e em espanhol, e a troca de idioma nos ajustes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-rUS-w392dp-h850dp-xhdpi")
class LanguageFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun freshApp(): Context {
        val app: Context = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("compasso", Context.MODE_PRIVATE).edit().clear().commit()
        return app
    }

    private fun readyApp(): Context {
        val app = freshApp()
        Store(app).apply {
            pinOffered = true
            seenVersion = AppViewModel.CURRENT_VERSION
        }
        return app
    }

    private fun tick(ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.waitForIdle()
    }

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        tick(300)
    }

    /** Celular em inglês: boas-vindas, apresentação, Hoje e um artigo, tudo em inglês. */
    @Test
    fun englishFromTheFirstScreen() {
        freshApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("A refuge to take back control").assertExists()
            compose.onRoot().captureRoboImage("build/shots/lang_en_0_welcome.png")
            compose.onNodeWithText("Start").performClick()
            tick(800)
            compose.onNodeWithText("A refuge, not a prison").assertExists()
            compose.onNodeWithText("Skip").performClick()
            tick(800)
            compose.onNodeWithText("Create a passcode").assertExists()
            compose.onNodeWithText("Not now").performClick()
            tick(800)
            compose.onNodeWithText("Your week").assertExists()
            compose.onNodeWithText("Release available").assertExists()
            compose.onNodeWithText("Start Easy").assertExists()
            compose.onRoot().captureRoboImage("build/shots/lang_en_1_today.png")

            compose.onNodeWithText("Read").performClick()
            tick(1_000)
            compose.onNodeWithText("Urges come in waves").assertExists()
            compose.onNodeWithText("Urges come in waves").performClick()
            tick(800)
            compose.onNodeWithText("Mind and habits").assertExists()
            scrollTo("Sources")
            compose.onRoot().captureRoboImage("build/shots/lang_en_2_article.png")
        }
    }

    /** Celular em espanhol: a sessão e o resumo em espanhol, com plurais certos. */
    @Test
    @Config(qualifiers = "es-rES-w392dp-h850dp-xhdpi")
    fun spanishSession() {
        readyApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Tu semana").assertExists()
            compose.onRoot().captureRoboImage("build/shots/lang_es_0_hoy.png")
            compose.onNodeWithText("Empezar Fácil").performScrollTo().performClick()
            tick(1_000)
            compose.onNodeWithText("4 rondas por delante", substring = true).assertExists()
            tick(4_000)
            compose.onNodeWithText("Ronda 1 de 4", substring = true).assertExists()
            compose.onNodeWithText("Estoy cerca").performClick()
            tick(1_000)
            compose.onNodeWithText("Inhala").assertExists()
            compose.onRoot().captureRoboImage("build/shots/lang_es_1_pausa.png")
            compose.onNodeWithContentDescription("Terminar sesión").performClick()
            tick(600)
            compose.onNodeWithText("No aguanté").performClick()
            tick(800)
            compose.onNodeWithText("Esta vez no").assertExists()
            compose.onRoot().captureRoboImage("build/shots/lang_es_2_resumen.png")
        }
    }

    /** Ajustes → Idioma: escolher Português remonta a tela no novo idioma e guarda a escolha. */
    @Test
    fun switchingLanguageInSettings() {
        val app = readyApp()
        ActivityScenario.launch(MainActivity::class.java).use {
            tick(500)
            compose.onNodeWithText("Your week").assertExists()
            compose.onNodeWithContentDescription("Settings and profile").performClick()
            tick(800)
            scrollTo("Português")
            compose.onNodeWithText("Português").performClick()
            tick(1_500)
            assertEquals(AppLanguage.PT, Store(app).loadLanguage())
            compose.onNodeWithText("Ajustes").assertExists()
            compose.onRoot().captureRoboImage("build/shots/lang_switch_pt.png")
        }
    }
}
