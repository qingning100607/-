package com.qingning.cloudrest.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import com.qingning.cloudrest.data.Prefs
import com.qingning.cloudrest.ui.AppSettings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundType(val emoji: String, val label: String, val asset: String) {
    RAIN("🌧", "雨声", "sounds/rain.ogg"),
    OCEAN("🌊", "海浪", "sounds/ocean.ogg"),
    FOREST("🌲", "林间风", "sounds/forest.ogg"),
    FIRE("🔥", "篝火", "sounds/fire.ogg"),
    WHITE("🌫", "白噪音", "sounds/white.ogg"),
    STREAM("💧", "溪流", "sounds/stream.ogg"),
    NIGHT("🦗", "夜虫", "sounds/night.ogg"),
    CAFE("☕", "咖啡馆", "sounds/cafe.ogg"),
    SNOW("❄️", "雪落", "sounds/snow.ogg"),
}

/**
 * 自然声：真实录音素材（assets 内置，MediaPlayer 循环播放）。
 * 敲击/提示音效：真实采样（SoundPool 低延迟播放），未加载时回退实时合成。
 */
object SoundEngine {

    private const val RATE = 22050
    private var rnd = Random(System.currentTimeMillis())

    private var appContext: Context? = null

    // ---------------- 短音效采样（SoundPool） ----------------

    private var sp: SoundPool? = null
    private val sfxIds = ConcurrentHashMap<String, Int>()
    private val sfxLoaded = ConcurrentHashMap<String, Boolean>()
    private val sfxAfd = CopyOnWriteArrayList<AssetFileDescriptor>()
    private val idToKey = ConcurrentHashMap<Int, String>()
    private var muyuCombo = 0
    private var lastMuyuAt = 0L
    private val cialloKeys = listOf("ciallo_1", "ciallo_2", "ciallo_3")
    private var lastCiallo = ""

    private val SFX_FILES = listOf(
        "muyu_crisp" to "sounds/muyu.ogg",
        "muyu_warm" to "sounds/muyu_warm.ogg",
        "muyu_soft" to "sounds/muyu_soft.ogg",
        "pop" to "sounds/pop.ogg",
        "crack" to "sounds/crack.ogg",
        "shred" to "sounds/shred.ogg",
        "chime" to "sounds/chime.ogg",
        "tick" to "sounds/tick.ogg",
        "ciallo_1" to "sounds/ciallo_1.ogg",
        "ciallo_2" to "sounds/ciallo_2.ogg",
        "ciallo_3" to "sounds/ciallo_3.ogg",
    )

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        loadChannelGains(ctx)
        try {
            val pool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                val key = idToKey[sampleId]
                if (status == 0 && key != null) {
                    sfxLoaded[key] = true
                    if (key == "muyu_crisp") {
                        try {
                            pool.play(sampleId, 0.004f, 0.004f, 0, 0, 1f)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            SFX_FILES.forEach { (key, path) ->
                try {
                    val afd = ctx.assets.openFd(path)
                    sfxAfd.add(afd)
                    val id = pool.load(afd, 1)
                    sfxIds[key] = id
                    idToKey[id] = key
                } catch (_: Exception) {
                }
            }
            sp = pool
        } catch (_: Exception) {
        }
    }

    /** 预热音频通路：进入页面时先“空放”一下，消除首次点击的声音延迟 */
    fun warm() {
        playSample("muyu_crisp", 0.004f)
        // 合成通道同时预热：写入一小段静音，让 AudioTrack 提前进入播放态
        play(FloatArray(360))
    }

    /** 播放 SoundPool 采样；未加载或失败返回 false（调用方回退到合成音） */
    private fun playSample(key: String, vol: Float, rate: Float = 1f): Boolean {
        val pool = sp ?: return false
        val id = sfxIds[key] ?: return false
        if (sfxLoaded[key] != true) return false
        return try {
            val g = (vol * AppSettings.sfxVolume).coerceIn(0f, 1f) * masterVolume()
            pool.play(id, g, g, 1, 0, rate) != 0
        } catch (_: Exception) {
            false
        }
    }

    // ---------------- 自然声（MediaPlayer 循环池） ----------------

    private val players = ConcurrentHashMap<SoundType, MediaPlayer>()

    val channelOn = SoundType.entries.associateWith { false }.toMutableMap()
    val channelGain = SoundType.entries.associateWith { 0.7f }.toMutableMap()

    /** 定时关闭截止时间戳，0 = 不启用；最后 60 秒自动渐弱 */
    @Volatile var fadeDeadline = 0L

    fun scheduleStopAfter(minutes: Int) {
        fadeDeadline = if (minutes <= 0) 0L else System.currentTimeMillis() + minutes * 60_000L
    }

    fun anyOn(): Boolean = channelOn.values.any { it }

    /** 当前播放中的声道名（通知栏展示用） */
    fun activeLabels(): List<String> = channelOn.filter { it.value }.keys.map { it.label }

    fun toggle(ch: SoundType) {
        val nowOn = !(channelOn[ch] ?: false)
        channelOn[ch] = nowOn
        if (nowOn) {
            rnd = Random(System.currentTimeMillis())
            try {
                ensurePlayer(ch)?.start()
            } catch (_: Exception) {
            }
            startLoop()
            tick()
        } else {
            try {
                players[ch]?.pause()
            } catch (_: Exception) {
            }
        }
        appContext?.let { SoundService.update(it) }
    }

    fun setGain(ch: SoundType, g: Float) {
        channelGain[ch] = g.coerceIn(0f, 1f)
        saveChannelGain(ch)
        applyVolume(ch)
    }

    /** 上次会话保存的声道音量（音量记忆） */
    private fun loadChannelGains(ctx: Context) {
        val p = ctx.getSharedPreferences("cloud_rest_channels", Context.MODE_PRIVATE)
        SoundType.entries.forEach { ch ->
            channelGain[ch] = Prefs.getFloat(p, "g_" + ch.name, 0.7f)
        }
    }

    private fun saveChannelGain(ch: SoundType) {
        appContext?.getSharedPreferences("cloud_rest_channels", Context.MODE_PRIVATE)
            ?.edit()?.putFloat("g_" + ch.name, channelGain[ch] ?: 0.7f)?.apply()
    }

    /** 一键音景：按给定“声道→音量”表整体切换，未列出的全部关闭 */
    fun applyScene(spec: Map<SoundType, Float>) {
        rnd = Random(System.currentTimeMillis())
        SoundType.entries.forEach { ch ->
            val want = spec[ch]
            val was = channelOn[ch] ?: false
            if (want != null) {
                channelGain[ch] = want.coerceIn(0f, 1f)
                saveChannelGain(ch)
                if (!was) {
                    channelOn[ch] = true
                    try {
                        ensurePlayer(ch)?.start()
                    } catch (_: Exception) {
                    }
                }
                applyVolume(ch)
            } else if (was) {
                channelOn[ch] = false
                try {
                    players[ch]?.pause()
                } catch (_: Exception) {
                }
            }
        }
        startLoop()
        tick()
        appContext?.let { SoundService.update(it) }
    }

    /** 当前混音 → 预设字符串（"RAIN:0.80,FIRE:0.50"） */
    fun encodeScene(): String =
        channelOn.filter { it.value }
            .map { (t, _) -> t.name + ":" + java.lang.String.format(java.util.Locale.US, "%.2f", channelGain[t] ?: 0.7f) }
            .joinToString(",")

    /** 预设字符串 → 声道音量表 */
    fun decodeScene(spec: String): Map<SoundType, Float> {
        val m = HashMap<SoundType, Float>()
        spec.split(",").forEach { part ->
            val i = part.indexOf(':')
            if (i <= 0) return@forEach
            val v = part.substring(i + 1).toFloatOrNull() ?: return@forEach
            SoundType.entries.firstOrNull { it.name == part.substring(0, i) }?.let { t -> m[t] = v }
        }
        return m
    }

    /** 当前主音量（含定时渐弱） */
    private fun masterVolume(): Float {
        if (fadeDeadline <= 0L) return 1f
        val remain = fadeDeadline - System.currentTimeMillis()
        return when {
            remain <= 0L -> 0f
            remain < 60_000L -> (remain / 60_000f).coerceIn(0f, 1f)
            else -> 1f
        }
    }

    private fun applyVolume(ch: SoundType) {
        val p = players[ch] ?: return
        val g = (channelGain[ch] ?: 0.7f) * masterVolume()
        try {
            p.setVolume(g, g)
        } catch (_: Exception) {
        }
    }

    private fun ensurePlayer(ch: SoundType): MediaPlayer? {
        players[ch]?.let { return it }
        val ctx = appContext ?: return null
        return try {
            val afd = ctx.assets.openFd(ch.asset)
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            p.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            p.isLooping = true
            val g = (channelGain[ch] ?: 0.7f) * masterVolume()
            p.setVolume(g, g)
            p.prepare()
            players[ch] = p
            p
        } catch (_: Exception) {
            null
        }
    }

    fun stopAll() {
        SoundType.entries.forEach {
            channelOn[it] = false
            try {
                players[it]?.pause()
            } catch (_: Exception) {
            }
        }
        fadeDeadline = 0L
        appContext?.let { SoundService.update(it) }
    }

    // ---------------- 短音效（实时合成） ----------------

    private val fx = CopyOnWriteArrayList<Fx>()

    private class Fx(val data: FloatArray, var pos: Int = 0)

    private fun play(d: FloatArray) {
        fx.add(Fx(d))
        startLoop()
    }

    fun tick() {
        if (!playSample("tick", 0.5f)) play(makeClick(0.03f))
    }

    fun pop() {
        if (!playSample("pop", 0.9f, 0.94f + rnd.nextFloat() * 0.12f)) play(makePop())
    }

    fun muyu() {
        val tone = when (AppSettings.muyuTone) {
            "warm" -> "muyu_warm"
            "soft" -> "muyu_soft"
            else -> "muyu_crisp"
        }
        val now = System.currentTimeMillis()
        muyuCombo = if (now - lastMuyuAt < 620L) (muyuCombo + 1).coerceAtMost(10) else 0
        lastMuyuAt = now
        val rate = 1f + muyuCombo * 0.006f
        if (!playSample(tone, 0.9f, rate)) play(makeMuyu())
    }

    fun crack() {
        if (!playSample("crack", 0.9f)) play(makeCrack())
    }

    fun shred() {
        if (!playSample("shred", 0.85f)) play(makeShred())
    }

    fun chime() {
        if (!playSample("chime", 0.8f)) play(makeChime())
    }

    /** Ciallo 语音：三种口味随机播放，避免连续重复同一口味 */
    fun ciallo() {
        var k = cialloKeys[rnd.nextInt(cialloKeys.size)]
        if (k == lastCiallo) k = cialloKeys[(cialloKeys.indexOf(k) + 1) % cialloKeys.size]
        lastCiallo = k
        if (!playSample(k, 0.95f, 0.98f + rnd.nextFloat() * 0.04f)) play(makePop())
    }

    private fun makePop(): FloatArray {
        val dur = 0.045f
        val n = (dur * RATE).toInt()
        val d = FloatArray(n)
        var ph = 0f
        val f0 = 460f
        val f1 = 130f
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            val f = f0 + (f1 - f0) * (t / dur)
            ph += 2f * PI.toFloat() * f / RATE
            val env = exp(-t * 60f)
            d[i] = sin(ph) * env * 0.8f + (rnd.nextFloat() * 2f - 1f) * env * 0.5f
        }
        return d
    }

    private fun makeMuyu(): FloatArray {
        val dur = 0.16f
        val n = (dur * RATE).toInt()
        val d = FloatArray(n)
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            val env = exp(-t * 30f)
            val v = sin(2f * PI.toFloat() * 175f * t) * 0.7f + sin(2f * PI.toFloat() * 320f * t) * 0.28f
            d[i] = v * env + (rnd.nextFloat() * 2f - 1f) * exp(-t * 160f) * 0.4f
        }
        return d
    }

    private fun makeCrack(): FloatArray {
        val dur = 0.05f
        val n = (dur * RATE).toInt()
        val d = FloatArray(n)
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            d[i] = (rnd.nextFloat() * 2f - 1f) * exp(-t * 120f) * 0.6f
        }
        return d
    }

    private fun makeShred(): FloatArray {
        val dur = 0.55f
        val n = (dur * RATE).toInt()
        val d = FloatArray(n)
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            val chop = if ((t * 30f).toInt() % 2 == 0) 1f else 0.35f
            d[i] = (rnd.nextFloat() * 2f - 1f) * chop * exp(-t * 2.2f) * 0.7f
        }
        return d
    }

    private fun makeChime(): FloatArray {
        val dur = 0.7f
        val n = (dur * RATE).toInt()
        val d = FloatArray(n)
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            val v = sin(2f * PI.toFloat() * 660f * t) * exp(-t * 4f) +
                sin(2f * PI.toFloat() * 990f * t) * exp(-t * 6f) * 0.5f
            d[i] = v * 0.5f
        }
        return d
    }

    private fun makeClick(dur: Float): FloatArray {
        val n = (dur * RATE).toInt()
        val d = FloatArray(n)
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            d[i] = (rnd.nextFloat() * 2f - 1f) * exp(-t * 200f) * 0.5f
        }
        return d
    }

    // ---------------- 输出线程（短音效 + 定时渐弱巡查） ----------------

    private var track: AudioTrack? = null
    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile private var trackStarted = false

    private fun ensureTrack() {
        if (track != null) return
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, 8192))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun startLoop() {
        if (running) return
        running = true
        thread = Thread {
            ensureTrack()
            val n = 1024
            val buf = FloatArray(n)
            val pcm = ShortArray(n)
            var lastFadeApply = 0L
            while (running) {
                // 定时渐弱：约每 120ms 刷新一次各声道音量
                val now = System.currentTimeMillis()
                if (fadeDeadline > 0L && now - lastFadeApply > 120) {
                    lastFadeApply = now
                    if (fadeDeadline - now <= 0L) {
                        stopAll()
                    } else {
                        SoundType.entries.forEach { applyVolume(it) }
                    }
                }
                if (fx.isEmpty()) {
                    try {
                        Thread.sleep(40)
                    } catch (_: InterruptedException) {
                    }
                    continue
                }
                fill(buf, n)
                if (!trackStarted) {
                    try {
                        track?.play()
                        trackStarted = true
                    } catch (_: Exception) {
                    }
                }
                for (i in 0 until n) pcm[i] = (buf[i] * 32767f).toInt().toShort()
                try {
                    track?.write(pcm, 0, n)
                } catch (_: Exception) {
                }
            }
        }.apply {
            name = "cloud-sound"
            isDaemon = true
            start()
        }
    }

    private fun fill(buf: FloatArray, n: Int) {
        for (i in 0 until n) {
            var v = 0f
            for (f in fx) {
                if (f.pos < f.data.size) {
                    v += f.data[f.pos]
                    f.pos++
                }
            }
            buf[i] = v.coerceIn(-1f, 1f)
        }
        if (fx.isNotEmpty()) fx.removeAll { it.pos >= it.data.size }
    }

    fun release() {
        running = false
        try {
            thread?.join(800)
        } catch (_: Exception) {
        }
        players.values.forEach {
            try {
                it.release()
            } catch (_: Exception) {
            }
        }
        players.clear()
        try {
            sp?.release()
        } catch (_: Exception) {
        }
        sp = null
        sfxIds.clear()
        sfxLoaded.clear()
        idToKey.clear()
        sfxAfd.forEach {
            try {
                it.close()
            } catch (_: Exception) {
            }
        }
        sfxAfd.clear()
        try {
            track?.stop()
        } catch (_: Exception) {
        }
        try {
            track?.release()
        } catch (_: Exception) {
        }
        track = null
        trackStarted = false
    }
}