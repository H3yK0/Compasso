package app.compasso

import android.app.ActivityManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.compasso.ui.AppViewModel
import app.compasso.ui.CompassoApp
import app.compasso.ui.LockRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels { ViewModelProvider.AndroidViewModelFactory(application) }
    private val activityManager: ActivityManager? by lazy { getSystemService(ActivityManager::class.java) }
    private var appliedAttempt = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyDiscreet(vm.settings.value.discreet)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            LaunchedEffect(settings.discreet) { applyDiscreet(settings.discreet) }
            CompassoApp(vm)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.lockRequest.collect { applyLock(it) } }
                launch { vm.focusActive.collect { hideOverlays(it) } }
                // O Android só fixa a tela depois que a pessoa confirma: acompanha o estado.
                launch {
                    while (isActive) {
                        vm.onPinState(isPinned())
                        delay(400)
                    }
                }
            }
        }
        vm.onMultiWindow(isInMultiWindowMode)
    }

    /** Modo discreto: esconde o conteúdo na tela de recentes e bloqueia prints e gravação de tela. */
    private fun applyDiscreet(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /** Fixar a tela (recurso do Android): enquanto fixada, não dá para abrir outro app. */
    private fun applyLock(req: LockRequest) {
        if (req.attempt == appliedAttempt) return
        appliedAttempt = req.attempt
        if (req.wanted) {
            runCatching { startLockTask() }
        } else if (isPinned()) {
            runCatching { stopLockTask() }
        }
    }

    private fun isPinned(): Boolean {
        val am = activityManager ?: return false
        return runCatching { am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE }.getOrDefault(false)
    }

    /** Android 12+: janelas flutuantes de outros apps somem enquanto o Compasso está na frente. */
    private fun hideOverlays(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) runCatching { window.setHideOverlayWindows(on) }
    }

    override fun onStart() {
        super.onStart()
        vm.onAppForeground()
    }

    override fun onResume() {
        super.onResume()
        vm.onReturn()
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations) vm.onLeave()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) vm.onAppBackground()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        vm.onMultiWindow(isInMultiWindowMode)
    }
}
