package app.compasso.ui

import android.app.Application
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.compasso.R
import app.compasso.audio.AmbientPlayer
import app.compasso.audio.Feedback
import app.compasso.audio.GameSound
import app.compasso.audio.GameSounds
import app.compasso.audio.Sound
import app.compasso.audio.Track
import app.compasso.data.ActiveSession
import app.compasso.data.AppClock
import app.compasso.data.ArticleRepo
import app.compasso.data.Settings
import app.compasso.data.Store
import app.compasso.games.GameKind
import app.compasso.model.Article
import app.compasso.model.ArticleInfo
import app.compasso.model.Coach
import app.compasso.model.LevelAdvice
import app.compasso.model.LevelReason
import app.compasso.model.Levels
import app.compasso.model.ReadingAdvice
import app.compasso.model.Signal
import app.compasso.model.Breath
import app.compasso.model.Codename
import app.compasso.model.Codenames
import app.compasso.model.EngineEvent
import app.compasso.model.Focus
import app.compasso.model.FocusSettings
import app.compasso.model.Lapse
import app.compasso.model.Level
import app.compasso.model.NoPoints
import app.compasso.model.LevelStatus
import app.compasso.model.Outcome
import app.compasso.model.Pacing
import app.compasso.model.PacingConfig
import app.compasso.model.PacingEntry
import app.compasso.model.PacingSettings
import app.compasso.model.PacingSnapshot
import app.compasso.model.PhaseType
import app.compasso.model.PinPolicy
import app.compasso.model.PointEntry
import app.compasso.model.PointKind
import app.compasso.model.Progress
import app.compasso.model.ReleaseGate
import app.compasso.model.ReleaseMode
import app.compasso.model.SessionEngine
import app.compasso.model.SessionRecord
import app.compasso.model.Tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Abas da tela principal. */
enum class Tab(@StringRes val label: Int) {
    TRAIN(R.string.tab_today),
    REFUGE(R.string.tab_refuge),
    PROGRESS(R.string.tab_progress),
    READ(R.string.tab_read),
}

sealed interface Screen {
    /** Profundidade na navegação (define se a troca de tela empurra ou volta). */
    val depth: Int

    /** Telas que sobem por cima de tudo (sessão, respirar, jogos, detalhes). */
    val modal: Boolean get() = false

    /** Telas da apresentação (a saída delas para o app é um esmaecer, não um "voltar"). */
    val onboarding: Boolean get() = false

    data object Welcome : Screen {
        override val depth = 0
        override val onboarding = true
    }

    /** Apresentação guiada. [back] nulo = primeira abertura; senão, para onde voltar no fim. */
    data class Intro(val back: Screen?) : Screen {
        override val depth: Int get() = if (back == null) 1 else 2
        override val onboarding: Boolean get() = back == null
    }

    /** Tela principal com as abas. */
    data object Root : Screen {
        override val depth = 0
    }

    data object History : Screen {
        override val depth = 1
    }

    data object Custom : Screen {
        override val depth = 1
    }

    data object Guide : Screen {
        override val depth = 2
    }

    data object Profile : Screen {
        override val depth = 2
    }

    data object About : Screen {
        override val depth = 2
    }

    /** Ajustes: abertos pelo emblema no alto da tela Hoje. */
    data object Settings : Screen {
        override val depth = 1
    }

    data class License(val doc: LicenseDoc) : Screen {
        override val depth = 3
    }

    data class Reader(val id: String) : Screen {
        override val depth = 1
    }

    data object PacingInfo : Screen {
        override val depth = 1
        override val modal = true
    }

    data object FocusPrep : Screen {
        override val depth = 1
        override val modal = true
    }

    data object Session : Screen {
        override val depth = 1
        override val modal = true
    }

    data object Summary : Screen {
        override val depth = 1
        override val modal = true
    }

    data object Breathe : Screen {
        override val depth = 1
        override val modal = true
    }

    data class Game(val kind: GameKind) : Screen {
        override val depth = 1
        override val modal = true
    }

    data class PinSetup(val fromOptions: Boolean) : Screen {
        override val depth: Int get() = 2
        override val onboarding: Boolean get() = !fromOptions
    }

    /** Pede a senha atual antes de trocar ou remover (quem pegou o celular destravado não consegue). */
    data class PinVerify(val action: PinAction) : Screen {
        override val depth: Int get() = 2
    }
}

enum class PinAction { CHANGE, REMOVE }

/** Resultado de conferir a senha. */
enum class PinCheck { OK, WRONG, WAIT }

/** Foto do estado da sessão para a tela desenhar. */
data class SessionUi(
    val level: Level,
    val phase: PhaseType,
    val round: Int,
    val totalRounds: Int,
    val phaseRemainingMs: Long,
    val phaseDurationMs: Long,
    val phaseElapsedMs: Long,
    val bpm: Int,
    val nextBpm: Int?,
    val beatCount: Long,
    val closeCount: Int,
    val elapsedMs: Long,
    val paused: Boolean,
    val controlOnly: Boolean = false,
    /** Aviso mostrado na tela de pausa (saiu do app, tela solta, tela dividida…). */
    val notice: SessionNotice? = null,
    /** Sessão em modo foco com a tela fixada (vale pontos). */
    val focused: Boolean = false,
    /** Tela dividida aberta: só dá para continuar em tela cheia. */
    val blocked: Boolean = false,
)

/** Aviso na tela de pausa da sessão. */
sealed interface SessionNotice {
    /** Saiu do app com o modo foco ligado; [exitsLeft] saídas antes de encerrar. */
    data class LeftApp(val exitsLeft: Int) : SessionNotice

    data object SplitScreen : SessionNotice

    data object Unpinned : SessionNotice
}

/** Por que a sessão não rendeu pontos. */
enum class NoPointsReason { INTERRUPTED, FOCUS_OFF, NOT_PINNED, DAILY_CAP, KIND_LIMIT, SESSION_LIMIT }

fun NoPoints.asReason(): NoPointsReason = when (this) {
    NoPoints.DAILY_CAP -> NoPointsReason.DAILY_CAP
    NoPoints.KIND_LIMIT -> NoPointsReason.KIND_LIMIT
    NoPoints.SESSION_LIMIT -> NoPointsReason.SESSION_LIMIT
}

data class SummaryUi(
    val record: SessionRecord,
    val saved: Boolean,
    val gate: ReleaseGate,
    val points: PointEntry? = null,
    val noPoints: NoPointsReason? = null,
    /** Leitura sugerida pelo que aconteceu nesta sessão e nas anteriores. */
    val reading: ReadingAdvice? = null,
)

/** Pedido de começar um nível enquanto a liberação está fechada (o app pergunta antes). */
data class StartPrompt(val level: Level, val gate: ReleaseGate.Closed)

/** O que o app pede para a Activity: fixar a tela ou soltar. [attempt] muda a cada novo pedido. */
data class LockRequest(val wanted: Boolean = false, val attempt: Int = 0)

/** Aviso rápido no topo da tela: pontos ganhos e com o quê. */
data class Banner(val points: Int, val kind: PointKind, val id: Long = System.nanoTime())

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val clock = AppClock(store)

    /** Horário confiável (não volta para trás se a hora do celular for atrasada). */
    fun now(): Long = clock.now()
    private val feedback = Feedback(app)
    private val ambient = AmbientPlayer(app)
    private var gameSounds: GameSounds? = null

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _history = MutableStateFlow(store.loadHistory())
    val history: StateFlow<List<SessionRecord>> = _history.asStateFlow()

    private val _custom = MutableStateFlow(store.loadCustomLevel())
    val customLevel: StateFlow<Level> = _custom.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(Screen.Root)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _tab = MutableStateFlow(Tab.TRAIN)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val _session = MutableStateFlow<SessionUi?>(null)
    val session: StateFlow<SessionUi?> = _session.asStateFlow()

    private val _summary = MutableStateFlow<SummaryUi?>(null)
    val summary: StateFlow<SummaryUi?> = _summary.asStateFlow()

    private val _hasPin = MutableStateFlow(store.hasPin())
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _locked = MutableStateFlow(store.hasPin())
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _pacing = MutableStateFlow(store.loadPacing())
    val pacing: StateFlow<PacingSettings> = _pacing.asStateFlow()

    private val _snapshot = MutableStateFlow(PacingSnapshot(ReleaseGate.Off, null, 0))
    val pacingSnapshot: StateFlow<PacingSnapshot> = _snapshot.asStateFlow()

    private val _prompt = MutableStateFlow<StartPrompt?>(null)
    val startPrompt: StateFlow<StartPrompt?> = _prompt.asStateFlow()

    private val _points = MutableStateFlow(store.loadPoints())
    val points: StateFlow<List<PointEntry>> = _points.asStateFlow()

    private val _lapses = MutableStateFlow(store.loadLapses())
    val lapses: StateFlow<List<Lapse>> = _lapses.asStateFlow()

    private val _level = MutableStateFlow(Progress.level(emptyList(), emptyList(), clock.now()))
    val level: StateFlow<LevelStatus> = _level.asStateFlow()

    private val _codename = MutableStateFlow(store.loadCodename() ?: Codenames.generate().also { store.saveCodename(it) })
    val codename: StateFlow<Codename> = _codename.asStateFlow()

    private val _focus = MutableStateFlow(store.loadFocus())
    val focus: StateFlow<FocusSettings> = _focus.asStateFlow()

    private val _lockRequest = MutableStateFlow(LockRequest())
    val lockRequest: StateFlow<LockRequest> = _lockRequest.asStateFlow()

    /** Sessão em modo foco acontecendo (a Activity esconde janelas flutuantes). */
    private val _focusActive = MutableStateFlow(false)
    val focusActive: StateFlow<Boolean> = _focusActive.asStateFlow()

    private val _pinned = MutableStateFlow(false)
    val pinned: StateFlow<Boolean> = _pinned.asStateFlow()

    private val _tipsSeen = MutableStateFlow(store.loadTipsSeen())
    val tipsSeen: StateFlow<Set<String>> = _tipsSeen.asStateFlow()

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()
    private var articlesLanguage: String? = null

    /** Recomendações a partir do histórico (ver [Coach]). */
    private val _levelAdvice = MutableStateFlow(LevelAdvice(Levels.EASY.id, LevelReason.FIRST))
    val levelAdvice: StateFlow<LevelAdvice> = _levelAdvice.asStateFlow()

    private val _readingAdvice = MutableStateFlow<ReadingAdvice?>(null)
    val readingAdvice: StateFlow<ReadingAdvice?> = _readingAdvice.asStateFlow()

    /** Sugestão de leitura que aparece ao abrir o app, de tempos em tempos. */
    private val _nudge = MutableStateFlow<ReadingAdvice?>(null)
    val nudge: StateFlow<ReadingAdvice?> = _nudge.asStateFlow()

    /** Leitor aberto (a música de fundo da leitura toca só nele). */
    private var readingActive = false

    private val _read = MutableStateFlow(store.loadRead())
    val read: StateFlow<Set<String>> = _read.asStateFlow()

    private val _best = MutableStateFlow(store.loadBest())
    val best: StateFlow<Map<GameKind, Int>> = _best.asStateFlow()

    private val _banner = MutableStateFlow<Banner?>(null)
    val banner: StateFlow<Banner?> = _banner.asStateFlow()

    private var pacingLog: List<PacingEntry> = emptyList()
    private var engine: SessionEngine? = null
    private var ticker: Job? = null
    private var paused = false
    private var sessionStartedAt = 0L
    private var lastLevel: Level? = null
    private var backgroundAt = 0L
    private var lastBreathStep = -1L
    private var bannerJob: Job? = null

    // modo foco da sessão atual
    private var pendingStart: Pair<Level, Boolean>? = null
    private var sessionFocusMode = false
    private var sessionPinned = false
    private var notice: SessionNotice? = null
    private var leftAt = 0L
    private var leftWall = 0L
    private var exits = 0
    private var awayJob: Job? = null
    private var inMultiWindow = false
    private var lastBeat = 0L
    private var breatheActive = false

    init {
        val now = clock.now()
        pacingLog = store.loadPacingLog()
        recoverOrphan(now)
        refreshPacing()
        refreshProgress()
        _screen.value = if (store.seenVersion >= CURRENT_VERSION) Screen.Root else Screen.Welcome
        // Atualiza prazos de tempos em tempos (podem vencer com o app aberto).
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                refreshPacing()
                refreshProgress()
            }
        }
    }

    // ---------- Idioma ----------

    /** A Activity informa o idioma em uso ("pt", "en", "es"): os artigos vêm nele. */
    fun setLanguage(language: String) {
        if (language == articlesLanguage) return
        articlesLanguage = language
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { ArticleRepo.load(getApplication(), language) }
            if (articlesLanguage == language) {
                _articles.value = list
                refreshAdvice()
                maybeNudge()
            }
        }
    }

    // ---------- Navegação ----------

    fun go(screen: Screen) {
        if (screen == Screen.Root) {
            refreshPacing()
            refreshProgress()
        }
        _screen.value = screen
    }

    fun goTab(tab: Tab) {
        _tab.value = tab
        go(Screen.Root)
    }

    fun selectTab(tab: Tab) {
        _tab.value = tab
        if (tab == Tab.PROGRESS || tab == Tab.TRAIN) {
            refreshPacing()
            refreshProgress()
        }
    }

    // ---------- Apresentação ----------

    fun finishWelcome() = go(Screen.Intro(back = null))

    fun finishIntro(back: Screen?) {
        if (back != null) {
            go(back)
            return
        }
        if (!store.hasPin() && !store.pinOffered) go(Screen.PinSetup(fromOptions = false)) else finishOnboarding()
    }

    // ---------- Recomendações ----------

    private fun articleInfos(): List<ArticleInfo> = _articles.value.map { ArticleInfo(it.id, it.order, it.tags) }

    private fun currentSignals(now: Long = clock.now()): List<Signal> =
        Coach.signals(_history.value, _lapses.value, _points.value, now)

    /** Recalcula o nível e a leitura sugeridos (depois de sessões, pontos ou leituras). */
    fun refreshAdvice() {
        val now = clock.now()
        _levelAdvice.value = Coach.level(_history.value, _lapses.value, now)
        _readingAdvice.value = Coach.reading(currentSignals(now), articleInfos(), _read.value)
    }

    /**
     * Ao abrir o app (ou voltar a ele), sugere uma leitura de tempos em tempos: só na tela principal,
     * destravado, fora de sessão, com a opção ligada e pelo menos [NUDGE_EVERY_MS] desde a última.
     */
    fun maybeNudge() {
        if (!_settings.value.readingNudges || _nudge.value != null) return
        if (_locked.value || engine != null || _screen.value != Screen.Root) return
        if (store.seenVersion < CURRENT_VERSION || _articles.value.isEmpty()) return
        val now = clock.now()
        if (store.nudgeAt > 0L && now - store.nudgeAt < NUDGE_EVERY_MS) return
        val advice = Coach.reading(currentSignals(now), articleInfos(), _read.value, skip = store.nudged.toSet()) ?: return
        store.nudgeAt = now
        store.nudged = store.nudged + advice.articleId
        _nudge.value = advice
    }

    fun openNudge() {
        val n = _nudge.value ?: return
        _nudge.value = null
        go(Screen.Reader(n.articleId))
    }

    fun dismissNudge() {
        _nudge.value = null
    }

    fun finishOnboarding() {
        store.seenVersion = CURRENT_VERSION
        _tab.value = Tab.TRAIN
        go(Screen.Root)
    }

    // ---------- Dicas ("ir descobrindo") ----------

    fun dismissTip(tip: Tip) {
        val next = _tipsSeen.value + tip.name
        _tipsSeen.value = next
        store.saveTipsSeen(next)
    }

    fun resetTips() {
        _tipsSeen.value = emptySet()
        store.saveTipsSeen(emptySet())
        if (!_settings.value.showTips) updateSettings { it.copy(showTips = true) }
    }

    // ---------- Ajustes ----------

    fun updateSettings(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        _settings.value = s
        store.saveSettings(s)
        ambient.gain = s.ambientVolume
    }

    fun saveCustom(level: Level) {
        val l = level.sanitized()
        _custom.value = l
        store.saveCustomLevel(l)
    }

    fun testSound() {
        val s = _settings.value
        feedback.play(Sound.STOP, s.volume)
        if (s.vibratePhase) feedback.vibratePhase(PhaseType.STOP)
    }

    /**
     * Apaga o histórico visível. O controle inteligente continua lembrando os últimos 8 dias e
     * os lapsos ("não aguentei" e interrompidas) continuam valendo para a sequência e o Invicto.
     */
    fun clearHistory() {
        store.clearHistory()
        _history.value = emptyList()
        refreshProgress()
    }

    // ---------- Controle inteligente ----------

    fun refreshPacing() {
        val now = clock.now()
        val applied = Pacing.applyDue(_pacing.value, now)
        if (applied != _pacing.value) {
            _pacing.value = applied
            store.savePacing(applied)
        }
        _snapshot.value = Pacing.snapshot(applied.config, pacingLog, now)
        val focus = Focus.applyDue(_focus.value, now)
        if (focus != _focus.value) {
            _focus.value = focus
            store.saveFocus(focus)
        }
    }

    fun requestPacing(target: PacingConfig) {
        val now = clock.now()
        val next = Pacing.request(Pacing.applyDue(_pacing.value, now), target, now)
        _pacing.value = next
        store.savePacing(next)
        refreshPacing()
    }

    fun cancelPendingPacing() = requestPacing(_pacing.value.config)

    // ---------- Modo foco (ajuste) ----------

    fun requestFocus(enable: Boolean) {
        val now = clock.now()
        val next = Focus.request(Focus.applyDue(_focus.value, now), enable, now)
        _focus.value = next
        store.saveFocus(next)
    }

    // ---------- Pontos, nível e codinome ----------

    fun refreshProgress() {
        _level.value = Progress.level(_points.value, _lapses.value, clock.now())
        refreshAdvice()
    }

    /** Tenta ganhar pontos; devolve a entrada nova ou o motivo de não ter rendido. */
    private fun award(kind: PointKind, banner: Boolean): Pair<PointEntry?, NoPoints?> {
        val now = clock.now()
        val result = Progress.award(_points.value, kind, now)
        val entry = result.entry
        if (entry != null) {
            val list = Progress.prune(_points.value + entry, now)
            _points.value = list
            store.savePoints(list)
            refreshProgress()
            if (banner) showBanner(Banner(entry.points, kind))
        }
        return entry to result.reason
    }

    fun checkIn() {
        award(PointKind.CHECKIN, banner = true)
    }

    fun newCodename() {
        val c = Codenames.generate()
        store.saveCodename(c)
        _codename.value = c
    }

    fun showBanner(b: Banner) {
        _banner.value = b
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            delay(2_600)
            _banner.value = null
        }
    }

    fun dismissBanner() {
        bannerJob?.cancel()
        _banner.value = null
    }

    // ---------- Respirar ----------

    fun breatheStart() {
        breatheActive = true
        val s = _settings.value
        ambient.gain = s.ambientVolume
        ambient.track = s.breatheTrack
        ambient.breathLevel = 0f
        ambient.allowAgain()
        if (s.breatheSound) ambient.start()
    }

    /** Trilha do Respirar (também usada nas pausas da sessão). Troca na hora se estiver tocando. */
    fun setBreatheTrack(track: Track) {
        updateSettings { it.copy(breatheTrack = track) }
        if (breatheActive) ambient.track = track
    }

    fun breatheLevel(level: Float) {
        ambient.breathLevel = level
    }

    fun breatheStop() {
        breatheActive = false
        ambient.stop()
    }

    fun setBreatheSound(on: Boolean) {
        updateSettings { it.copy(breatheSound = on) }
        if (on && breatheActive) {
            ambient.allowAgain()
            ambient.start()
        } else if (!on) {
            ambient.stop()
        }
    }

    /** Um bloco de 2 minutos de respiração chegou ao fim. */
    fun onBreatheDone() {
        award(PointKind.BREATHE, banner = true)
    }

    /** Toque leve na troca da respiração (tela Respirar). */
    fun breathCue(inhale: Boolean) {
        if (_settings.value.vibrateBreath) feedback.vibrateBreath(inhale)
    }

    // ---------- Jogos ----------

    /** Prepara os sons dos jogos (gerados na primeira vez) antes da primeira jogada. */
    fun prepareGameSounds() {
        if (gameSounds == null) gameSounds = GameSounds(getApplication())
    }

    fun gameSound(sound: GameSound) {
        val s = _settings.value
        if (s.volume <= 0f) return
        val gs = gameSounds ?: GameSounds(getApplication()).also { gameSounds = it }
        gs.play(sound, (s.volume * 0.85f).coerceIn(0f, 1f))
    }

    /** Pelo menos 3 minutos jogando nesta visita. */
    fun onGamePlayed() {
        award(PointKind.GAME, banner = true)
    }

    fun onGameScore(kind: GameKind, score: Int) {
        val current = _best.value[kind] ?: 0
        if (score > current) {
            _best.value = _best.value + (kind to score)
            store.saveBest(kind, score)
        }
    }

    // ---------- Leituras ----------

    fun article(id: String): Article? = _articles.value.firstOrNull { it.id == id }

    fun onArticleRead(id: String) {
        if (id in _read.value) return
        val next = _read.value + id
        _read.value = next
        store.saveRead(next)
        award(PointKind.ARTICLE, banner = true)
        refreshAdvice()
    }

    /** O leitor abriu ou fechou: com a música de leitura ligada, ela toca enquanto ele estiver aberto. */
    fun readerOpen(open: Boolean) {
        readingActive = open
        if (open) startReadingMusic() else if (!breatheActive) ambient.stop()
    }

    private fun startReadingMusic() {
        val s = _settings.value
        if (!readingActive || !s.readingMusic) return
        ambient.gain = s.ambientVolume
        ambient.track = s.readingTrack
        // sem respiração guiada: um nível fixo e calmo
        ambient.breathLevel = 0.35f
        ambient.allowAgain()
        ambient.start()
    }

    fun setReadingMusic(on: Boolean) {
        updateSettings { it.copy(readingMusic = on) }
        if (on) startReadingMusic() else if (readingActive) ambient.stop()
    }

    fun setReadingTrack(track: Track) {
        updateSettings { it.copy(readingTrack = track) }
        if (readingActive && _settings.value.readingMusic) {
            ambient.track = track
            startReadingMusic()
        }
    }

    // ---------- Senha ----------

    /** Quanto falta para poder tentar a senha de novo (0 = pode tentar). */
    fun pinWaitMs(): Long = (store.pinWaitUntil - clock.now()).coerceAtLeast(0L)

    /**
     * Confere a senha. Erros ficam gravados e, da 5ª tentativa errada em diante, cada erro pede
     * uma espera (fechar e abrir o app não zera, e atrasar a hora do celular não encurta).
     */
    private fun checkPin(pin: String): PinCheck {
        if (pinWaitMs() > 0L) return PinCheck.WAIT
        if (store.checkPin(pin)) {
            store.pinFails = 0
            store.pinWaitUntil = 0L
            return PinCheck.OK
        }
        val fails = store.pinFails + 1
        store.pinFails = fails
        val wait = PinPolicy.waitAfter(fails)
        if (wait > 0L) store.pinWaitUntil = clock.now() + wait
        return PinCheck.WRONG
    }

    fun unlock(pin: String): PinCheck {
        val r = checkPin(pin)
        if (r == PinCheck.OK) {
            _locked.value = false
            maybeNudge()
        }
        return r
    }

    /** Senha atual conferida: segue para trocar ou remover. */
    fun verifyPin(pin: String, action: PinAction): PinCheck {
        val r = checkPin(pin)
        if (r == PinCheck.OK) {
            when (action) {
                PinAction.CHANGE -> go(Screen.PinSetup(fromOptions = true))
                PinAction.REMOVE -> {
                    removePin()
                    go(Screen.Settings)
                }
            }
        }
        return r
    }

    /** Criar senha nova ou trocar a atual (trocar pede a atual antes). */
    fun requestPinSetup() {
        go(if (store.hasPin()) Screen.PinVerify(PinAction.CHANGE) else Screen.PinSetup(fromOptions = true))
    }

    fun requestPinRemoval() {
        if (store.hasPin()) go(Screen.PinVerify(PinAction.REMOVE))
    }

    fun setPin(pin: String) {
        store.setPin(pin)
        store.pinFails = 0
        store.pinWaitUntil = 0L
        _hasPin.value = true
    }

    private fun removePin() {
        store.removePin()
        _hasPin.value = false
        _locked.value = false
    }

    fun skipPinOffer() {
        store.pinOffered = true
    }

    // ---------- Ciclo de vida ----------

    fun onAppBackground() {
        backgroundAt = SystemClock.elapsedRealtime()
        pause()
        if (breatheActive || readingActive) ambient.stop()
    }

    fun onAppForeground() {
        if (backgroundAt > 0L && store.hasPin() && SystemClock.elapsedRealtime() - backgroundAt > LOCK_AFTER_MS) {
            _locked.value = true
        }
        backgroundAt = 0L
        refreshPacing()
        refreshProgress()
        if (breatheActive && _settings.value.breatheSound) {
            ambient.allowAgain()
            ambient.start()
        }
        startReadingMusic()
        maybeNudge()
    }

    /** O app deixou de ser a tela da frente (outro app, notificação aberta, tela apagada…). */
    fun onLeave() {
        val e = engine ?: return
        if (leftAt != 0L) return
        leftAt = SystemClock.elapsedRealtime()
        leftWall = clock.now()
        if (e.isFinal) {
            // fim da sessão já alcançado: sair por um tempo apenas conclui
            awayJob?.cancel()
            awayJob = viewModelScope.launch {
                delay(Focus.GRACE_MS)
                if (engine === e) finishSession(finalOutcome(e))
            }
            return
        }
        pause()
        saveHeartbeat()
        if (!sessionFocusMode) return
        exits++
        if (exits >= Focus.MAX_EXITS) {
            finishSession(Outcome.INTERRUPTED, endedAt = leftWall)
            return
        }
        awayJob?.cancel()
        awayJob = viewModelScope.launch {
            delay(Focus.GRACE_MS)
            if (engine === e && leftAt != 0L) finishSession(Outcome.INTERRUPTED, endedAt = leftWall)
        }
    }

    /** O app voltou a ser a tela da frente. */
    fun onReturn() {
        if (leftAt == 0L) return
        leftAt = 0L
        awayJob?.cancel()
        awayJob = null
        val e = engine ?: return
        if (e.isFinal || !sessionFocusMode) return
        notice = SessionNotice.LeftApp(Focus.MAX_EXITS - exits)
        publish()
    }

    /** Tela dividida (ou janela) aberta durante a sessão. */
    fun onMultiWindow(active: Boolean) {
        inMultiWindow = active
        if (engine == null) return
        if (active) {
            onLeave()
            if (engine != null) {
                notice = SessionNotice.SplitScreen
                publish()
            }
        } else {
            onReturn()
        }
    }

    // ---------- Começar ----------

    /** Começa o nível; se a liberação estiver fechada, pergunta antes (vira só treino de controle). */
    fun requestStart(level: Level) {
        refreshPacing()
        val gate = _snapshot.value.gate
        if (gate is ReleaseGate.Closed && level.release != ReleaseMode.NEVER) {
            _prompt.value = StartPrompt(level, gate)
        } else {
            beginStart(level, releaseAllowed = true)
        }
    }

    fun confirmPrompt() {
        val p = _prompt.value ?: return
        _prompt.value = null
        beginStart(p.level, releaseAllowed = false)
    }

    fun dismissPrompt() {
        _prompt.value = null
    }

    fun breatheFromPrompt() {
        _prompt.value = null
        _screen.value = Screen.Breathe
    }

    fun repeatLast() {
        lastLevel?.let { requestStart(it) }
    }

    /** Com o modo foco, antes de começar o app pede para fixar a tela. */
    private fun beginStart(level: Level, releaseAllowed: Boolean) {
        refreshPacing()
        if (_focus.value.enabled) {
            pendingStart = level to releaseAllowed
            _focusActive.value = true
            _lockRequest.value = LockRequest(true, _lockRequest.value.attempt + 1)
            _screen.value = Screen.FocusPrep
            if (_pinned.value) startPending(pinned = true)
        } else {
            startSession(level, releaseAllowed, focusMode = false, pinned = false)
        }
    }

    /** Pede de novo para fixar a tela (se a pessoa tocou em "Agora não" no aviso do Android). */
    fun retryPin() {
        _lockRequest.value = LockRequest(true, _lockRequest.value.attempt + 1)
    }

    /** Começa sem fixar a tela: a sessão vale como treino, mas não rende pontos. */
    fun startWithoutPin() = startPending(pinned = false)

    fun cancelFocusPrep() {
        pendingStart = null
        _focusActive.value = false
        _lockRequest.value = LockRequest(false, _lockRequest.value.attempt + 1)
        go(Screen.Root)
    }

    private fun startPending(pinned: Boolean) {
        val (level, release) = pendingStart ?: return
        pendingStart = null
        startSession(level, release, focusMode = true, pinned = pinned)
    }

    /** A Activity informa se a tela está fixada (o Android só fixa depois que a pessoa confirma). */
    fun onPinState(pinnedNow: Boolean) {
        if (_pinned.value == pinnedNow) return
        _pinned.value = pinnedNow
        if (pinnedNow) {
            if (_screen.value == Screen.FocusPrep && pendingStart != null) startPending(pinned = true)
        } else {
            val e = engine ?: return
            if (sessionPinned && !e.isFinal) {
                sessionPinned = false
                pause()
                notice = SessionNotice.Unpinned
                publish()
            }
        }
    }

    // ---------- Sessão ----------

    private fun startSession(level: Level, releaseAllowed: Boolean, focusMode: Boolean, pinned: Boolean) {
        ticker?.cancel()
        val s = _settings.value
        val e = SessionEngine(level, countdownSeconds = if (s.countdown) 3 else 0, releaseAllowed = releaseAllowed)
        engine = e
        lastLevel = level
        paused = false
        lastBreathStep = -1L
        ambient.allowAgain()
        sessionStartedAt = clock.now()
        sessionFocusMode = focusMode
        sessionPinned = pinned
        _focusActive.value = focusMode
        notice = null
        exits = 0
        leftAt = 0L
        awayJob?.cancel()
        lastBeat = 0L
        handle(e.start())
        publish()
        saveHeartbeat()
        _screen.value = Screen.Session
        if (inMultiWindow) onMultiWindow(true)
        ticker = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                // ~60 vezes por segundo durante a sessão; bem menos quando pausada ou no final
                delay(if (paused || engine?.isFinal == true) 120L else 16L)
                val now = SystemClock.elapsedRealtime()
                val dt = (now - last).coerceIn(0L, 250L)
                last = now
                val eng = engine ?: break
                if (!paused && !eng.isFinal) {
                    val events = eng.advance(dt)
                    if (events.isNotEmpty()) handle(events)
                    breathTick(eng)
                    publish()
                    if (now - lastBeat > HEARTBEAT_MS) saveHeartbeat()
                }
                ambientTick(eng)
            }
        }
    }

    fun pressClose() {
        val e = engine ?: return
        if (paused) return
        handle(e.pressClose())
        publish()
    }

    fun pause() {
        if (engine?.isFinal == false && !paused) {
            paused = true
            if (!breatheActive) ambient.stop()
            publish()
        }
    }

    fun resume() {
        if (engine == null || inMultiWindow) return
        paused = false
        notice = null
        publish()
    }

    fun togglePause() {
        if (paused) resume() else pause()
    }

    private fun finalOutcome(e: SessionEngine) = if (e.phase == PhaseType.RELEASE) Outcome.RELEASED else Outcome.DENIED

    fun finishSession(outcome: Outcome, endedAt: Long = clock.now()) {
        val e = engine ?: return
        ticker?.cancel()
        ticker = null
        awayJob?.cancel()
        awayJob = null
        ambient.stop()
        val now = clock.now()
        val record = SessionRecord(
            startedAt = sessionStartedAt,
            endedAt = endedAt.coerceAtLeast(sessionStartedAt),
            levelId = e.level.id,
            levelName = e.level.name,
            durationMs = e.activeElapsedMs,
            roundsDone = e.roundsCompleted,
            roundsTotal = e.level.rounds,
            closeCount = e.closeCount,
            maxBpm = e.maxBpm,
            outcome = outcome,
            controlOnly = e.controlOnly,
            focused = sessionFocusMode && sessionPinned && outcome != Outcome.INTERRUPTED,
        )
        // Encerrar logo no começo (toque sem querer) não conta.
        val accidental = outcome == Outcome.STOPPED && e.activeElapsedMs < 20_000L
        val save = _settings.value.keepHistory && !accidental
        // Sessão interrompida fica registrada mesmo com o histórico desligado (vale para a sequência).
        if (save || outcome == Outcome.INTERRUPTED) {
            val list = (listOf(record) + _history.value).take(MAX_HISTORY)
            _history.value = list
            store.saveHistory(list)
        }
        // O controle inteligente guarda só a data e o tipo da sessão, dos últimos 8 dias.
        if (!accidental) {
            pacingLog = Pacing.prune(pacingLog + PacingEntry.from(record), now)
            store.savePacingLog(pacingLog)
        }
        addLapse(record, now)
        store.saveActive(null)

        val kind = Progress.kindFor(record)
        var entry: PointEntry? = null
        val why: NoPointsReason? = when {
            outcome == Outcome.INTERRUPTED -> NoPointsReason.INTERRUPTED
            outcome == Outcome.STOPPED -> null
            !sessionFocusMode -> NoPointsReason.FOCUS_OFF
            !sessionPinned -> NoPointsReason.NOT_PINNED
            kind == null -> null
            else -> award(kind, banner = false).let { (en, reason) ->
                entry = en
                reason?.asReason()
            }
        }
        refreshPacing()
        refreshProgress()
        val reading = Coach.reading(currentSignals(now), articleInfos(), _read.value)
        _summary.value = SummaryUi(record, saved = save, gate = _snapshot.value.gate, points = entry, noPoints = why, reading = reading)
        engine = null
        paused = false
        notice = null
        sessionFocusMode = false
        sessionPinned = false
        _focusActive.value = false
        _lockRequest.value = LockRequest(false, _lockRequest.value.attempt + 1)
        _session.value = null
        _screen.value = Screen.Summary
    }

    /** Sessão que ficou aberta quando o app foi fechado: entra no histórico como interrompida. */
    private fun recoverOrphan(now: Long) {
        val a = store.loadActive() ?: return
        store.saveActive(null)
        val outcome = if (a.focusMode) Outcome.INTERRUPTED else Outcome.STOPPED
        if (outcome == Outcome.STOPPED && a.elapsedMs < 20_000L) return
        val record = SessionRecord(
            startedAt = a.startedAt,
            endedAt = a.beatAt.coerceIn(a.startedAt, now),
            levelId = a.levelId,
            levelName = a.levelName,
            durationMs = a.elapsedMs,
            roundsDone = a.roundsDone,
            roundsTotal = a.roundsTotal,
            closeCount = a.closeCount,
            maxBpm = a.maxBpm,
            outcome = outcome,
            controlOnly = a.controlOnly,
        )
        if (_settings.value.keepHistory || outcome == Outcome.INTERRUPTED) {
            val list = (listOf(record) + _history.value).take(MAX_HISTORY)
            _history.value = list
            store.saveHistory(list)
        }
        pacingLog = Pacing.prune(pacingLog + PacingEntry.from(record), now)
        store.savePacingLog(pacingLog)
        addLapse(record, now)
    }

    /** "Não aguentei" e interrompidas ficam registrados mesmo com o histórico desligado. */
    private fun addLapse(record: SessionRecord, now: Long) {
        val lapse = Lapse.from(record) ?: return
        val list = Progress.pruneLapses(_lapses.value + lapse, now)
        _lapses.value = list
        store.saveLapses(list)
    }

    private fun saveHeartbeat() {
        val e = engine ?: return
        lastBeat = SystemClock.elapsedRealtime()
        store.saveActive(
            ActiveSession(
                startedAt = sessionStartedAt,
                levelId = e.level.id,
                levelName = e.level.name,
                roundsTotal = e.level.rounds,
                focusMode = sessionFocusMode,
                controlOnly = e.controlOnly,
                beatAt = clock.now(),
                elapsedMs = e.activeElapsedMs,
                roundsDone = e.roundsCompleted,
                closeCount = e.closeCount,
                maxBpm = e.maxBpm,
            )
        )
    }

    /** Trilha calma nas pausas: entra suave na fase PARE e sai quando o ritmo volta. */
    private fun ambientTick(e: SessionEngine) {
        val s = _settings.value
        val want = s.pauseSound && !paused && e.phase == PhaseType.STOP
        if (want) {
            ambient.gain = s.ambientVolume
            ambient.track = s.breatheTrack
            ambient.breathLevel = if (s.breathGuide) Breath.level(e.phaseElapsedMs) else 0.4f
            if (!ambient.isPlaying) ambient.start()
        } else if (ambient.isPlaying && !breatheActive) {
            ambient.stop()
        }
    }

    private fun breathTick(e: SessionEngine) {
        val s = _settings.value
        if (e.phase != PhaseType.STOP || !s.breathGuide) {
            lastBreathStep = -1L
            return
        }
        val step = Breath.step(e.phaseElapsedMs)
        if (lastBreathStep == -1L) {
            // início da pausa: a troca de fase já avisou
            lastBreathStep = step
            return
        }
        if (step != lastBreathStep) {
            lastBreathStep = step
            if (s.vibrateBreath) feedback.vibrateBreath(Breath.isInhale(e.phaseElapsedMs))
        }
    }

    private fun handle(events: List<EngineEvent>) {
        if (events.isEmpty()) return
        val s = _settings.value
        val phaseChanged = events.any { it is EngineEvent.PhaseStart }
        for (ev in events) {
            when (ev) {
                is EngineEvent.PhaseStart -> {
                    // cada pausa nova pode voltar a tocar a trilha, mesmo que outro app tenha pego o áudio antes
                    if (ev.type == PhaseType.STOP) ambient.allowAgain()
                    if (s.phaseSound) feedback.soundFor(ev.type)?.let { feedback.play(it, s.volume) }
                    if (s.vibratePhase) feedback.vibratePhase(ev.type)
                }
                EngineEvent.Beat -> if (!phaseChanged) {
                    if (s.beatSound) feedback.play(Sound.TICK, s.volume)
                    if (s.vibrateBeat) feedback.vibrateBeat()
                }
                EngineEvent.CountTick -> {
                    if (s.phaseSound) feedback.play(Sound.COUNT, s.volume)
                    if (s.vibratePhase) feedback.vibrateTick()
                }
            }
        }
    }

    private fun publish() {
        val e = engine
        if (e == null) {
            _session.value = null
            return
        }
        _session.value = SessionUi(
            level = e.level,
            phase = e.phase,
            round = e.round,
            totalRounds = e.level.rounds,
            phaseRemainingMs = e.phaseRemainingMs,
            phaseDurationMs = e.phaseDurationMs,
            phaseElapsedMs = e.phaseElapsedMs,
            bpm = e.bpm,
            nextBpm = e.nextBpm,
            beatCount = e.beatCount,
            closeCount = e.closeCount,
            elapsedMs = e.activeElapsedMs,
            paused = paused,
            controlOnly = e.controlOnly,
            notice = notice,
            focused = sessionFocusMode && sessionPinned,
            blocked = inMultiWindow,
        )
    }

    override fun onCleared() {
        ticker?.cancel()
        ambient.stop()
        feedback.release()
        gameSounds?.release()
    }

    companion object {
        /** Versão da apresentação (sobe quando ela mudar e precisar ser mostrada de novo). */
        const val CURRENT_VERSION = 1
        private const val MAX_HISTORY = 500
        private const val LOCK_AFTER_MS = 30_000L
        private const val HEARTBEAT_MS = 5_000L

        /** Intervalo mínimo entre duas sugestões de leitura ao abrir o app. */
        const val NUDGE_EVERY_MS = 20 * 3_600_000L
    }
}
