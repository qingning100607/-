package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.BackButton
import com.qingning.cloudrest.ui.theme.NightSkyBrush
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Spark(
    val born: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val life: Float,
    val size: Float,
)

private data class Star(val x: Float, val y: Float, val phase: Float, val speed: Float, val r: Float)

/** 烟花手账：点击/拖动放烟花，夜空里自动绽放 */
@Composable
fun FireworkScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
    val rnd = remember { Random(System.currentTimeMillis()) }
    val particlesRef = remember { mutableStateOf(listOf<Spark>()) }
    val stars = remember {
        List(70) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.7f,
                phase = Random.nextFloat() * 6.28f,
                speed = 0.6f + Random.nextFloat() * 1.8f,
                r = 1.5f + Random.nextFloat() * 2.5f,
            )
        }
    }
    var t by remember { mutableStateOf(0L) }
    var canvasW by remember { mutableStateOf(1080f) }
    var canvasH by remember { mutableStateOf(2000f) }
    var shown by remember { mutableStateOf(Store.fireworks) }

    val palette = listOf(
        Color(0xFFFFD9A3), Color(0xFFF0A8BC), Color(0xFFC9B8F0), Color(0xFFA8C8F0),
        Color(0xFFFFF3C4), Color(0xFFB5E0C8), Color(0xFFFFB37A),
    )

    fun explode(cx: Float, cy: Float, big: Boolean) {
        val now = System.nanoTime()
        val n = if (big) 38 else 10
        val new = List(n) { i ->
            val ang = (i.toFloat() / n * 2f * PI).toFloat() + rnd.nextFloat() * 0.5f - 0.25f
            val sp = (70f + rnd.nextFloat() * 240f) * density.density
            Spark(
                born = now,
                x = cx,
                y = cy,
                vx = cos(ang) * sp,
                vy = sin(ang) * sp,
                color = palette.random(rnd),
                life = 0.85f + rnd.nextFloat() * 0.75f,
                size = (3f + rnd.nextFloat() * 4f) * density.density,
            )
        }
        particlesRef.value = (particlesRef.value.filter { (now - it.born) / 1e9f < it.life } + new).takeLast(380)
        if (big) {
            shown++
            Store.fireworks = shown
            SoundEngine.tick()
        }
    }

    LaunchedEffect(Unit) {
        while (true) withFrameNanos { t = it }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2600)
            explode(rnd.nextFloat() * canvasW, canvasH * (0.10f + rnd.nextFloat() * 0.30f), big = true)
        }
    }

    var lastDrag by remember { mutableStateOf(0L) }

    Box(
        Modifier
            .fillMaxSize()
            .background(NightSkyBrush)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { explode(it.x, it.y, big = true) })
                }
                .pointerInput(Unit) {
                    detectDragGestures(onDrag = { change, _ ->
                        val now = System.nanoTime()
                        if (now - lastDrag > 55_000_000L) {
                            explode(change.position.x, change.position.y, big = false)
                            lastDrag = now
                        }
                    })
                },
        ) {
            canvasW = size.width
            canvasH = size.height
            // 星星
            stars.forEach { s ->
                val tw = 0.35f + 0.35f * sin(t / 1e9f * s.speed + s.phase)
                drawCircle(
                    Color.White.copy(alpha = 0.15f + 0.55f * tw),
                    s.r,
                    Offset(s.x * size.width, s.y * size.height),
                )
            }
            // 粒子
            particlesRef.value.forEach { p ->
                val age = (t - p.born) / 1e9f
                if (age < p.life) {
                    val k = 1f - age / p.life
                    val x = p.x + p.vx * age
                    val y = p.y + p.vy * age + 0.5f * 380f * age * age
                    val alpha = (k * k).coerceIn(0f, 1f)
                    drawCircle(p.color.copy(alpha = alpha), p.size * k, Offset(x, y))
                    // 尾迹
                    drawLine(
                        p.color.copy(alpha = alpha * 0.55f),
                        Offset(x, y),
                        Offset(x - p.vx * 0.045f, y - p.vy * 0.045f),
                        strokeWidth = p.size * 0.8f * k,
                    )
                }
            }
        }

        Row(
        Modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            BackButton(onBack)
            Spacer(Modifier.width(6.dp))
            Text("🎆 烟花手账", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        Text(
            "已绽放 $shown 朵 · 拖动可画烟花线",
            fontSize = 13.sp,
            color = Color(0xCCFFFFFF),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
        )
    }
}