package app.compasso.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Trilha calma gerada no próprio app, sem arquivos de música (então sem direitos autorais):
 * - acordes suaves que mudam devagar a cada 16 s, com o filtro abrindo na inspiração;
 * - um "mar" de ruído filtrado que cresce ao inspirar e recua ao soltar o ar;
 * - sinos raros numa escala pentatônica;
 * - um pouco de reverberação para dar espaço.
 */
class AmbientSynth(private val sampleRate: Int = 44_100, seed: Long = 7L) {
    private val random = Random(seed)
    private var t = 0L

    private val chords = arrayOf(
        doubleArrayOf(146.83, 220.00, 329.63, 369.99, 554.37), // Ré maior com 9ª
        doubleArrayOf(123.47, 185.00, 293.66, 440.00, 554.37), // Si menor com 9ª
        doubleArrayOf(98.00, 146.83, 246.94, 369.99, 440.00), // Sol maior com 9ª
        doubleArrayOf(110.00, 164.81, 246.94, 369.99, 493.88), // Lá suspenso
    )
    private val chordSec = 16.0
    private val fadeSec = 5.0
    private val voiceAmp = doubleArrayOf(0.050, 0.042, 0.034, 0.030, 0.022)
    private val pans = DoubleArray(5) { -0.55 + 1.1 * it / 4.0 }
    private val phases = Array(chords.size) { Array(5) { DoubleArray(2) } }
    private val lfoPhase = DoubleArray(5) { random.nextDouble() * 2 * PI }
    private val lfoRate = DoubleArray(5) { 0.03 + random.nextDouble() * 0.05 }

    private var padL = 0.0
    private var padR = 0.0
    private var nL1 = 0.0
    private var nL2 = 0.0
    private var nR1 = 0.0
    private var nR2 = 0.0

    private class Bell(val freq: Double, val pan: Double, val amp: Double) {
        var age = 0L
        val phase = DoubleArray(4)
    }

    private val bells = ArrayList<Bell>()
    private var nextBellAt = (sampleRate * 3.5).toLong()
    private val bellNotes = doubleArrayOf(587.33, 659.25, 739.99, 880.0, 987.77, 1174.66)

    private val reverb = Reverb(sampleRate)
    private var breath = 0.0
    private var master = 0.0

    /** 1 = tocando; 0 = sumindo (fade). */
    @Volatile
    var masterTarget = 1.0
    val masterLevel: Double get() = master

    private val breathCoef = 1.0 - exp(-1.0 / (0.25 * sampleRate))
    private val masterCoef = 1.0 - exp(-1.0 / (1.2 * sampleRate))
    private val twoPi = 2 * PI

    /** Preenche [out] (estéreo intercalado) com [frames] quadros. [breathTarget]: 0 vazio → 1 cheio. */
    fun render(out: FloatArray, frames: Int, breathTarget: Float) {
        val sr = sampleRate.toDouble()
        for (i in 0 until frames) {
            breath += (breathTarget - breath) * breathCoef
            master += (masterTarget - master) * masterCoef
            val sec = t / sr

            // ---- acordes com troca suave ----
            val pos = sec / chordSec
            val idx = floor(pos).toInt() % chords.size
            val nextIdx = (idx + 1) % chords.size
            val within = (pos - floor(pos)) * chordSec
            val x = if (within < chordSec - fadeSec) 0.0 else (within - (chordSec - fadeSec)) / fadeSec
            val wCur = Sine.cos(x * PI / 2)
            val wNext = Sine.sin(x * PI / 2)
            var l = 0.0
            var r = 0.0
            for (pass in 0..1) {
                val ci = if (pass == 0) idx else nextIdx
                val w = if (pass == 0) wCur else wNext
                if (w < 0.001) continue
                val ph = phases[ci]
                for (v in 0 until 5) {
                    val f = chords[ci][v]
                    ph[v][0] += twoPi * f * 0.9985 / sr
                    ph[v][1] += twoPi * f * 1.0015 / sr
                    if (ph[v][0] > twoPi) ph[v][0] -= twoPi
                    if (ph[v][1] > twoPi) ph[v][1] -= twoPi
                    val s = Sine.sin(ph[v][0]) + Sine.sin(ph[v][1]) + 0.22 * Sine.sin(2 * ph[v][0]) + 0.07 * Sine.sin(3 * ph[v][1])
                    val lfo = 0.62 + 0.38 * Sine.sin(lfoPhase[v] + twoPi * lfoRate[v] * sec)
                    val a = s * w * lfo * voiceAmp[v]
                    l += a * (1 - pans[v]) * 0.5
                    r += a * (1 + pans[v]) * 0.5
                }
            }
            // filtro que abre na inspiração
            val padCut = 650.0 + 1700.0 * breath
            val pa = 1.0 - exp(-twoPi * padCut / sr)
            padL += (l - padL) * pa
            padR += (r - padR) * pa

            // ---- mar ----
            val seaCut = 380.0 + 650.0 * breath
            val na = 1.0 - exp(-twoPi * seaCut / sr)
            nL1 += (random.nextDouble(-1.0, 1.0) - nL1) * na
            nL2 += (nL1 - nL2) * na
            nR1 += (random.nextDouble(-1.0, 1.0) - nR1) * na
            nR2 += (nR1 - nR2) * na
            val seaAmp = 0.05 + 0.32 * breath * breath
            val seaL = nL2 * seaAmp
            val seaR = nR2 * seaAmp

            // ---- sinos ----
            if (t >= nextBellAt) {
                bells += Bell(
                    bellNotes[random.nextInt(bellNotes.size)],
                    random.nextDouble(-0.7, 0.7),
                    random.nextDouble(0.045, 0.075),
                )
                nextBellAt = t + (sr * random.nextDouble(5.0, 11.0)).toLong()
            }
            var bL = 0.0
            var bR = 0.0
            val it = bells.iterator()
            while (it.hasNext()) {
                val b = it.next()
                val age = b.age / sr
                if (age > 7.0) {
                    it.remove()
                    continue
                }
                val attack = (age / 0.006).coerceAtMost(1.0)
                var s = 0.0
                for (k in 0 until 4) {
                    val ratio = BELL_RATIOS[k]
                    b.phase[k] += twoPi * b.freq * ratio / sr
                    if (b.phase[k] > twoPi) b.phase[k] -= twoPi
                    s += Sine.sin(b.phase[k]) * BELL_AMPS[k] * exp(-age / BELL_DECAY[k])
                }
                val v = s * attack * b.amp
                bL += v * (1 - b.pan) * 0.5
                bR += v * (1 + b.pan) * 0.5
                b.age++
            }

            // ---- espaço ----
            val wet = reverb.process((padL + padR) * 0.35 + (bL + bR) * 0.9)
            val outL = tanh(1.15 * (padL + seaL + bL + 0.28 * wet.first)) * 0.9 * master
            val outR = tanh(1.15 * (padR + seaR + bR + 0.28 * wet.second)) * 0.9 * master
            out[2 * i] = outL.toFloat()
            out[2 * i + 1] = outR.toFloat()
            t++
        }
    }

    private companion object {
        val BELL_RATIOS = doubleArrayOf(1.0, 2.0, 3.01, 4.2)
        val BELL_AMPS = doubleArrayOf(1.0, 0.38, 0.14, 0.07)
        val BELL_DECAY = doubleArrayOf(2.2, 1.2, 0.6, 0.35)
    }
}

/** Seno por tabela (bem mais leve que Math.sin numa trilha que roda o tempo todo). */
internal object Sine {
    private const val N = 4096
    private val table = DoubleArray(N + 1) { kotlin.math.sin(2 * PI * it / N) }

    fun sin(x: Double): Double {
        var p = x / (2 * PI)
        p -= floor(p)
        val f = p * N
        val i = f.toInt()
        val frac = f - i
        return table[i] + (table[i + 1] - table[i]) * frac
    }

    fun cos(x: Double): Double = sin(x + PI / 2)
}

/** Reverberação simples (4 filtros pente + 2 passa-tudo por canal). */
internal class Reverb(sampleRate: Int) {
    private class Comb(size: Int, val feedback: Double, val damp: Double) {
        val buf = DoubleArray(size)
        var idx = 0
        var store = 0.0
        fun process(x: Double): Double {
            val y = buf[idx]
            store = y * (1 - damp) + store * damp
            buf[idx] = x + store * feedback
            idx = (idx + 1) % buf.size
            return y
        }
    }

    private class AllPass(size: Int) {
        val buf = DoubleArray(size)
        var idx = 0
        fun process(x: Double): Double {
            val b = buf[idx]
            val y = -x + b
            buf[idx] = x + b * 0.5
            idx = (idx + 1) % buf.size
            return y
        }
    }

    private fun ms(sr: Int, m: Double) = (sr * m / 1000.0).toInt()
    private val combsL = listOf(29.7, 37.1, 41.1, 43.7).map { Comb(ms(sampleRate, it), 0.82, 0.3) }
    private val combsR = listOf(30.3, 37.7, 41.7, 44.3).map { Comb(ms(sampleRate, it), 0.82, 0.3) }
    private val apL = listOf(AllPass(ms(sampleRate, 5.0)), AllPass(ms(sampleRate, 1.7)))
    private val apR = listOf(AllPass(ms(sampleRate, 5.3)), AllPass(ms(sampleRate, 1.9)))

    fun process(x: Double): Pair<Double, Double> {
        var l = combsL.sumOf { it.process(x) } * 0.25
        var r = combsR.sumOf { it.process(x) } * 0.25
        apL.forEach { l = it.process(l) }
        apR.forEach { r = it.process(r) }
        return l to r
    }
}

/** Toca a trilha numa linha de áudio própria, recebendo o nível da respiração a cada quadro. */
class AmbientPlayer(context: Context) {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)

    @Volatile
    var breathLevel: Float = 0f

    /** Volume da trilha (0 a 1), além do volume de mídia do celular. */
    @Volatile
    var gain: Float = 0.7f

    @Volatile
    private var stopRequested = false

    /** Nos testes no computador (Robolectric) não há saída de áudio de verdade. */
    private val enabled = Build.FINGERPRINT != "robolectric"

    val isPlaying: Boolean get() = thread?.isAlive == true && !stopRequested
    private var thread: Thread? = null
    private var focusRequest: AudioFocusRequest? = null

    fun start() {
        if (!enabled) return
        if (thread?.isAlive == true) {
            stopRequested = false
            return
        }
        stopRequested = false
        requestFocus()
        thread = Thread({ loop() }, "trilha-respirar").apply {
            priority = Thread.MAX_PRIORITY
            isDaemon = true
            start()
        }
    }

    /** Some aos poucos (cerca de 1,5 s) e libera o áudio. */
    fun stop() {
        stopRequested = true
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun requestFocus() {
        val am = audioManager ?: return
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes())
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS) stop()
            }
            .build()
        runCatching { am.requestAudioFocus(req) }
        focusRequest = req
    }

    private fun attributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun loop() {
        val sr = 44_100
        val track = runCatching {
            val min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
            AudioTrack.Builder()
                .setAudioAttributes(attributes())
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(max(min, sr * 2 * 4 / 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return
        val synth = AmbientSynth(sr, seed = System.nanoTime())
        val frames = 1024
        val buf = FloatArray(frames * 2)
        runCatching { track.play() }
        var safety = 0
        while (true) {
            if (stopRequested) {
                synth.masterTarget = 0.0
                if (synth.masterLevel < 0.002 || ++safety > 200) break
            } else {
                synth.masterTarget = 1.0
                safety = 0
            }
            synth.render(buf, frames, breathLevel)
            val g = gain.coerceIn(0f, 1f)
            for (k in buf.indices) buf[k] *= g
            val written = runCatching { track.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING) }.getOrDefault(-1)
            if (written < 0) break
        }
        runCatching { track.stop() }
        runCatching { track.release() }
    }
}
