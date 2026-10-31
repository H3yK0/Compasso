package app.compasso.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class GameSound { PAD1, PAD2, PAD3, PAD4, WRONG, PLACE, CLEAR, MERGE }

/** Sons curtos e suaves dos jogos, gerados na primeira abertura (sem arquivos de áudio). */
class GameSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val ids = HashMap<GameSound, Int>()
    private val ready = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(ready) { ready += id } }
        val dir = File(context.cacheDir, "sons").apply { mkdirs() }
        for (s in GameSound.entries) {
            runCatching {
                val f = File(dir, "jogo_${s.name.lowercase()}_v1.wav")
                if (!f.exists() || f.length() == 0L) f.writeBytes(wav(render(s)))
                ids[s] = pool.load(f.path, 1)
            }
        }
    }

    fun play(sound: GameSound, volume: Float = 0.7f) {
        val id = ids[sound] ?: return
        if (!synchronized(ready) { id in ready }) return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() {
        runCatching { pool.release() }
    }

    private companion object {
        const val RATE = 44_100
        val PADS = doubleArrayOf(523.25, 659.25, 783.99, 880.0)

        fun render(s: GameSound): FloatArray = when (s) {
            GameSound.PAD1 -> marimba(PADS[0])
            GameSound.PAD2 -> marimba(PADS[1])
            GameSound.PAD3 -> marimba(PADS[2])
            GameSound.PAD4 -> marimba(PADS[3])
            GameSound.WRONG -> tone(doubleArrayOf(196.0, 185.0), 0.5, 0.35)
            GameSound.PLACE -> marimba(392.0, length = 0.18, gain = 0.45)
            GameSound.CLEAR -> chime()
            GameSound.MERGE -> marimba(587.33, length = 0.22, gain = 0.4)
        }

        fun marimba(f: Double, length: Double = 0.45, gain: Double = 0.6): FloatArray {
            val n = (RATE * length).toInt()
            return FloatArray(n) { i ->
                val t = i.toDouble() / RATE
                val env = min(1.0, t / 0.004) * exp(-t / (length / 4.5))
                val s = sin(2 * PI * f * t) + 0.3 * sin(2 * PI * f * 4 * t) * exp(-t / 0.03)
                (s * env * gain * 0.6).toFloat()
            }
        }

        fun tone(freqs: DoubleArray, length: Double, gain: Double): FloatArray {
            val n = (RATE * length).toInt()
            return FloatArray(n) { i ->
                val t = i.toDouble() / RATE
                val env = min(1.0, t / 0.01) * exp(-t / (length / 3))
                (freqs.sumOf { sin(2 * PI * it * t) } / freqs.size * env * gain).toFloat()
            }
        }

        fun chime(): FloatArray {
            val notes = doubleArrayOf(659.25, 783.99, 1046.5)
            val step = (RATE * 0.07).toInt()
            val len = (RATE * 0.7).toInt()
            val out = FloatArray(step * notes.size + len)
            notes.forEachIndexed { k, f ->
                for (i in 0 until len) {
                    val t = i.toDouble() / RATE
                    val env = min(1.0, t / 0.004) * exp(-t / 0.18)
                    out[k * step + i] += (sin(2 * PI * f * t) * env * 0.35).toFloat()
                }
            }
            return out
        }

        fun wav(samples: FloatArray): ByteArray {
            val dataSize = samples.size * 2
            val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            buf.put("RIFF".toByteArray(Charsets.US_ASCII)); buf.putInt(36 + dataSize)
            buf.put("WAVE".toByteArray(Charsets.US_ASCII)); buf.put("fmt ".toByteArray(Charsets.US_ASCII))
            buf.putInt(16); buf.putShort(1.toShort()); buf.putShort(1.toShort())
            buf.putInt(RATE); buf.putInt(RATE * 2); buf.putShort(2.toShort()); buf.putShort(16.toShort())
            buf.put("data".toByteArray(Charsets.US_ASCII)); buf.putInt(dataSize)
            for (x in samples) buf.putShort((x.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            return buf.array()
        }
    }
}
