package app.compasso.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.compasso.model.PhaseType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class Sound { TICK, COUNT, GO, STOP, RELEASE, END }

/**
 * Sons e vibração. Os sons são sintetizados na primeira abertura (sem arquivos de áudio
 * no app) e tocados pelo SoundPool, que tem pouca latência — importante para a batida.
 */
class Feedback(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Sound, Int>()
    private val ready = HashSet<Int>()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(ready) { ready.add(sampleId) }
        }
        val dir = File(context.cacheDir, "sons").apply { mkdirs() }
        for (s in Sound.entries) {
            runCatching {
                val f = File(dir, "${s.name.lowercase()}_v1.wav")
                if (!f.exists() || f.length() == 0L) f.writeBytes(Synth.wav(s))
                ids[s] = pool.load(f.path, 1)
            }
        }
    }

    fun play(sound: Sound, volume: Float) {
        val id = ids[sound] ?: return
        if (!synchronized(ready) { id in ready }) return
        val v = volume.coerceIn(0f, 1f)
        if (v > 0f) pool.play(id, v, v, 1, 0, 1f)
    }

    fun soundFor(type: PhaseType): Sound? = when (type) {
        PhaseType.GO -> Sound.GO
        PhaseType.STOP -> Sound.STOP
        PhaseType.RELEASE -> Sound.RELEASE
        PhaseType.DENIED -> Sound.END
        PhaseType.COUNTDOWN -> null
    }

    fun vibrateBeat() = vibrate(longArrayOf(0, 18))

    fun vibrateTick() = vibrate(longArrayOf(0, 30))

    fun vibratePhase(type: PhaseType) {
        when (type) {
            PhaseType.GO -> vibrate(longArrayOf(0, 70))
            PhaseType.STOP -> vibrate(longArrayOf(0, 250, 120, 250))
            PhaseType.RELEASE -> vibrate(longArrayOf(0, 90, 80, 90, 80, 320))
            PhaseType.DENIED -> vibrate(longArrayOf(0, 450))
            PhaseType.COUNTDOWN -> Unit
        }
    }

    /** Toque leve na troca da respiração: um ao inspirar, dois curtinhos ao soltar o ar. */
    fun vibrateBreath(inhale: Boolean) {
        if (inhale) {
            vibrateSoft(longArrayOf(0, 45), intArrayOf(0, 90))
        } else {
            vibrateSoft(longArrayOf(0, 25, 110, 25), intArrayOf(0, 70, 0, 70))
        }
    }

    private fun vibrateSoft(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        runCatching {
            if (!v.hasVibrator()) return
            if (v.hasAmplitudeControl()) {
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        }
    }

    private fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        runCatching {
            if (v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    fun release() {
        runCatching { pool.release() }
        runCatching { vibrator?.cancel() }
    }
}

/** Gera os sons em WAV (PCM 16 bits, mono). */
internal object Synth {
    private const val RATE = 44_100

    fun wav(sound: Sound): ByteArray = encode(
        when (sound) {
            Sound.TICK -> click()
            Sound.COUNT -> notes(listOf(1046.5), noteSec = 0.10, gain = 0.55)
            Sound.GO -> notes(listOf(523.25, 783.99), noteSec = 0.11, gain = 0.6)
            Sound.STOP -> notes(listOf(880.0, 587.33), noteSec = 0.17, gain = 0.85)
            Sound.RELEASE -> notes(listOf(523.25, 659.25, 783.99, 1046.5), noteSec = 0.12, gain = 0.6, tailSec = 0.45)
            Sound.END -> notes(listOf(392.0, 293.66), noteSec = 0.24, gain = 0.65, tailSec = 0.2)
        }
    )

    /** Clique curto, tipo bloco de madeira de metrônomo. */
    private fun click(): FloatArray {
        val n = (RATE * 0.05).toInt()
        return FloatArray(n) { i ->
            val t = i.toDouble() / RATE
            val env = exp(-t / 0.009)
            val s = 0.65 * sin(2 * PI * 1250 * t) + 0.35 * sin(2 * PI * 2500 * t)
            (s * env * 0.9).toFloat()
        }
    }

    /** Sequência de notas suaves (seno + harmônico) com ataque rápido e decaimento. */
    private fun notes(freqs: List<Double>, noteSec: Double, gain: Double, tailSec: Double = 0.08): FloatArray {
        val step = (RATE * noteSec).toInt()
        val lastLen = step + (RATE * tailSec).toInt()
        val total = step * (freqs.size - 1) + lastLen
        val out = FloatArray(total)
        freqs.forEachIndexed { k, f ->
            val start = k * step
            val len = if (k == freqs.lastIndex) lastLen else (step * 1.3).toInt()
            val tau = len.toDouble() / RATE / 3.2
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= total) break
                val t = i.toDouble() / RATE
                val attack = min(1.0, t / 0.006)
                val s = sin(2 * PI * f * t) + 0.22 * sin(2 * PI * 2 * f * t)
                out[idx] += (s * attack * exp(-t / tau) * gain * 0.75).toFloat()
            }
        }
        return out
    }

    private fun encode(samples: FloatArray): ByteArray {
        val dataSize = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII))
        buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII))
        buf.putInt(16)
        buf.putShort(1.toShort()) // PCM
        buf.putShort(1.toShort()) // mono
        buf.putInt(RATE)
        buf.putInt(RATE * 2)
        buf.putShort(2.toShort())
        buf.putShort(16.toShort())
        buf.put("data".toByteArray(Charsets.US_ASCII))
        buf.putInt(dataSize)
        for (x in samples) buf.putShort((x.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        return buf.array()
    }
}
