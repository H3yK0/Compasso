package app.compasso.audio

import androidx.annotation.StringRes
import app.compasso.R
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.tanh
import kotlin.random.Random

/** As trilhas calmas do app. Todas são geradas ao vivo pelo próprio app, sem arquivos de música. */
enum class Track(@StringRes val label: Int, @StringRes val description: Int) {
    TIDE(R.string.track_tide, R.string.track_tide_sub),
    RAIN(R.string.track_rain, R.string.track_rain_sub),
    FOREST(R.string.track_forest, R.string.track_forest_sub),
    BOWLS(R.string.track_bowls, R.string.track_bowls_sub),
    STARS(R.string.track_stars, R.string.track_stars_sub),
    PIANO(R.string.track_piano, R.string.track_piano_sub),
}

/** Uma trilha que se gera sozinha, quadro a quadro, e acompanha a respiração (0 vazio → 1 cheio). */
interface Soundscape {
    /** 1 = tocando; 0 = sumindo (fade). */
    var masterTarget: Double
    val masterLevel: Double

    /** Preenche [out] (estéreo intercalado) com [frames] quadros. */
    fun render(out: FloatArray, frames: Int, breathTarget: Float)
}

fun soundscapeFor(track: Track, sampleRate: Int = 44_100, seed: Long = 7L): Soundscape = when (track) {
    Track.TIDE -> AmbientSynth(sampleRate, seed)
    Track.RAIN -> RainScape(sampleRate, seed)
    Track.FOREST -> ForestScape(sampleRate, seed)
    Track.BOWLS -> BowlsScape(sampleRate, seed)
    Track.STARS -> StarsScape(sampleRate, seed)
    Track.PIANO -> PianoScape(sampleRate, seed)
}

/**
 * Base das trilhas novas: respiração suavizada, entrada e saída em fade, reverberação e um
 * limitador suave no fim (nunca passa de ±1). Cada trilha só escreve [l], [r] e [send] por quadro.
 */
abstract class BaseScape(sampleRate: Int, seed: Long) : Soundscape {
    protected val random = Random(seed)
    protected val sr = sampleRate.toDouble()
    protected val twoPi = 2 * PI
    protected var t = 0L
    protected var breath = 0.0
    private var master = 0.0

    @Volatile
    override var masterTarget = 1.0
    override val masterLevel: Double get() = master

    private val breathCoef = 1.0 - exp(-1.0 / (0.25 * sampleRate))
    private val masterCoef = 1.0 - exp(-1.0 / (1.2 * sampleRate))
    private val reverb = Reverb(sampleRate)
    protected open val wetMix = 0.3
    protected open val drive = 1.1

    protected var l = 0.0
    protected var r = 0.0

    /** Quanto deste quadro vai para a reverberação. */
    protected var send = 0.0

    protected abstract fun sample(sec: Double)

    override fun render(out: FloatArray, frames: Int, breathTarget: Float) {
        for (i in 0 until frames) {
            breath += (breathTarget - breath) * breathCoef
            master += (masterTarget - master) * masterCoef
            l = 0.0
            r = 0.0
            send = 0.0
            sample(t / sr)
            val wet = reverb.process(send)
            out[2 * i] = (tanh(drive * (l + wetMix * wet.first)) * 0.9 * master).toFloat()
            out[2 * i + 1] = (tanh(drive * (r + wetMix * wet.second)) * 0.9 * master).toFloat()
            t++
        }
    }

    protected fun pan(v: Double, p: Double) {
        l += v * (1 - p) * 0.5
        r += v * (1 + p) * 0.5
    }

    protected fun lowpassCoef(cutoffHz: Double) = 1.0 - exp(-twoPi * cutoffHz / sr)

    protected fun noise() = random.nextDouble(-1.0, 1.0)
}

/** Chuva leve: chiado suave de chuva, gotas esparsas e um acorde grave ao fundo. */
class RainScape(sampleRate: Int = 44_100, seed: Long = 7L) : BaseScape(sampleRate, seed) {
    private var hpL = 0.0
    private var hpR = 0.0
    private var lpL = 0.0
    private var lpR = 0.0
    private var padPh1 = 0.0
    private var padPh2 = 0.0
    private var padLp = 0.0

    private class Drop(val freq: Double, val pan: Double, val amp: Double, val tau: Double) {
        var age = 0L
        var phase = 0.0
    }

    private val drops = ArrayList<Drop>()
    private val hpA = lowpassCoef(500.0)

    override fun sample(sec: Double) {
        // chuva: ruído sem os graves, abafado aos poucos
        val nL = noise()
        val nR = noise()
        hpL += (nL - hpL) * hpA
        hpR += (nR - hpR) * hpA
        val la = lowpassCoef(2600.0 + 900.0 * breath)
        lpL += ((nL - hpL) - lpL) * la
        lpR += ((nR - hpR) - lpR) * la
        val rainAmp = 0.16 + 0.06 * breath
        l += lpL * rainAmp
        r += lpR * rainAmp

        // gotas: poucas por segundo, cada uma um "tic" agudo e curto
        val rate = 9.0 + 6.0 * breath
        if (random.nextDouble() < rate / sr && drops.size < 24) {
            drops += Drop(random.nextDouble(1600.0, 3800.0), random.nextDouble(-0.8, 0.8), random.nextDouble(0.015, 0.045), random.nextDouble(0.008, 0.022))
        }
        val it = drops.iterator()
        while (it.hasNext()) {
            val d = it.next()
            val age = d.age / sr
            if (age > d.tau * 8) {
                it.remove()
                continue
            }
            d.phase += twoPi * d.freq * (1.0 - 0.15 * min(1.0, age / d.tau)) / sr
            val v = Sine.sin(d.phase) * d.amp * exp(-age / d.tau)
            pan(v, d.pan)
            send += v * 0.6
            d.age++
        }

        // acorde grave e quente (lá e mi), que abre um pouco na inspiração
        padPh1 += twoPi * 110.0 / sr
        padPh2 += twoPi * 164.81 / sr
        if (padPh1 > twoPi) padPh1 -= twoPi
        if (padPh2 > twoPi) padPh2 -= twoPi
        val lfo = 0.7 + 0.3 * Sine.sin(twoPi * 0.05 * sec)
        val pad = (Sine.sin(padPh1) + 0.7 * Sine.sin(padPh2)) * 0.035 * lfo
        padLp += (pad - padLp) * lowpassCoef(500.0 + 700.0 * breath)
        l += padLp
        r += padLp
        send += padLp * 0.5
    }
}

/** Floresta noturna: vento entre as árvores, grilos ao longe, uma coruja de vez em quando e um acorde grave. */
class ForestScape(sampleRate: Int = 44_100, seed: Long = 7L) : BaseScape(sampleRate, seed) {
    private val padFreqs = doubleArrayOf(110.0, 164.81, 220.0, 246.94)
    private val padAmps = doubleArrayOf(0.030, 0.022, 0.016, 0.010)
    private val padPh = DoubleArray(4)
    private var padLp = 0.0
    private var windL = 0.0
    private var windR = 0.0
    private var windL2 = 0.0
    private var windR2 = 0.0

    private class Cricket(val freq: Double, val pan: Double, var nextAt: Long) {
        var start = -1L
        var phase = 0.0
    }

    private val crickets = listOf(
        Cricket(4150.0, -0.6, (sr * 0.8).toLong()),
        Cricket(4420.0, 0.55, (sr * 1.3).toLong()),
    )

    private var owlAt = (sr * random.nextDouble(9.0, 16.0)).toLong()
    private var owlStart = -1L
    private var owlPhase = 0.0

    override val wetMix = 0.35

    override fun sample(sec: Double) {
        // vento: ruído bem grave que cresce na inspiração
        val wa = lowpassCoef(240.0 + 380.0 * breath)
        windL += (noise() - windL) * wa
        windR += (noise() - windR) * wa
        windL2 += (windL - windL2) * wa
        windR2 += (windR - windR2) * wa
        val windAmp = 0.22 + 0.18 * breath
        l += windL2 * windAmp
        r += windR2 * windAmp

        // acorde grave (lá menor com 9ª), bem baixo
        var pad = 0.0
        for (k in padFreqs.indices) {
            padPh[k] += twoPi * padFreqs[k] / sr
            if (padPh[k] > twoPi) padPh[k] -= twoPi
            pad += Sine.sin(padPh[k]) * padAmps[k] * (0.75 + 0.25 * Sine.sin(twoPi * (0.03 + 0.01 * k) * sec + k))
        }
        padLp += (pad - padLp) * lowpassCoef(420.0 + 500.0 * breath)
        l += padLp
        r += padLp
        send += padLp * 0.6

        // grilos: três pulsos curtos a cada pouco menos de um segundo
        for (c in crickets) {
            if (c.start < 0 && t >= c.nextAt) c.start = t
            if (c.start >= 0) {
                val age = (t - c.start) / sr
                if (age > 0.16) {
                    c.start = -1
                    c.nextAt = t + (sr * random.nextDouble(0.7, 1.3)).toLong()
                } else {
                    val pulse = (age * 22.0) % 1.0
                    val env = if (pulse < 0.5) Sine.sin(PI * pulse * 2) else 0.0
                    c.phase += twoPi * c.freq / sr
                    if (c.phase > twoPi) c.phase -= twoPi
                    val v = Sine.sin(c.phase) * env * env * 0.010
                    pan(v, c.pan)
                    send += v * 0.4
                }
            }
        }

        // coruja ao longe: "hu-huu" grave e raro
        if (owlStart < 0 && t >= owlAt) owlStart = t
        if (owlStart >= 0) {
            val age = (t - owlStart) / sr
            if (age > 1.1) {
                owlStart = -1
                owlAt = t + (sr * random.nextDouble(18.0, 32.0)).toLong()
            } else {
                val (f, a) = when {
                    age < 0.28 -> 392.0 to Sine.sin(PI * age / 0.28)
                    age < 0.45 -> 0.0 to 0.0
                    else -> 349.0 to Sine.sin(PI * min(1.0, (age - 0.45) / 0.6))
                }
                if (f > 0) {
                    owlPhase += twoPi * f / sr
                    if (owlPhase > twoPi) owlPhase -= twoPi
                    val v = Sine.sin(owlPhase) * a * a * 0.028
                    pan(v, -0.3)
                    send += v * 1.2
                }
            }
        }
    }
}

/** Tigelas: tigelas de metal tocadas devagar, com o "batimento" típico, sobre um zumbido grave. */
class BowlsScape(sampleRate: Int = 44_100, seed: Long = 7L) : BaseScape(sampleRate, seed) {
    private val notes = doubleArrayOf(196.0, 220.0, 261.63, 293.66, 329.63)
    private val ratios = doubleArrayOf(1.0, 2.76, 5.40, 8.93)
    private val amps = doubleArrayOf(1.0, 0.45, 0.22, 0.10)
    private val decays = doubleArrayOf(9.0, 5.0, 3.0, 1.6)

    private class Strike(val f: Double, val beat: Double, val pan: Double, val amp: Double) {
        var age = 0L
        val ph = DoubleArray(8)
    }

    private val strikes = ArrayList<Strike>()
    private var nextAt = (sr * 0.5).toLong()
    private var dronePh1 = 0.0
    private var dronePh2 = 0.0

    override val wetMix = 0.42

    override fun sample(sec: Double) {
        if (t >= nextAt) {
            strikes += Strike(notes[random.nextInt(notes.size)], random.nextDouble(0.6, 1.6), random.nextDouble(-0.5, 0.5), random.nextDouble(0.10, 0.15))
            nextAt = t + (sr * random.nextDouble(6.0, 11.0)).toLong()
        }
        val it = strikes.iterator()
        while (it.hasNext()) {
            val s = it.next()
            val age = s.age / sr
            if (age > 16.0) {
                it.remove()
                continue
            }
            val attack = min(1.0, age / 0.004)
            var v = 0.0
            for (k in ratios.indices) {
                val f = s.f * ratios[k]
                s.ph[2 * k] += twoPi * f / sr
                s.ph[2 * k + 1] += twoPi * (f + s.beat * (k + 1)) / sr
                if (s.ph[2 * k] > twoPi) s.ph[2 * k] -= twoPi
                if (s.ph[2 * k + 1] > twoPi) s.ph[2 * k + 1] -= twoPi
                v += (Sine.sin(s.ph[2 * k]) + Sine.sin(s.ph[2 * k + 1])) * 0.5 * amps[k] * exp(-age / decays[k])
            }
            v *= attack * s.amp
            pan(v, s.pan)
            send += v
            s.age++
        }
        // zumbido grave (sol e ré) que respira junto
        dronePh1 += twoPi * 98.0 / sr
        dronePh2 += twoPi * 146.83 / sr
        if (dronePh1 > twoPi) dronePh1 -= twoPi
        if (dronePh2 > twoPi) dronePh2 -= twoPi
        val drone = (Sine.sin(dronePh1) + 0.6 * Sine.sin(dronePh2)) * (0.012 + 0.02 * breath)
        l += drone
        r += drone
        send += drone * 0.3
    }
}

/** Céu estrelado: um acorde longo em quintas que respira e pontinhos de brilho que acendem e apagam. */
class StarsScape(sampleRate: Int = 44_100, seed: Long = 7L) : BaseScape(sampleRate, seed) {
    private val drone = doubleArrayOf(55.0, 82.5, 110.0, 165.0, 220.0, 330.0)
    private val droneAmp = doubleArrayOf(0.050, 0.040, 0.034, 0.024, 0.018, 0.010)
    private val dronePh = Array(drone.size) { DoubleArray(2) }
    private var lpL = 0.0
    private var lpR = 0.0

    private val sparkle = doubleArrayOf(880.0, 1318.51, 1760.0, 1975.53, 2637.02)
    private val sparklePan = doubleArrayOf(-0.7, 0.4, -0.2, 0.7, 0.0)
    private val sparklePh = DoubleArray(sparkle.size)
    private val sparkleAmp = DoubleArray(sparkle.size)
    private val sparkleTarget = DoubleArray(sparkle.size)
    private val sparkleNext = LongArray(sparkle.size)
    private val sparkleCoef = 1.0 - exp(-1.0 / (1.5 * sampleRate))
    private var airL = 0.0
    private var airR = 0.0

    override val wetMix = 0.38

    override fun sample(sec: Double) {
        var dl = 0.0
        var dr = 0.0
        for (k in drone.indices) {
            val ph = dronePh[k]
            ph[0] += twoPi * drone[k] * 0.9994 / sr
            ph[1] += twoPi * drone[k] * 1.0006 / sr
            if (ph[0] > twoPi) ph[0] -= twoPi
            if (ph[1] > twoPi) ph[1] -= twoPi
            val swell = 0.7 + 0.3 * Sine.sin(twoPi * (0.021 + 0.007 * k) * sec + k * 1.7)
            dl += Sine.sin(ph[0]) * droneAmp[k] * swell
            dr += Sine.sin(ph[1]) * droneAmp[k] * swell
        }
        val a = lowpassCoef(280.0 + 1300.0 * breath)
        lpL += (dl - lpL) * a
        lpR += (dr - lpR) * a
        l += lpL
        r += lpR
        send += (lpL + lpR) * 0.4

        // brilhos: cada um vai para um volume sorteado de tempos em tempos, devagar
        for (k in sparkle.indices) {
            if (t >= sparkleNext[k]) {
                sparkleTarget[k] = if (random.nextDouble() < 0.55) random.nextDouble(0.004, 0.013) else 0.0
                sparkleNext[k] = t + (sr * random.nextDouble(2.0, 5.0)).toLong()
            }
            sparkleAmp[k] += (sparkleTarget[k] - sparkleAmp[k]) * sparkleCoef
            sparklePh[k] += twoPi * sparkle[k] / sr
            if (sparklePh[k] > twoPi) sparklePh[k] -= twoPi
            val v = Sine.sin(sparklePh[k]) * sparkleAmp[k]
            pan(v, sparklePan[k])
            send += v * 1.4
        }

        // "ar": ruído bem filtrado, quase imperceptível
        val na = lowpassCoef(500.0)
        airL += (noise() - airL) * na
        airR += (noise() - airR) * na
        l += airL * (0.03 + 0.03 * breath)
        r += airR * (0.03 + 0.03 * breath)
    }
}

/** Piano calmo: notas soltas numa escala pentatônica, com baixo que muda devagar e bastante espaço. */
class PianoScape(sampleRate: Int = 44_100, seed: Long = 7L) : BaseScape(sampleRate, seed) {
    private val scale = doubleArrayOf(261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33, 659.25)
    private val basses = doubleArrayOf(130.81, 110.0, 87.31, 98.0)

    private class Note(val f: Double, val amp: Double, val decay: Double, val pan: Double) {
        var age = 0L
        val ph = DoubleArray(3)
    }

    private val notes = ArrayList<Note>()
    private var nextAt = (sr * 0.6).toLong()
    private var step = 3
    private var count = 0
    private var bassIdx = 0
    private var padPh = 0.0
    private var padF = basses[0]

    override val wetMix = 0.45

    override fun sample(sec: Double) {
        if (t >= nextAt) {
            step = (step + random.nextInt(-2, 3)).coerceIn(0, scale.lastIndex)
            notes += Note(scale[step], random.nextDouble(0.07, 0.10), 2.2, random.nextDouble(-0.4, 0.4))
            if (count % 4 == 0) {
                padF = basses[bassIdx % basses.size]
                notes += Note(padF, 0.085, 3.6, -0.1)
                bassIdx++
            }
            count++
            nextAt = t + (sr * random.nextDouble(1.6, 2.8)).toLong()
        }
        val it = notes.iterator()
        while (it.hasNext()) {
            val n = it.next()
            val age = n.age / sr
            if (age > 9.0) {
                it.remove()
                continue
            }
            for (k in 0..2) {
                n.ph[k] += twoPi * n.f * (k + 1) / sr
                if (n.ph[k] > twoPi) n.ph[k] -= twoPi
            }
            val tone = Sine.sin(n.ph[0]) + 0.45 * Sine.sin(n.ph[1]) * exp(-age / 0.6) + 0.18 * Sine.sin(n.ph[2]) * exp(-age / 0.3)
            val v = tone * n.amp * min(1.0, age / 0.004) * exp(-age / n.decay)
            pan(v, n.pan)
            send += v
            n.age++
        }
        // fundo bem baixo na nota do baixo, que respira junto
        padPh += twoPi * padF / sr
        if (padPh > twoPi) padPh -= twoPi
        val pad = Sine.sin(padPh) * (0.008 + 0.012 * breath)
        l += pad
        r += pad
        send += pad
    }
}
