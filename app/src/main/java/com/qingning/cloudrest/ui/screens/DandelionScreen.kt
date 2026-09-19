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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.screenBg
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class FlySeed(
    val x0: Float,
    val y0: Float,
    val vx: Float,
    val vy: Float,
    val born: Long,
    val phase: Float,
)

/** 吹蒲公英：轻轻一点，烦恼随风飘走 */
@Composable
fun DandelionScreen(onBack: () -> Unit) {
    val night = AppSettings.deepNight
    val rnd = remember { Random(System.currentTimeMillis()) }
    var t by remember { mutableStateOf(0L) }
    var gen by remember { mutableStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var head by remember { mutableStateOf(listOf<Offset>()) }
    var flying by remember { mutableStateOf(listOf<FlySeed>()) }
    var blown by remember { mutableStateOf(0) }
    var celebrated by remember { mutableStateOf(false) }
    var canvasW by remember { mutableStateOf(1080f) }
    var canvasH by remember { mutableStateOf(2000f) }

    // 生成一株蒲公英（72 颗种子，近似圆盘分布）
    LaunchedEffect(gen) {
        ready = false
        head = List(72) {
            val ang = rnd.nextFloat() * Math.PI.toFloat() * 2f
            val r = 0.55f + rnd.nextFloat() * 0.45f
            Offset(cos(ang) * r, -sin(ang) * r * 0.9f)
        }
        flying = emptyList()
        blown = 0
        ready = true
    }

    LaunchedEffect(Unit) {
        var lastClean = 0L
        while (true) withFrameNanos { now ->
            t = now
            if (now - lastClean > 400_000_000L) {
                lastClean = now
                flying = flying.filter { (now - it.born) / 1e9f < 4.6f }
                if (ready && head.isEmpty() && flying.isEmpty() && !celebrated) {
                    celebrated = true
                    SoundEngine.chime()
                }
            }
        }
    }

    fun blow(pos: Offset) {
        if (head.isEmpty()) return
        val cx = canvasW / 2f
        val cy = canvasH * 0.48f
        val hr = canvasH * 0.13f
        val picked = head
            .map { it to Offset(cx + it.x * hr, cy + it.y * hr) }
            .sortedBy { (_, p) -> (p - pos).getDistance() }
            .take(12)
        if (picked.isEmpty()) return
        val pickedSet = picked.map { it.first }.toSet()
        head = head.filter { it !in pickedSet }
        val now = System.nanoTime()
        val fresh = picked.map { (_, p) ->
            val dx = p.x - pos.x
            val dy = p.y - pos.y
            val d = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(24f)
            val sp = 150f + rnd.nextFloat() * 170f
            FlySeed(
                x0 = p.x,
                y0 = p.y,
                vx = dx / d * sp + 36f,
                vy = dy / d * sp - 46f,
                born = now,
                phase = rnd.nextFloat() * 6.28f,
            )
        }
        flying = (flying + fresh).takeLast(240)
        blown += fresh.size
        SoundEngine.tick()
    }

    Box(Modifier.fillMaxSize().background(screenBg(Color(0xFFCDE8D0), Color(0xFFE8F4D8)))) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { pos -> blow(pos) } },
        ) {
            val w = size.width
            val h = size.height
            canvasW = w
            canvasH = h
            drawRect(
                Brush.verticalGradient(
                    if (night) listOf(Color(0xFF262038), Color(0xFF2E3A34))
                    else listOf(Color(0xFFC8E6F0), Color(0xFFE8F4D8)),
                ),
            )
            val cx = w / 2f
            val cy = h * 0.48f
            val hr = h * 0.13f
            // 光晕
            drawCircle(
                Brush.radialGradient(listOf(if (night) Color(0x33EDE7F4) else Color(0x59FFFFFF), Color.Transparent)),
                radius = hr * 2.2f,
                center = Offset(cx, cy),
            )
            // 花茎
            val stem = Path().apply {
                moveTo(cx, cy + hr * 0.85f)
                cubicTo(cx + 14f, cy + hr * 2.4f, cx - 18f, h * 0.66f, cx + 4f, h * 0.76f)
            }
            drawPath(stem, Color(0xFF8FAF8A), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
            // 种子球
            val fluff = if (night) Color(0xFFEDE7F4) else Color.White
            head.forEach { u ->
                val p = Offset(cx + u.x * hr, cy + u.y * hr)
                drawLine(fluff.copy(alpha = 0.35f), Offset(cx, cy), p, strokeWidth = 1f)
                drawCircle(fluff.copy(alpha = 0.25f), radius = 6.dp.toPx(), center = p)
                drawCircle(fluff, radius = 2.2.dp.toPx(), center = p)
            }
            // 飞走的种子
            flying.forEach { s ->
                val age = (t - s.born) / 1e9f
                if (age in 0f..4.6f) {
                    val alpha = if (age > 3.6f) (4.6f - age).coerceIn(0f, 1f) else 1f
                    val x = s.x0 + s.vx * age + sin(age * 2.4f + s.phase) * 14f * age
                    val y = s.y0 + s.vy * age + 6f * age * age
                    drawLine(fluff.copy(alpha = alpha * 0.45f), Offset(x, y), Offset(x - s.vx * 0.03f, y - s.vy * 0.03f), strokeWidth = 1.6f)
                    drawCircle(fluff.copy(alpha = alpha), radius = 2.6.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        Column(Modifier.fillMaxSize().zIndex(1f)) {
            PlayHeader("吹蒲公英", "🌼", onBack)
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    celebrated -> "都飞到很远很远的地方了"
                    head.isEmpty() && flying.isNotEmpty() -> "飞呀飞，飘向远方……"
                    head.isEmpty() -> "都飞走啦 ✨"
                    else -> "轻点屏幕，吹散它（还剩 ${head.size} 颗小伞）"
                },
                fontSize = 14.sp,
                color = SubInk,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Text(
            "轻轻一点，烦恼随风飘走",
            fontSize = 12.sp,
            color = SubInk,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
        )

        if (celebrated) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF2FFFFFF))
                    .padding(horizontal = 26.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🌬️", fontSize = 34.sp)
                Spacer(Modifier.height(6.dp))
                Text("祝福已经飘向远方，共放飞 $blown 颗", fontSize = 15.sp, color = Color(0xFF4A3F4F))
                Spacer(Modifier.height(14.dp))
                Text(
                    "🌼 再来一朵",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF8FCF9E), Color(0xFFB8D88A))))
                        .bounceClickable {
                            celebrated = false
                            gen += 1
                            SoundEngine.tick()
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }
    }
}
