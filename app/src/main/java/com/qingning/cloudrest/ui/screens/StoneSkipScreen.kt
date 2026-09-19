package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.screenBg
import kotlin.math.pow
import kotlin.random.Random

private enum class StonePhase { READY, FLY, SINK, DONE }

private data class Ripple(val x: Float, val born: Long, val strength: Int)

private class SkipCtl {
    var arcStart = 0L
    var arcDur = 1_000_000_000L
    var fromX = 0f
    var arcDx = 0f
    var arcH = 0f
    var waitingAt = -1L
    var sinkStart = 0L
    val taps = ArrayList<Long>()
}

/** 打水漂：看准落水那一下，点！ */
@Composable
fun StoneSkipScreen(onBack: () -> Unit) {
    val night = AppSettings.deepNight
    val rnd = remember { Random(System.currentTimeMillis()) }
    val ctl = remember { SkipCtl() }
    var t by remember { mutableStateOf(0L) }
    var phase by remember { mutableStateOf(StonePhase.READY) }
    var canvasW by remember { mutableStateOf(1080f) }
    var canvasH by remember { mutableStateOf(2000f) }
    var stoneX by remember { mutableStateOf(-100f) }
    var stoneY by remember { mutableStateOf(0f) }
    var stoneAlpha by remember { mutableStateOf(1f) }
    var skips by remember { mutableStateOf(0) }
    var best by remember { mutableStateOf(Store.stoneBest) }
    var ripples by remember { mutableStateOf(listOf<Ripple>()) }

    fun bounce(now: Long) {
        skips += 1
        ripples = (ripples + Ripple(stoneX, now, minOf(skips, 6))).takeLast(24)
        ctl.waitingAt = -1L
        ctl.fromX = stoneX
        var dx = canvasW * 0.30f * 0.93f.pow(skips.toFloat())
        if (stoneX + dx > canvasW * 0.96f) dx = canvasW * 0.96f - stoneX
        ctl.arcDx = dx.coerceAtLeast(canvasW * 0.07f)
        ctl.arcDur = ((1_000L - skips * 32L).coerceAtLeast(660L)) * 1_000_000L
        ctl.arcH = canvasH * (0.08f + rnd.nextFloat() * 0.04f)
        ctl.arcStart = now
        SoundEngine.tick()
        ctl.taps.removeAll { it >= now - 300_000_000L }
    }

    fun startThrow(now: Long) {
        skips = 0
        ripples = emptyList()
        stoneAlpha = 1f
        ctl.fromX = canvasW * -0.05f
        ctl.arcDx = canvasW * 0.36f
        ctl.arcH = canvasH * 0.115f
        ctl.arcDur = 1_050_000_000L
        ctl.arcStart = now
        ctl.waitingAt = -1L
        phase = StonePhase.FLY
        SoundEngine.pop()
    }

    LaunchedEffect(Unit) {
        while (true) withFrameNanos { now ->
            t = now
            val waterY = canvasH * 0.62f
            when (phase) {
                StonePhase.READY -> {
                    if (ctl.taps.isNotEmpty()) {
                        ctl.taps.clear()
                        startThrow(now)
                    }
                }
                StonePhase.FLY -> {
                    val p = ((now - ctl.arcStart).toFloat() / ctl.arcDur).coerceIn(0f, 1f)
                    stoneX = ctl.fromX + ctl.arcDx * p
                    stoneY = waterY - ctl.arcH * 4f * p * (1f - p)
                    if (ctl.waitingAt < 0 && p >= 1f) {
                        val hit = ctl.taps.any { it >= now - 240_000_000L }
                        if (hit) bounce(now) else ctl.waitingAt = now
                    }
                    if (ctl.waitingAt > 0) {
                        if (ctl.taps.any { it >= ctl.waitingAt - 240_000_000L }) {
                            bounce(now)
                        } else if (now - ctl.waitingAt > 330_000_000L) {
                            ctl.waitingAt = -1L
                            stoneY = waterY
                            phase = StonePhase.SINK
                            ctl.sinkStart = now
                            ripples = (ripples + Ripple(stoneX, now, 3)).takeLast(24)
                            SoundEngine.chime()
                        }
                    }
                }
                StonePhase.SINK -> {
                    val p = ((now - ctl.sinkStart).toFloat() / 620_000_000f).coerceIn(0f, 1f)
                    stoneY = waterY + 120f * p
                    stoneAlpha = 1f - p
                    if (p >= 1f) {
                        phase = StonePhase.DONE
                        if (skips > best) {
                            best = skips
                            Store.stoneBest = best
                        }
                    }
                }
                StonePhase.DONE -> {
                    if (ctl.taps.isNotEmpty()) {
                        ctl.taps.clear()
                        stoneAlpha = 1f
                        ripples = emptyList()
                        phase = StonePhase.READY
                    }
                }
            }
            ripples = ripples.filter { (now - it.born) / 1e9f < 1.3f }
            ctl.taps.removeAll { it < now - 1_500_000_000L }
        }
    }

    Box(Modifier.fillMaxSize().background(screenBg(Color(0xFFD6ECF6), Color(0xFFE8F0FA)))) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { ctl.taps.add(System.nanoTime()) } },
        ) {
            val w = size.width
            val h = size.height
            canvasW = w
            canvasH = h
            val waterY = h * 0.62f
            // 天空
            drawRect(
                Brush.verticalGradient(
                    if (night) listOf(Color(0xFF23203A), Color(0xFF3A3050))
                    else listOf(Color(0xFFC8E0F4), Color(0xFFF6DCC8)),
                ),
                size = Size(w, waterY),
            )
            drawCircle(if (night) Color(0xFFEDE7F4) else Color(0xFFFFF0C8), w * 0.06f, Offset(w * 0.78f, waterY * 0.38f))
            // 远山
            val hill = Path().apply {
                moveTo(0f, waterY * 0.95f)
                cubicTo(w * 0.25f, waterY * 0.72f, w * 0.55f, waterY * 0.78f, w * 0.72f, waterY * 0.9f)
                cubicTo(w * 0.84f, waterY * 0.97f, w * 0.94f, waterY * 0.94f, w, waterY * 0.9f)
                lineTo(w, waterY)
                lineTo(0f, waterY)
                close()
            }
            drawPath(hill, if (night) Color(0xFF34304E) else Color(0x80908CC8))
            // 水面
            drawRect(
                Brush.verticalGradient(
                    if (night) listOf(Color(0xFF28324E), Color(0xFF1A2236))
                    else listOf(Color(0xFF8FC4E0), Color(0xFF5E98C8)),
                ),
                topLeft = Offset(0f, waterY),
                size = Size(w, h - waterY),
            )
            // 水面光纹
            for (i in 0..4) {
                val yy = waterY + (h - waterY) * (0.12f + i * 0.18f)
                drawLine(
                    Color.White.copy(alpha = 0.13f),
                    Offset(w * (0.10f + i * 0.05f), yy),
                    Offset(w * (0.52f + i * 0.06f), yy),
                    strokeWidth = 2f,
                )
            }
            // 涟漪
            ripples.forEach { rp ->
                val age = (t - rp.born) / 1e9f
                if (age in 0f..1.3f) {
                    val k = age / 1.3f
                    val maxR = (34f + 16f * rp.strength) * (0.3f + 0.7f * k)
                    val a = (1f - k) * 0.55f
                    drawCircle(Color.White.copy(alpha = a), radius = maxR, center = Offset(rp.x, waterY), style = Stroke(2.2f))
                    drawCircle(Color.White.copy(alpha = a * 0.6f), radius = maxR * 0.55f, center = Offset(rp.x, waterY), style = Stroke(1.6f))
                }
            }
            // 石片
            if (phase == StonePhase.FLY || phase == StonePhase.SINK) {
                val sw = 12.dp.toPx()
                val sh = 5.5.dp.toPx()
                drawOval(
                    (if (night) Color(0xFF9AA0B8) else Color(0xFF6E6A78)).copy(alpha = stoneAlpha),
                    topLeft = Offset(stoneX - sw / 2f, stoneY - sh / 2f),
                    size = Size(sw, sh),
                )
            }
        }

        Column(Modifier.fillMaxSize().zIndex(1f)) {
            PlayHeader("打水漂", "🌊", onBack)
            Spacer(Modifier.height(2.dp))
            Text(
                "连续 $skips 跳 · 最佳 $best 跳",
                fontSize = 14.sp,
                color = SubInk,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Text(
            when (phase) {
                StonePhase.READY -> "轻点屏幕，把石片甩出去 🪨"
                StonePhase.FLY -> if (skips == 0) "看准落水那一下，点！" else "接住它，再来一跳！"
                StonePhase.SINK -> "咚——沉底了"
                StonePhase.DONE -> "重新来一发吧"
            },
            fontSize = 12.sp,
            color = SubInk,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
        )

        if (phase == StonePhase.DONE) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF2FFFFFF))
                    .padding(horizontal = 26.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🪨", fontSize = 32.sp)
                Spacer(Modifier.height(6.dp))
                Text("这次跳过 $skips 跳！", fontSize = 16.sp, color = Color(0xFF4A3F4F))
                Spacer(Modifier.height(4.dp))
                Text("历史最佳 $best 跳", fontSize = 12.sp, color = Color(0xFF8A7E90))
                Spacer(Modifier.height(14.dp))
                Text(
                    "再来一次 ↻",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF6FB8C8), Color(0xFF9A8FD8))))
                        .bounceClickable {
                            ctl.taps.clear()
                            phase = StonePhase.READY
                            stoneAlpha = 1f
                            ripples = emptyList()
                            SoundEngine.tick()
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }
    }
}
