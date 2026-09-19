package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.screenBg
import kotlin.random.Random

/** 雾窗画：擦一擦雾气，看看窗外 */
@Composable
fun FogWindowScreen(onBack: () -> Unit) {
    val night = AppSettings.deepNight
    var t by remember { mutableStateOf(0L) }
    var wipes by remember { mutableStateOf(listOf<Offset>()) }
    var done by remember { mutableStateOf(false) }
    var canvasW by remember { mutableStateOf(1080f) }
    var canvasH by remember { mutableStateOf(2000f) }
    val dots = remember { List(46) { Triple(Random.nextFloat(), Random.nextFloat(), 0.04f + Random.nextFloat() * 0.08f) } }

    // 覆盖率网格：擦亮约 80% 才结算
    val gw = 24
    val gh = 54
    val grid = remember { BooleanArray(gw * gh) }
    var cleared by remember { mutableStateOf(0) }
    val wipePx = with(LocalDensity.current) { 44.dp.toPx() }
    val percent = cleared * 100 / (gw * gh)

    fun mark(p: Offset) {
        if (canvasW <= 0f || canvasH <= 0f) return
        val cellW = canvasW / gw
        val cellH = canvasH / gh
        val rad = wipePx * 0.95f
        val minX = ((p.x - rad) / cellW).toInt().coerceIn(0, gw - 1)
        val maxX = ((p.x + rad) / cellW).toInt().coerceIn(0, gw - 1)
        val minY = ((p.y - rad) / cellH).toInt().coerceIn(0, gh - 1)
        val maxY = ((p.y + rad) / cellH).toInt().coerceIn(0, gh - 1)
        var add = 0
        for (gy in minY..maxY) {
            for (gx in minX..maxX) {
                val cell = gy * gw + gx
                if (!grid[cell]) {
                    val ddx = (gx + 0.5f) * cellW - p.x
                    val ddy = (gy + 0.5f) * cellH - p.y
                    if (ddx * ddx + ddy * ddy <= rad * rad) {
                        grid[cell] = true
                        add++
                    }
                }
            }
        }
        if (add > 0) cleared += add
    }

    LaunchedEffect(Unit) { while (true) withFrameNanos { t = it } }
    LaunchedEffect(cleared) {
        if (!done && cleared >= (gw * gh * 4) / 5) {
            done = true
            SoundEngine.chime()
        }
    }

    Box(Modifier.fillMaxSize().background(screenBg(Color(0xFFEAF2FB), Color(0xFFF6F0FA)))) {
        // ===== 窗外风景（底层） =====
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val skyTop = if (night) Color(0xFF262038) else Color(0xFFAEC6EE)
            val skyMid = if (night) Color(0xFF4A3A60) else Color(0xFFF2C9DA)
            val skyBot = if (night) Color(0xFF6A4A72) else Color(0xFFFBE3C8)
            drawRect(Brush.verticalGradient(listOf(skyTop, skyMid, skyBot)))
            drawCircle(
                Brush.radialGradient(listOf(if (night) Color(0x59EDE7F4) else Color(0x66FFE8C8), Color.Transparent)),
                radius = w * 0.55f,
                center = Offset(w * 0.72f, h * 0.34f),
            )
            drawCircle(if (night) Color(0xFFEDE7F4) else Color(0xFFFFF3D6), w * 0.075f, Offset(w * 0.72f, h * 0.34f))
            // 飘动的云
            val secs = t / 1e9f
            val cloudCol = if (night) Color(0x59A79CBC) else Color(0xB3FFFFFF)
            for (i in 0..2) {
                val cw = w * (0.30f + i * 0.07f)
                val x = ((secs * (6f + i * 4f) + i * 0.37f * w) % (w + cw)) - cw
                val y = h * (0.15f + i * 0.09f)
                drawOval(cloudCol, topLeft = Offset(x, y), size = Size(cw, cw * 0.22f))
                drawOval(cloudCol, topLeft = Offset(x + cw * 0.18f, y - cw * 0.08f), size = Size(cw * 0.5f, cw * 0.18f))
            }
            // 远山
            val hill1 = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.22f, h * 0.62f, w * 0.48f, h * 0.66f, w * 0.68f, h * 0.77f)
                cubicTo(w * 0.82f, h * 0.84f, w * 0.92f, h * 0.76f, w, h * 0.72f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hill1, if (night) Color(0xFF3A3158) else Color(0x8C9A8FC8))
            val hill2 = Path().apply {
                moveTo(0f, h * 0.86f)
                cubicTo(w * 0.3f, h * 0.74f, w * 0.62f, h * 0.82f, w * 0.8f, h * 0.88f)
                cubicTo(w * 0.9f, h * 0.92f, w * 0.96f, h * 0.9f, w, h * 0.88f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hill2, if (night) Color(0xFF2E2748) else Color(0x99B8A8D8))
        }

        // ===== 雾气层（可擦除） =====
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            wipes = wipes + pos
                            mark(pos)
                            SoundEngine.tick()
                        },
                    ) { change, _ ->
                        change.consume()
                        val p = change.position
                        val last = wipes.lastOrNull()
                        if (last == null || (p - last).getDistance() > 8f) {
                            wipes = wipes + p
                            mark(p)
                        }
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            canvasW = w
            canvasH = h
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        if (night) Color(0xE63A3450) else Color(0xEBE9EEF7),
                        if (night) Color(0xD93F3A58) else Color(0xDDF3F3FB),
                    ),
                ),
            )
            dots.forEach { (fx, fy, a) ->
                drawCircle(Color.White.copy(alpha = a), radius = (10f + fy * 46f).dp.toPx(), center = Offset(fx * w, fy * h))
            }
            val r = 44.dp.toPx()
            wipes.forEach { p ->
                drawCircle(Color.Black, radius = r * 0.72f, center = p, blendMode = BlendMode.Clear)
                drawCircle(Color.Black.copy(alpha = 0.5f), radius = r, center = p, blendMode = BlendMode.Clear)
            }
        }

        // ===== 顶部与提示 =====
        Column(Modifier.fillMaxSize().zIndex(1f)) {
            PlayHeader("雾窗画", "🌫️", onBack)
            Spacer(Modifier.height(2.dp))
            Text(
                if (done) "窗明几净，心情也擦亮了 ☀️" else "拖动手指擦一擦 · 已擦亮 $percent%",
                fontSize = 14.sp,
                color = SubInk,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Text(
            "擦出的每一道，都是给心情开的一扇窗",
            fontSize = 12.sp,
            color = SubInk,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
        )

        if (done) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF2FFFFFF))
                    .padding(horizontal = 26.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("☀️", fontSize = 34.sp)
                Spacer(Modifier.height(6.dp))
                Text("窗外是温柔的晚霞", fontSize = 15.sp, color = Color(0xFF4A3F4F))
                Spacer(Modifier.height(14.dp))
                Text(
                    "🫧 再雾一次",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFA78BDA), Color(0xFFF0A8BC))))
                        .bounceClickable {
                            wipes = emptyList()
                            grid.fill(false)
                            cleared = 0
                            done = false
                            SoundEngine.tick()
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }
    }
}
