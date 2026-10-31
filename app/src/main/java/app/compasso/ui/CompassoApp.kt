package app.compasso.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.compasso.data.ThemeMode
import app.compasso.games.GameKind
import app.compasso.model.Outcome
import app.compasso.model.PhaseType
import app.compasso.model.Tip
import app.compasso.ui.components.AlertAction
import app.compasso.ui.components.IosActionSheet
import app.compasso.ui.components.IosAlert
import app.compasso.ui.components.FloatingTabBar
import app.compasso.ui.components.FloatingTabBarSpace
import app.compasso.ui.components.TabItem
import app.compasso.ui.games.BlocksScreen
import app.compasso.ui.games.EchoScreen
import app.compasso.ui.games.MergeScreen
import app.compasso.ui.theme.CompassoTheme
import app.compasso.ui.theme.Ios
import app.compasso.ui.theme.PhaseColors

internal val tabItems = listOf(
    TabItem(Tab.TRAIN.label, Icons.Outlined.Explore, Icons.Rounded.Explore),
    TabItem(Tab.REFUGE.label, Icons.Outlined.Spa, Icons.Rounded.Spa),
    TabItem(Tab.PROGRESS.label, Icons.Outlined.Insights, Icons.Rounded.Insights),
    TabItem(Tab.READ.label, Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Rounded.MenuBook),
)

@Composable
fun CompassoApp(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val locked by vm.locked.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val banner by vm.banner.collectAsStateWithLifecycle()
    val prompt by vm.startPrompt.collectAsStateWithLifecycle()

    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CompassoTheme(dark = dark, accent = effectiveAccent(settings.accent, level)) {
        val colored = !locked && (
            screen == Screen.Session || screen == Screen.Breathe || screen == Screen.FocusPrep || screen == Screen.Welcome
            )
        SystemBarsEffect(darkIcons = !dark && !colored)
        Box(Modifier.fillMaxSize().background(Ios.colors.bg)) {
            if (locked) {
                PinUnlockScreen(onTry = vm::unlock)
            } else {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { iosTransition(initialState, targetState) },
                    label = "telas",
                ) { current -> ScreenContent(current, vm) }

                prompt?.let { p ->
                    IosAlert(
                        title = "Sessão só de controle",
                        message = "A liberação volta ${formatUntil(p.gate.until)}. ${gateReason(p.gate)} " +
                            "Esta sessão vai até o fim sem liberação.",
                        actions = listOf(
                            AlertAction("Começar treino", vm::confirmPrompt, bold = true),
                            AlertAction("Respirar 2 min", vm::breatheFromPrompt),
                            AlertAction("Cancelar", vm::dismissPrompt),
                        ),
                        onDismiss = vm::dismissPrompt,
                    )
                }
                PointsBanner(banner, onDismiss = vm::dismissBanner)
            }
        }
    }
}

@Composable
private fun ScreenContent(current: Screen, vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val custom by vm.customLevel.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val pacing by vm.pacing.collectAsStateWithLifecycle()
    val snapshot by vm.pacingSnapshot.collectAsStateWithLifecycle()
    val codename by vm.codename.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val tipsSeen by vm.tipsSeen.collectAsStateWithLifecycle()
    val articles by vm.articles.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val best by vm.best.collectAsStateWithLifecycle()
    val toRoot: () -> Unit = { vm.go(Screen.Root) }
    fun tipFor(t: Tip): Tip? = if (settings.showTips && t.name !in tipsSeen) t else null

    when (current) {
        Screen.Welcome -> WelcomeScreen(onStart = vm::finishWelcome)

        is Screen.Intro -> {
            val done: () -> Unit = { vm.finishIntro(current.back) }
            if (current.back != null) BackHandler(onBack = done)
            IntroScreen(codename, vm::newCodename, onDone = done, replay = current.back != null)
        }

        Screen.WhatsNew -> WhatsNewScreen(
            onContinue = { vm.closeWhatsNew(false) },
            onTour = { vm.closeWhatsNew(true) },
            includePrevious = vm.previousVersion < 3,
        )

        Screen.Root -> RootTabs(vm, tab, ::tipFor)

        Screen.Session -> {
            val ui = rememberLast(session)
            if (ui != null) {
                SessionRoute(ui, vm, settings.breathGuide)
            } else {
                Box(Modifier.fillMaxSize().background(PhaseColors.countdown[1]))
            }
        }

        Screen.Summary -> {
            BackHandler(onBack = toRoot)
            val s = rememberLast(summary)
            if (s != null) {
                SummaryScreen(s, onHome = toRoot, onRepeat = vm::repeatLast, tip = tipFor(Tip.SUMMARY), onTipClose = vm::dismissTip)
            } else {
                Box(Modifier.fillMaxSize())
            }
        }

        Screen.FocusPrep -> {
            BackHandler(onBack = vm::cancelFocusPrep)
            FocusPrepScreen(onPin = vm::retryPin, onWithoutPin = vm::startWithoutPin, onCancel = vm::cancelFocusPrep)
        }

        Screen.Breathe -> BreatheScreen(
            sound = settings.breatheSound,
            onSound = vm::setBreatheSound,
            onStart = vm::breatheStart,
            onLevel = vm::breatheLevel,
            onBlockDone = vm::onBreatheDone,
            onStop = vm::breatheStop,
            onCue = vm::breathCue,
            onClose = toRoot,
        )

        is Screen.Game -> {
            BackHandler(onBack = toRoot)
            LaunchedEffect(Unit) { vm.prepareGameSounds() }
            val record = best[current.kind] ?: 0
            val onScore: (Int) -> Unit = { vm.onGameScore(current.kind, it) }
            when (current.kind) {
                GameKind.BLOCKS -> BlocksScreen(record, onScore, vm::gameSound, vm::onGamePlayed, toRoot)
                GameKind.MERGE -> MergeScreen(record, onScore, vm::gameSound, vm::onGamePlayed, toRoot)
                GameKind.ECHO -> EchoScreen(record, onScore, vm::gameSound, vm::onGamePlayed, toRoot)
            }
        }

        Screen.PacingInfo -> {
            BackHandler(onBack = toRoot)
            PacingInfoScreen(snapshot = snapshot, pacing = pacing, onDone = toRoot, onSettings = { vm.go(Screen.Settings) })
        }

        Screen.History -> {
            BackHandler(onBack = toRoot)
            HistoryScreen(records = history, onBack = toRoot, backLabel = tab.label)
        }

        Screen.Custom -> {
            BackHandler(onBack = toRoot)
            CustomLevelScreen(initial = custom, onSave = vm::saveCustom, onStart = vm::requestStart, onBack = toRoot)
        }

        Screen.Guide -> {
            val back: () -> Unit = { vm.go(Screen.Settings) }
            BackHandler(onBack = back)
            GuideScreen(onReplayIntro = { vm.go(Screen.Intro(back = Screen.Guide)) }, onBack = back, backLabel = "Ajustes")
        }

        Screen.Profile -> {
            val back: () -> Unit = { vm.go(Screen.Settings) }
            BackHandler(onBack = back)
            ProfileScreen(
                codename = codename,
                level = level,
                accent = effectiveAccent(settings.accent, level),
                onNewCodename = vm::newCodename,
                onAccent = { a -> vm.updateSettings { it.copy(accent = a) } },
                onBack = back,
                backLabel = "Ajustes",
            )
        }

        Screen.About -> {
            val back: () -> Unit = { vm.go(Screen.Settings) }
            BackHandler(onBack = back)
            AboutScreen(onLicense = { vm.go(Screen.License(it)) }, onBack = back)
        }

        Screen.Settings -> {
            BackHandler(onBack = toRoot)
            SettingsRoute(vm, tipFor(Tip.SETTINGS), onBack = toRoot)
        }

        is Screen.License -> {
            BackHandler { vm.go(Screen.About) }
            LicenseScreen(current.doc, onBack = { vm.go(Screen.About) })
        }

        is Screen.Reader -> {
            BackHandler(onBack = toRoot)
            val article = articles.firstOrNull { it.id == current.id }
            if (article != null) {
                ReaderScreen(article, isRead = article.id in read, onRead = { vm.onArticleRead(article.id) }, onBack = toRoot)
            } else {
                Box(Modifier.fillMaxSize().background(Ios.colors.bg))
            }
        }

        is Screen.PinSetup -> {
            val leave: () -> Unit = {
                if (current.fromOptions) {
                    vm.go(Screen.Settings)
                } else {
                    vm.skipPinOffer()
                    vm.finishOnboarding()
                }
            }
            BackHandler(onBack = leave)
            PinSetupScreen(
                firstRun = !current.fromOptions,
                onDone = { pin ->
                    vm.setPin(pin)
                    if (current.fromOptions) vm.go(Screen.Settings) else vm.finishOnboarding()
                },
                onSkip = leave,
            )
        }
    }
}

/** Tela principal: o conteúdo da aba escolhida e a barra de abas flutuante. */
@Composable
private fun RootTabs(vm: AppViewModel, tab: Tab, tipFor: (Tip) -> Tip?) {
    val history by vm.history.collectAsStateWithLifecycle()
    val custom by vm.customLevel.collectAsStateWithLifecycle()
    val snapshot by vm.pacingSnapshot.collectAsStateWithLifecycle()
    val focus by vm.focus.collectAsStateWithLifecycle()
    val points by vm.points.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val codename by vm.codename.collectAsStateWithLifecycle()
    val articles by vm.articles.collectAsStateWithLifecycle()
    val read by vm.read.collectAsStateWithLifecycle()
    val best by vm.best.collectAsStateWithLifecycle()
    val holder = rememberSaveableStateHolder()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BackHandler(enabled = tab != Tab.TRAIN) { vm.selectTab(Tab.TRAIN) }
    Box(Modifier.fillMaxSize()) {
        holder.SaveableStateProvider(tab.name) {
            when (tab) {
                Tab.TRAIN -> TodayScreen(
                    gate = snapshot.gate,
                    customLevel = custom,
                    focus = focus,
                    codename = codename,
                    level = level,
                    points = points,
                    history = history,
                    tip = tipFor(Tip.TRAIN),
                    onTipClose = vm::dismissTip,
                    onStart = vm::requestStart,
                    onEditCustom = { vm.go(Screen.Custom) },
                    onBreathe = { vm.go(Screen.Breathe) },
                    onGateInfo = { vm.go(Screen.PacingInfo) },
                    onSettings = { vm.go(Screen.Settings) },
                    onProgress = { vm.selectTab(Tab.PROGRESS) },
                    bottomPadding = FloatingTabBarSpace + navInset,
                )

                Tab.REFUGE -> RefugeScreen(
                    best = best,
                    tip = tipFor(Tip.REFUGE),
                    onTipClose = vm::dismissTip,
                    onBreathe = { vm.go(Screen.Breathe) },
                    onGame = { vm.go(Screen.Game(it)) },
                    onQuickGame = { vm.go(Screen.Game(GameKind.entries.random())) },
                    onRead = { vm.selectTab(Tab.READ) },
                    bottomPadding = FloatingTabBarSpace + navInset,
                )

                Tab.PROGRESS -> ProgressScreen(
                    level = level,
                    codename = codename,
                    points = points,
                    history = history,
                    tip = tipFor(Tip.PROGRESS),
                    onTipClose = vm::dismissTip,
                    onCheckIn = vm::checkIn,
                    onProfile = { vm.go(Screen.Profile) },
                    onHistory = { vm.go(Screen.History) },
                    bottomPadding = FloatingTabBarSpace,
                )

                Tab.READ -> ReadScreen(
                    articles = articles,
                    read = read,
                    tip = tipFor(Tip.READ),
                    onTipClose = vm::dismissTip,
                    onOpen = { vm.go(Screen.Reader(it)) },
                    bottomPadding = FloatingTabBarSpace,
                )
            }
        }
        FloatingTabBar(tabItems, selected = tab.ordinal, onSelect = { vm.selectTab(Tab.entries[it]) }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Ajustes (abertos pelo emblema na tela Hoje). */
@Composable
private fun SettingsRoute(vm: AppViewModel, tip: Tip?, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val pacing by vm.pacing.collectAsStateWithLifecycle()
    val hasPin by vm.hasPin.collectAsStateWithLifecycle()
    val focus by vm.focus.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val codename by vm.codename.collectAsStateWithLifecycle()
    OptionsScreen(
        settings = settings,
        pacing = pacing,
        focus = focus,
        hasPin = hasPin,
        historyCount = history.size,
        codename = codename,
        level = level,
        tip = tip,
        actions = OptionsActions(
            onChange = vm::updateSettings,
            onPacing = vm::requestPacing,
            onCancelPending = vm::cancelPendingPacing,
            onFocus = vm::requestFocus,
            onTestSound = vm::testSound,
            onSetPin = { vm.go(Screen.PinSetup(fromOptions = true)) },
            onRemovePin = vm::removePin,
            onClearHistory = vm::clearHistory,
            onGuide = { vm.go(Screen.Guide) },
            onReplayIntro = { vm.go(Screen.Intro(back = Screen.Settings)) },
            onResetTips = vm::resetTips,
            onProfile = { vm.go(Screen.Profile) },
            onAbout = { vm.go(Screen.About) },
            onTipClose = vm::dismissTip,
        ),
        onBack = onBack,
    )
}

@Composable
private fun SessionRoute(ui: SessionUi, vm: AppViewModel, breathGuide: Boolean) {
    var askEnd by rememberSaveable { mutableStateOf(false) }
    val final = ui.phase == PhaseType.RELEASE || ui.phase == PhaseType.DENIED
    val finishFinal: () -> Unit = {
        vm.finishSession(if (ui.phase == PhaseType.RELEASE) Outcome.RELEASED else Outcome.DENIED)
    }
    val requestEnd: () -> Unit = {
        if (final) {
            finishFinal()
        } else {
            vm.pause()
            askEnd = true
        }
    }

    BackHandler(onBack = requestEnd)
    KeepScreenOn()

    SessionScreen(
        ui = ui,
        breathGuide = breathGuide,
        onClose = vm::pressClose,
        onTogglePause = vm::togglePause,
        onRequestEnd = requestEnd,
        onFinish = finishFinal,
    )

    if (askEnd) {
        IosActionSheet(
            title = "Encerrar a sessão?",
            message = "Se não deu pra segurar até o fim, marque abaixo. Isso conta no seu progresso.",
            actions = listOf(
                AlertAction("Não segurei", {
                    askEnd = false
                    vm.finishSession(Outcome.LOST_CONTROL)
                }),
                AlertAction("Só encerrar", {
                    askEnd = false
                    vm.finishSession(Outcome.STOPPED)
                }, destructive = true),
            ),
            cancel = AlertAction("Continuar sessão", {
                askEnd = false
                vm.resume()
            }),
            onDismiss = {
                askEnd = false
                vm.resume()
            },
        )
    }
}

/** Aviso rápido de pontos ganhos, que desce do topo e some sozinho. */
@Composable
private fun PointsBanner(banner: Banner?, onDismiss: () -> Unit) {
    val last = rememberLast(banner)
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically(tween(320, easing = IosEase)) { -it * 2 } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(260, easing = IosEase)) { -it * 2 } + fadeOut(tween(200)),
        ) {
            val c = Ios.colors
            val b = last ?: return@AnimatedVisibility
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 380.dp)
                    .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(c.alert)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                    .padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).background(c.orange), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(b.title, style = Ios.type.headline, color = c.label)
                    b.subtitle?.let { Text(it, style = Ios.type.footnote, color = c.secondary) }
                }
            }
        }
    }
}

// ---------- Transições no estilo iOS ----------

private val IosEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val DURATION = 380

private fun AnimatedContentTransitionScope<Screen>.iosTransition(from: Screen, to: Screen): ContentTransform {
    val slide = tween<IntOffset>(DURATION, easing = IosEase)
    val fade = tween<Float>(DURATION, easing = IosEase)
    return when {
        // fim da apresentação: o app aparece suave
        from.onboarding && !to.onboarding && to == Screen.Root ->
            (fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 0.97f)) togetherWith fadeOut(tween(300))
        // tela que sobe por cima (sessão, respirar, jogos, detalhes)
        to.modal && !from.modal ->
            slideInVertically(slide) { it } togetherWith fadeOut(fade, targetAlpha = 1f)
        // tela que desce e revela a de baixo
        from.modal && !to.modal ->
            (fadeIn(fade, initialAlpha = 1f) togetherWith slideOutVertically(slide) { it }).apply { targetContentZIndex = -1f }
        // entre telas que sobem (preparo → sessão → resumo)
        from.modal && to.modal ->
            (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.98f)) togetherWith fadeOut(tween(320))
        // empurra para a direita
        to.depth > from.depth ->
            slideInHorizontally(slide) { it } togetherWith slideOutHorizontally(slide) { -it / 4 }
        // volta
        to.depth < from.depth ->
            (slideInHorizontally(slide) { -it / 4 } togetherWith slideOutHorizontally(slide) { it }).apply { targetContentZIndex = -1f }
        else -> fadeIn(tween(250)) togetherWith fadeOut(tween(250))
    }
}

/** Guarda o último valor não nulo (para a tela que está saindo não piscar vazia). */
private class Last<T> {
    var value: T? = null
}

@Composable
private fun <T : Any> rememberLast(value: T?): T? {
    val holder = remember { Last<T>() }
    if (value != null) holder.value = value
    return value ?: holder.value
}

@Composable
private fun SystemBarsEffect(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
