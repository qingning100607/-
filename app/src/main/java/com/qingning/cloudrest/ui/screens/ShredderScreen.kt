package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.SunsetBrush
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class ShredStage { IDLE, SHREDDING, DONE }

private data class Strip(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val vx: Float,
    val vy: Float,
    val rot: Float,
    val vr: Float,
    val born: Long,
)

/** 烦恼粉碎机：写下烦恼，亲眼看着它碎掉 */
@Composable
fun ShredderScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rnd = remember { Random(System.currentTimeMillis()) }
    var text by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(ShredStage.IDLE) }
    var strips by remember { mutableStateOf(listOf<Strip>()) }
    var resultQuote by remember { mutableStateOf("") }
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    var t by remember { mutableStateOf(0L) }
    var shreddedCount by remember { mutableStateOf(Store.shredded) }

    LaunchedEffect(Unit) {
        while (true) withFrameNanos { t = it }
    }

    LaunchedEffect(stage) {
        if (stage == ShredStage.SHREDDING) {
            delay(1000)
            stage = ShredStage.DONE
            SoundEngine.chime()
        }
    }

    fun shredIt() {
        if (text.isBlank() || stage == ShredStage.SHREDDING || cardRect == Rect.Zero) return
        SoundEngine.shred()
        if (AppSettings.hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val now = System.nanoTime()
        val n = (text.length / 2 + 4).coerceIn(6, 16)
        val stripW = cardRect.width / n
        strips = List(n) { i ->
            Strip(
                x = cardRect.left + i * stripW,
                y = cardRect.top,
                w = stripW,
                h = cardRect.height,
                vx = (rnd.nextFloat() - 0.5f) * 260f * density.density,
                vy = (140f + rnd.nextFloat() * 300f) * density.density,
                rot = rnd.nextFloat() * 0.5f - 0.25f,
                vr = (rnd.nextFloat() - 0.5f) * 7f,
                born = now,
            )
        }
        resultQuote = Store.randomQuote()
        text = ""
        shreddedCount++
        Store.shredded = shreddedCount
        stage = ShredStage.SHREDDING
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(com.qingning.cloudrest.ui.theme.screenBg(Color(0xFFFBEFF2), Color(0xFFFBF3E8)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            PlayHeader("烦恼粉碎机", "🗑️", onBack)
            Text("写下来，然后亲眼看着它碎掉", fontSize = 13.sp, color = SubInk)
            Spacer(Modifier.height(14.dp))

            when (stage) {
                ShredStage.DONE -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xE6FFFFFF))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("烦恼已粉碎 ✨", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Spacer(Modifier.height(8.dp))
                            Text("「 $resultQuote 」", fontSize = 13.sp, color = SubInk, lineHeight = 20.sp)
                        }
                    }
                }
                else -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .onGloballyPositioned { cardRect = it.boundsInWindow() }
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xE6FFFFFF))
                            .padding(18.dp)
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { if (it.length <= 120) text = it },
                            textStyle = TextStyle(fontSize = 15.sp, color = Ink, lineHeight = 23.sp),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { inner ->
                                if (text.isEmpty()) {
                                    Text("写下你的烦恼…（写完就粉碎它）", fontSize = 14.sp, color = Color(0x668A7E90))
                                }
                                inner()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (stage == ShredStage.SHREDDING || text.isBlank()) SolidColor(Color(0x338A7E90))
                        else SunsetBrush
                    )
                    .bounceClickable { if (stage == ShredStage.DONE) stage = ShredStage.IDLE else shredIt() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (stage) {
                        ShredStage.SHREDDING -> "粉碎中…"
                        ShredStage.DONE -> "再写一件"
                        else -> if (text.isBlank()) "先写点什么吧" else "粉碎它 🗑️"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "已粉碎 $shreddedCount 件烦恼",
                fontSize = 12.sp,
                color = SubInk,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(20.dp))
        }

        Canvas(Modifier.fillMaxSize()) {
            strips.forEach { s ->
                val age = (t - s.born) / 1e9f
                if (age in 0f..1.1f) {
                    val alpha = (1f - age / 1.1f).coerceIn(0f, 1f)
                    val x = s.x + s.vx * age
                    val y = s.y + s.vy * age + 0.5f * 700f * age * age
                    val rot = (s.rot + s.vr * age) * 180f / Math.PI.toFloat()
                    withTransform({
                        translate(x + s.w / 2f, y + s.h / 2f)
                        rotate(rot, pivot = Offset.Zero)
                    }) {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = alpha * 0.95f), Color(0xFFF3E6EA).copy(alpha = alpha * 0.9f)),
                                startY = -s.h / 2f,
                                endY = s.h / 2f,
                            ),
                            topLeft = Offset(-s.w / 2f, -s.h / 2f),
                            size = androidx.compose.ui.geometry.Size(s.w, s.h),
                        )
                    }
                }
            }
        }
    }
}