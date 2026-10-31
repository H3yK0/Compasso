package app.compasso.data

import android.content.Context
import app.compasso.games.GameKind
import app.compasso.model.Accent
import app.compasso.model.Codename
import app.compasso.model.Codenames
import app.compasso.model.FocusSettings
import app.compasso.model.Lapse
import app.compasso.model.Level
import app.compasso.model.Levels
import app.compasso.model.Outcome
import app.compasso.model.PointEntry
import app.compasso.model.PointKind
import app.compasso.model.Pacing
import app.compasso.model.PacingConfig
import app.compasso.model.PacingEntry
import app.compasso.model.PacingSettings
import app.compasso.model.PendingChange
import app.compasso.model.ReleaseMode
import app.compasso.model.SessionRecord
import app.compasso.model.TrustedTime
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Idioma do app. [SYSTEM] segue o celular (português, inglês ou espanhol; os outros caem no inglês). */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    PT("pt-BR"),
    EN("en"),
    ES("es"),
}

data class Settings(
    val beatSound: Boolean = true,
    val phaseSound: Boolean = true,
    val volume: Float = 0.8f,
    val vibratePhase: Boolean = true,
    val vibrateBeat: Boolean = false,
    val countdown: Boolean = true,
    val breathGuide: Boolean = true,
    val vibrateBreath: Boolean = false,
    val discreet: Boolean = true,
    val keepHistory: Boolean = true,
    /** Noite é o visual principal do Compasso. */
    val theme: ThemeMode = ThemeMode.DARK,
    /** Trilha calma na tela Respirar. */
    val breatheSound: Boolean = true,
    /** Trilha calma nas pausas da sessão (junto com a respiração guiada). */
    val pauseSound: Boolean = true,
    val ambientVolume: Float = 0.7f,
    /** Dicas do modo "ir descobrindo". */
    val showTips: Boolean = true,
    /** Cor de destaque escolhida (as especiais dependem do nível atual). */
    val accent: Accent = Accent.TEAL,
    val language: AppLanguage = AppLanguage.SYSTEM,
)

/** Sessão em andamento gravada no celular: se o app for fechado no meio, ela não some sem registro. */
data class ActiveSession(
    val startedAt: Long,
    val levelId: String,
    val levelName: String,
    val roundsTotal: Int,
    val focusMode: Boolean,
    val controlOnly: Boolean,
    val beatAt: Long,
    val elapsedMs: Long,
    val roundsDone: Int,
    val closeCount: Int,
    val maxBpm: Int,
)

/** Tudo fica só neste celular (SharedPreferences privadas do app). */
class Store(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("compasso", Context.MODE_PRIVATE)

    // ---------- Ajustes ----------

    fun loadSettings(): Settings {
        val d = Settings()
        return Settings(
            beatSound = prefs.getBoolean("beatSound", d.beatSound),
            phaseSound = prefs.getBoolean("phaseSound", d.phaseSound),
            volume = prefs.getFloat("volume", d.volume).coerceIn(0f, 1f),
            vibratePhase = prefs.getBoolean("vibratePhase", d.vibratePhase),
            vibrateBeat = prefs.getBoolean("vibrateBeat", d.vibrateBeat),
            countdown = prefs.getBoolean("countdown", d.countdown),
            breathGuide = prefs.getBoolean("breathGuide", d.breathGuide),
            vibrateBreath = prefs.getBoolean("vibrateBreath", d.vibrateBreath),
            discreet = prefs.getBoolean("discreet", d.discreet),
            keepHistory = prefs.getBoolean("keepHistory", d.keepHistory),
            theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", null) ?: d.theme.name) }.getOrDefault(d.theme),
            breatheSound = prefs.getBoolean("breatheSound", d.breatheSound),
            pauseSound = prefs.getBoolean("pauseSound", d.pauseSound),
            ambientVolume = prefs.getFloat("ambientVolume", d.ambientVolume).coerceIn(0f, 1f),
            showTips = prefs.getBoolean("showTips", d.showTips),
            accent = runCatching { Accent.valueOf(prefs.getString("accent", null) ?: d.accent.name) }.getOrDefault(d.accent),
            language = loadLanguage(),
        )
    }

    fun saveSettings(s: Settings) {
        prefs.edit()
            .putBoolean("beatSound", s.beatSound)
            .putBoolean("phaseSound", s.phaseSound)
            .putFloat("volume", s.volume)
            .putBoolean("vibratePhase", s.vibratePhase)
            .putBoolean("vibrateBeat", s.vibrateBeat)
            .putBoolean("countdown", s.countdown)
            .putBoolean("breathGuide", s.breathGuide)
            .putBoolean("vibrateBreath", s.vibrateBreath)
            .putBoolean("discreet", s.discreet)
            .putBoolean("keepHistory", s.keepHistory)
            .putString("theme", s.theme.name)
            .putBoolean("breatheSound", s.breatheSound)
            .putBoolean("pauseSound", s.pauseSound)
            .putFloat("ambientVolume", s.ambientVolume)
            .putBoolean("showTips", s.showTips)
            .putString("accent", s.accent.name)
            .putString("language", s.language.name)
            .apply()
    }

    /** Lido também antes de a tela abrir (para montar o app já no idioma certo). */
    fun loadLanguage(): AppLanguage =
        runCatching { AppLanguage.valueOf(prefs.getString("language", null) ?: AppLanguage.SYSTEM.name) }.getOrDefault(AppLanguage.SYSTEM)

    // ---------- Nível personalizado ----------

    fun loadCustomLevel(): Level {
        val d = Levels.DEFAULT_CUSTOM
        return d.copy(
            rounds = prefs.getInt("c_rounds", d.rounds),
            goMinSec = prefs.getInt("c_go_min", d.goMinSec),
            goMaxSec = prefs.getInt("c_go_max", d.goMaxSec),
            stopMinSec = prefs.getInt("c_stop_min", d.stopMinSec),
            stopMaxSec = prefs.getInt("c_stop_max", d.stopMaxSec),
            bpmStart = prefs.getInt("c_bpm_start", d.bpmStart),
            bpmEnd = prefs.getInt("c_bpm_end", d.bpmEnd),
            release = runCatching { ReleaseMode.valueOf(prefs.getString("c_release", null) ?: d.release.name) }.getOrDefault(d.release),
            releaseChance = prefs.getInt("c_chance", d.releaseChance),
        ).sanitized()
    }

    fun saveCustomLevel(l: Level) {
        prefs.edit()
            .putInt("c_rounds", l.rounds)
            .putInt("c_go_min", l.goMinSec)
            .putInt("c_go_max", l.goMaxSec)
            .putInt("c_stop_min", l.stopMinSec)
            .putInt("c_stop_max", l.stopMaxSec)
            .putInt("c_bpm_start", l.bpmStart)
            .putInt("c_bpm_end", l.bpmEnd)
            .putString("c_release", l.release.name)
            .putInt("c_chance", l.releaseChance)
            .apply()
    }

    // ---------- Histórico ----------

    fun loadHistory(): List<SessionRecord> {
        val raw = prefs.getString("history", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    val start = o.getLong("t")
                    val duration = o.getLong("d")
                    SessionRecord(
                        startedAt = start,
                        endedAt = o.optLong("e", start + duration),
                        levelId = o.getString("id"),
                        levelName = o.getString("n"),
                        durationMs = duration,
                        roundsDone = o.getInt("r"),
                        roundsTotal = o.getInt("rt"),
                        closeCount = o.getInt("c"),
                        maxBpm = o.optInt("b", 0),
                        outcome = Outcome.valueOf(o.getString("o")),
                        controlOnly = o.optBoolean("co", false),
                        focused = o.optBoolean("f", false),
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun saveHistory(list: List<SessionRecord>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("t", r.startedAt)
                    .put("e", r.endedAt)
                    .put("id", r.levelId)
                    .put("n", r.levelName)
                    .put("d", r.durationMs)
                    .put("r", r.roundsDone)
                    .put("rt", r.roundsTotal)
                    .put("c", r.closeCount)
                    .put("b", r.maxBpm)
                    .put("o", r.outcome.name)
                    .put("co", r.controlOnly)
                    .put("f", r.focused)
            )
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
    }

    // ---------- Controle inteligente ----------

    fun loadPacing(): PacingSettings {
        val config = PacingConfig(
            enabled = prefs.getBoolean("p_on", true),
            intervalHours = prefs.getInt("p_hours", 24).takeIf { it in Pacing.INTERVAL_OPTIONS } ?: 24,
        )
        val at = prefs.getLong("p_pend_at", 0L)
        val pendingHours = prefs.getInt("p_pend_hours", 24)
        val pending = if (at > 0L && pendingHours in Pacing.INTERVAL_OPTIONS) {
            PendingChange(PacingConfig(prefs.getBoolean("p_pend_on", true), pendingHours), at)
        } else {
            null
        }
        return PacingSettings(config, pending)
    }

    fun savePacing(s: PacingSettings) {
        val e = prefs.edit()
            .putBoolean("p_on", s.config.enabled)
            .putInt("p_hours", s.config.intervalHours)
        val p = s.pending
        if (p == null) {
            e.remove("p_pend_at").remove("p_pend_on").remove("p_pend_hours")
        } else {
            e.putLong("p_pend_at", p.effectiveAt).putBoolean("p_pend_on", p.target.enabled).putInt("p_pend_hours", p.target.intervalHours)
        }
        e.apply()
    }

    fun loadPacingLog(): List<PacingEntry> {
        val raw = prefs.getString("pacing", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    PacingEntry(o.getLong("s"), o.getLong("e"), o.getBoolean("r"), o.getBoolean("c"))
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun savePacingLog(list: List<PacingEntry>) {
        val arr = JSONArray()
        list.forEach { p -> arr.put(JSONObject().put("s", p.startedAt).put("e", p.endedAt).put("r", p.released).put("c", p.counts)) }
        prefs.edit().putString("pacing", arr.toString()).apply()
    }

    // ---------- Pontos de constância ----------

    fun loadPoints(): List<PointEntry> {
        val raw = prefs.getString("points", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    PointEntry(o.getLong("a"), PointKind.valueOf(o.getString("k")), o.getInt("p"))
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun savePoints(list: List<PointEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("a", it.at).put("k", it.kind.name).put("p", it.points)) }
        prefs.edit().putString("points", arr.toString()).apply()
    }

    // ---------- Lapsos ("não segurei" e sessões interrompidas) ----------

    /** Só o momento e o tipo. Não some com o histórico: conta para a sequência e para o Invicto. */
    fun loadLapses(): List<Lapse> {
        val raw = prefs.getString("lapses", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    Lapse(o.getLong("a"), Outcome.valueOf(o.getString("o")))
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun saveLapses(list: List<Lapse>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("a", it.at).put("o", it.outcome.name)) }
        prefs.edit().putString("lapses", arr.toString()).apply()
    }

    // ---------- Relógio confiável ----------

    fun loadClock(): TrustedTime.Mark? {
        if (!prefs.contains("clk_t")) return null
        return TrustedTime.Mark(prefs.getLong("clk_t", 0L), prefs.getLong("clk_u", 0L))
    }

    fun saveClock(m: TrustedTime.Mark) {
        prefs.edit().putLong("clk_t", m.trusted).putLong("clk_u", m.uptime).apply()
    }

    // ---------- Codinome ----------

    fun loadCodename(): Codename? {
        if (!prefs.contains("cn_noun")) return null
        return Codename(
            noun = prefs.getInt("cn_noun", 0).coerceIn(0, Codenames.nounCount - 1),
            adjective = prefs.getInt("cn_adj", 0).coerceIn(0, Codenames.adjectiveCount - 1),
            number = prefs.getInt("cn_num", 10),
            seed = prefs.getLong("cn_seed", 0L),
        )
    }

    fun saveCodename(c: Codename) {
        prefs.edit()
            .putInt("cn_noun", c.noun)
            .putInt("cn_adj", c.adjective)
            .putInt("cn_num", c.number)
            .putLong("cn_seed", c.seed)
            .apply()
    }

    // ---------- Modo foco ----------

    fun loadFocus(): FocusSettings = FocusSettings(prefs.getBoolean("f_on", true), prefs.getLong("f_off_at", 0L))

    fun saveFocus(f: FocusSettings) {
        prefs.edit().putBoolean("f_on", f.enabled).putLong("f_off_at", f.offAt).apply()
    }

    // ---------- Sessão em andamento ----------

    fun loadActive(): ActiveSession? {
        val raw = prefs.getString("active", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            ActiveSession(
                startedAt = o.getLong("t"),
                levelId = o.getString("id"),
                levelName = o.getString("n"),
                roundsTotal = o.getInt("rt"),
                focusMode = o.getBoolean("fm"),
                controlOnly = o.optBoolean("co", false),
                beatAt = o.getLong("hb"),
                elapsedMs = o.getLong("d"),
                roundsDone = o.getInt("r"),
                closeCount = o.getInt("c"),
                maxBpm = o.optInt("b", 0),
            )
        }.getOrNull()
    }

    fun saveActive(a: ActiveSession?) {
        if (a == null) {
            prefs.edit().remove("active").apply()
            return
        }
        val o = JSONObject()
            .put("t", a.startedAt).put("id", a.levelId).put("n", a.levelName).put("rt", a.roundsTotal)
            .put("fm", a.focusMode).put("co", a.controlOnly).put("hb", a.beatAt).put("d", a.elapsedMs)
            .put("r", a.roundsDone).put("c", a.closeCount).put("b", a.maxBpm)
        prefs.edit().putString("active", o.toString()).apply()
    }

    // ---------- Dicas, novidades, leituras e jogos ----------

    fun loadTipsSeen(): Set<String> = prefs.getStringSet("tips", emptySet())?.toSet() ?: emptySet()

    fun saveTipsSeen(ids: Set<String>) {
        prefs.edit().putStringSet("tips", HashSet(ids)).apply()
    }

    /** Versão da apresentação já vista (para mostrar a apresentação uma vez). */
    var seenVersion: Int
        get() = prefs.getInt("seen_version", 0)
        set(value) {
            prefs.edit().putInt("seen_version", value).apply()
        }

    fun loadRead(): Set<String> = prefs.getStringSet("read", emptySet())?.toSet() ?: emptySet()

    fun saveRead(ids: Set<String>) {
        prefs.edit().putStringSet("read", HashSet(ids)).apply()
    }

    fun loadBest(): Map<GameKind, Int> = GameKind.entries.associateWith { prefs.getInt("best_${it.name}", 0) }

    fun saveBest(game: GameKind, score: Int) {
        prefs.edit().putInt("best_${game.name}", score).apply()
    }

    // ---------- Senha ----------

    fun hasPin(): Boolean = prefs.contains("pinHash")

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        prefs.edit()
            .putString("pinSalt", saltB64)
            .putString("pinHash", hash(saltB64, pin))
            .putBoolean("pinOffered", true)
            .apply()
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString("pinSalt", null) ?: return false
        val stored = prefs.getString("pinHash", null) ?: return false
        return MessageDigest.isEqual(stored.toByteArray(), hash(salt, pin).toByteArray())
    }

    fun removePin() {
        prefs.edit().remove("pinHash").remove("pinSalt").remove("pinFails").remove("pinWaitUntil").apply()
    }

    /** Tentativas erradas seguidas (gravadas: fechar o app não zera a contagem). */
    var pinFails: Int
        get() = prefs.getInt("pinFails", 0)
        set(value) {
            prefs.edit().putInt("pinFails", value).apply()
        }

    /** Até quando a senha não pode ser tentada de novo (horário confiável). */
    var pinWaitUntil: Long
        get() = prefs.getLong("pinWaitUntil", 0L)
        set(value) {
            prefs.edit().putLong("pinWaitUntil", value).apply()
        }

    /** Se o app já ofereceu criar a senha na primeira abertura. */
    var pinOffered: Boolean
        get() = prefs.getBoolean("pinOffered", false)
        set(value) {
            prefs.edit().putBoolean("pinOffered", value).apply()
        }

    private fun hash(salt: String, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(md.digest("$salt:$pin".toByteArray(Charsets.UTF_8)))
    }
}
