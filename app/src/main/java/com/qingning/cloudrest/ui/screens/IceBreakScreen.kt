package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.theme.SubInk
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Shard(
    val pts: List<Offset>,
    val vx: Float,
    val vy: Float,
    val rot: Float,
    val vr: Float,
)

private fun genVerts(rnd: Random): List<Offset> =
    List(8) { i ->
        val ang = (i / 8f * 2f * PI).toFloat() + (rnd.nextFloat() - 0.5f) * 0.45f
        val rad = 0.80f + rnd.nextFloat() * 0.40f
        Offset(cos(ang) * rad, sin(ang) * rad)
    }

private fun genCracks(rnd: Random, verts: List<Offset>): List<Path> =
    List(8) {
        val n = verts.size
        val a = verts[it % n]
        val b = verts[(it + 1) % n]
        val t0 = 0.2f + rnd.nextFloat() * 0.6f
        val start = Offset(a.x + (b.x - a.x) * t0, a.y + (b.y - a.y) * t0)
        val mid = Offset(start.x * (0.35f + rnd.nextFloat() * 0.3f), start.y * (0.35f + rnd.nextFloat() * 0.3f))
        val end = Offset(start.x * 0.15f, start.y * 0.15f)
        Path().apply {
            moveTo(start.x, start.y)
            lineTo(mid.x, mid.y)
            lineTo(end.x, end.y)
        }
    }

/** 敲冰块：六连击敲碎烦恼 */
@Composable
fun IceBreakScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val rnd = remember { Random(System.currentTimeMillis()) }
    var iceGen by remember { mutableStateOf(0) }
    var hits by remember { mutableStateOf(0) }
    var shatterAt by remember { mutableStateOf(0L) }
    var shards by remember { mutableStateOf(listOf<Shard>()) }
    var brokenCount by remember { mutableStateOf(Store.iceBroken) }
    var t by remember { mutableStateOf(0L) }

    val verts = remember(iceGen) { genVerts(rnd) }
    val cracks = remember(iceGen) { genCracks(rnd, verts) }
    val bubbles = remember(iceGen) {
        List(3) { Offset((rnd.nextFloat() - 0.5f) * 0.8f, (rnd.nextFloat() - 0.5f) * 0.7f) }
    }

    LaunchedEffect(Unit) {
        while (true) withFrameNanos { t = it }
    }

    LaunchedEffect(shatterAt) {
        if (shatterAt > 0L) {
            delay(1150)
            hits = 0
            shatterAt = 0L
            shards = emptyList()
            iceGen++
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(com.qingning.cloudrest.ui.theme.screenBg(Color(0xFFE3F0FB), Color(0xFFF2ECF9)))
    ) {
        // 注意：Canvas 在下方且全屏捕获手势，这里必须抬高层级，保证返回键可点
        Column(Modifier.fillMaxSize().zIndex(1f)) {
            PlayHeader("敲冰块", "🧊", onBack)
            Text(
                if (shatterAt > 0L) "碎了！烦恼清空 ✨" else "再敲 ${6 - hits} 下就能敲碎它",
                fontSize = 14.sp,
                color = SubInk,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (shatterAt > 0L) return@detectTapGestures
                        val now = System.nanoTime()
                        hits++
                        SoundEngine.crack()
                        if (AppSettings.hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (hits >= 6) {
                            shatterAt = now
                            val n = verts.size
                            shards = List(n) { i ->
                                val a = verts[i % n]
                                val b = verts[(i + 1) % n]
                                val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                val ang = kotlin.math.atan2(mid.y, mid.x)
                                val sp = (180f + rnd.nextFloat() * 200f)
                                Shard(
                                    pts = listOf(Offset.Zero, a, b),
                                    vx = cos(ang) * sp,
                                    vy = sin(ang) * sp - (80f + rnd.nextFloat() * 120f),
                                    rot = rnd.nextFloat() * 6.28f,
                                    vr = (rnd.nextFloat() - 0.5f) * 9f,
                                )
                            }.also {
                                brokenCount++
                                Store.iceBroken = brokenCount
                                SoundEngine.chime()
                            }
                        }
                    }
                },
        ) {
            val scale = size.width * 0.30f
            val cx = size.width / 2f
            val cy = size.height * 0.56f

            fun toAbs(p: Offset) = Offset(cx + p.x * scale, cy + p.y * scale)

            if (shatterAt == 0L) {
                // 冰影
                val shadow = Path().apply {
                    verts.forEachIndexed { i, p ->
                        val q = toAbs(p)
                        if (i == 0) moveTo(q.x + 10f, q.y + 14f) else lineTo(q.x + 10f, q.y + 14f)
                    }
                    close()
                }
                drawPath(shadow, Color(0x1F4A6B8F))

                // 冰体
                val body = Path().apply {
                    verts.forEachIndexed { i, p ->
                        val q = toAbs(p)
                        if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y)
                    }
                    close()
                }
                drawPath(
                    body,
                    Brush.linearGradient(
                        listOf(Color(0xE8DCEFFB), Color(0xB0C8E6F8)),
                        start = Offset(cx - scale, cy - scale),
                        end = Offset(cx + scale, cy + scale),
                    ),
                )
                drawPath(body, Color(0x80FFFFFF), style = Stroke(width = 3f))
                // 高光
                drawLine(
                    Color(0x99FFFFFF),
                    toAbs(Offset(-0.5f, -0.55f)),
                    toAbs(Offset(-0.15f, -0.25f)),
                    strokeWidth = 6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                // 气泡
                bubbles.forEach { b ->
                    drawCircle(Color(0x59FFFFFF), 6f, toAbs(b))
                }
                // 裂纹
                for (ci in 0 until hits.coerceAtMost(cracks.size)) {
                    drawPath(cracks[ci], Color(0xB0FFFFFF), style = Stroke(width = 2.2f))
                }
            } else {
                val age = (t - shatterAt) / 1e9f
                val fade = (1f - age / 1.0f).coerceIn(0f, 1f)
                shards.forEach { s ->
                    val dx = s.vx * age
                    val dy = s.vy * age + 0.5f * 560f * age * age
                    val rad = s.rot + s.vr * age
                    val cosR = cos(rad)
                    val sinR = sin(rad)
                    val p = Path().apply {
                        s.pts.forEachIndexed { i, pt ->
                            val rx = pt.x * cosR - pt.y * sinR
                            val ry = pt.x * sinR + pt.y * cosR
                            val qx = cx + rx * scale + dx
                            val qy = cy + ry * scale + dy
                            if (i == 0) moveTo(qx, qy) else lineTo(qx, qy)
                        }
                        close()
                    }
                    drawPath(p, Color(0xFFC8E6F8).copy(alpha = fade))
                    drawPath(p, Color.White.copy(alpha = fade * 0.7f), style = Stroke(width = 2f))
                }
            }
        }

        Text(
            "咔啦一声，压力碎掉",
            fontSize = 12.sp,
            color = SubInk,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
        )
    }
}