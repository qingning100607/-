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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class RowSpec(
    val text: String,
    val color: Color,
    val sizeSp: Float,
    val speedDp: Float,
    val yFrac: Float,
    val phase: Float,
)

private data class Burst(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val text: String,
    val born: Long,
)

/** Ciallo 电台：原站滚动文字秀的原生复刻 + 点击爆发 */
@Composable
fun CialloScreen() {
    val bgTop = if (com.qingning.cloudrest.ui.AppSettings.deepNight) Color(0xFF1F1A30) else Color(0xFFFFF9F2)
    val bgBottom = if (com.qingning.cloudrest.ui.AppSettings.deepNight) Color(0xFF2C2340) else Color(0xFFF6ECF6)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgTop, bgBottom)))) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        var t by remember { mutableStateOf(0L) }
        var bursts by remember { mutableStateOf(listOf<Burst>()) }
        var nextId by remember { mutableStateOf(0L) }
        val rnd = remember { Random(System.currentTimeMillis()) }

        val rows = remember {
            val colors = listOf(
                Color(0xFFE86A6A), Color(0xFF4FB8C8), Color(0xFF9BD56B), Color(0xFFF08BAF),
                Color(0xFF6E9FE8), Color(0xFFF2C94C), Color(0xFFE8954F), Color(0xFF8A7E90),
            )
            List(13) { i ->
                RowSpec(
                    text = if (i % 2 == 0) "Ciallo～(∠・ω< )⌒★" else "Ciallo～(∠・ω< )⌒☆",
                    color = colors[i % colors.size],
                    sizeSp = 26f + (i * 17) % 34,
                    speedDp = 80f + (i * 53) % 170,
                    yFrac = 0.05f + i * 0.072f,
                    phase = (i * 0.373f) % 1f,
                )
            }
        }

        // 13 行文字只在首帧排版一次，滚动时直接复用
        val rowLayouts = remember(rows) {
            rows.map { r ->
                textMeasurer.measure(
                    r.text,
                    TextStyle(fontSize = r.sizeSp.sp, color = r.color, fontWeight = FontWeight.Bold),
                )
            }
        }
        val burstLayouts = remember { HashMap<String, TextLayoutResult>() }

        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { t = it }
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { pos ->
                        val cols = listOf(
                            Color(0xFFE86A6A), Color(0xFF4FB8C8), Color(0xFF9BD56B), Color(0xFFF08BAF),
                            Color(0xFF6E9FE8), Color(0xFFF2C94C),
                        )
                        val txts = listOf("Ciallo～(∠・ω< )⌒★", "Ciallo～(∠・ω< )⌒☆")
                        val fresh = (0 until 8).map { i ->
                            val ang = (i.toFloat() / 8f * 2f * PI).toFloat() + rnd.nextFloat() * 0.6f - 0.3f
                            val sp = (140f + rnd.nextFloat() * 160f) * density.density
                            Burst(
                                id = nextId + i,
                                x = pos.x,
                                y = pos.y,
                                vx = cos(ang) * sp,
                                vy = sin(ang) * sp,
                                color = cols.random(rnd),
                                text = txts.random(rnd),
                                born = t,
                            )
                        }
                        nextId += 8
                        bursts = (bursts.filter { (t - it.born) / 1e9f < 1.4f } + fresh).takeLast(150)
                        SoundEngine.ciallo()
                    })
                },
        ) {
            val w = size.width
            val h = size.height
            for (i in rows.indices) {
                val r = rows[i]
                val layout = rowLayouts[i]
                val period = w + layout.size.width + 60f
                val speedPx = r.speedDp * density.density
                val secs = t / 1e9f
                val x = w - ((secs * speedPx + r.phase * period) % period)
                drawText(layout, topLeft = Offset(x, r.yFrac * h))
            }
            bursts.forEach { b ->
                val age = (t - b.born) / 1e9f
                if (age in 0f..1.4f) {
                    val x = b.x + b.vx * age
                    val y = b.y + b.vy * age + 0.5f * 420f * age * age
                    val alpha = (1f - age / 1.4f).coerceIn(0f, 1f)
                    val layout = burstLayouts.getOrPut(b.text) {
                        textMeasurer.measure(
                            b.text,
                            TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    drawText(layout, color = b.color, topLeft = Offset(x, y), alpha = alpha)
                }
            }
            val fade = 60f
            drawRect(Brush.horizontalGradient(listOf(bgTop, Color.Transparent), startX = 0f, endX = fade))
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, bgTop), startX = w - fade, endX = w))
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Ciallo 电台", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text("Ciallo～(∠・ω< )⌒☆", fontSize = 12.sp, color = SubInk)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("点屏幕，放一串 Ciallo～（每次随机一种口味）", fontSize = 13.sp, color = SubInk)
        }
    }
}