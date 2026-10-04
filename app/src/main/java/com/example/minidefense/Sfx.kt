package com.example.minidefense

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object Sfx {
    private val pool = mutableMapOf<String, AudioTrack>()

    private fun gen(f0: Float, f1: Float, secs: Float, noise: Boolean = false, vol: Float = 0.5f): AudioTrack {
        val rate = 22050
        val n = (rate * secs).toInt()
        val data = ByteArray(n * 2)
        var phase = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val f = f0 + (f1 - f0) * t
            phase += f / rate
            val v = if (noise) sin(phase * 2 * PI).toFloat() * 0.4f + (Random.nextFloat() * 2 - 1) * 0.6f
            else sin(phase * 2 * PI).toFloat()
            val s = (v * (1f - t) * vol).coerceIn(-1f, 1f)
            val q = (s * 32767).toInt()
            data[i * 2] = (q and 0xFF).toByte()
            data[i * 2 + 1] = ((q shr 8) and 0xFF).toByte()
        }
        val at = AudioTrack(
            AudioManager.STREAM_MUSIC, rate,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
            n * 2, AudioTrack.MODE_STATIC
        )
        at.write(data, 0, n * 2)
        return at
    }

    fun init() {
        if (pool.isNotEmpty()) return
        pool["shoot"] = gen(900f, 500f, 0.07f, vol = 0.25f)
        pool["pop"] = gen(300f, 80f, 0.15f, noise = true)
        pool["place"] = gen(200f, 420f, 0.12f)
        pool["leak"] = gen(400f, 100f, 0.3f)
        pool["wave"] = gen(500f, 750f, 0.18f, vol = 0.35f)
        pool["win"] = gen(523f, 1046f, 0.5f, vol = 0.4f)
        pool["lose"] = gen(300f, 60f, 0.6f, vol = 0.4f)
    }

    fun play(name: String) {
        val at = pool[name] ?: return
        try {
            if (at.playState == AudioTrack.PLAYSTATE_PLAYING) at.stop()
            at.reloadStaticData()
            at.play()
        } catch (_: Exception) { }
    }
}
