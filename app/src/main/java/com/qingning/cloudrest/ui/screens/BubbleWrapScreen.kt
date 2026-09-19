package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val COLS = 8
private const val ROWS = 12

/** 捏泡泡纸：8x12 网格，全破自动翻新 */
@Composable
fun BubbleWrapScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val brokenRef = remember { mutableStateOf(setOf<Int>()) }
    val btRef = remember { mutableStateOf(mapOf<Int, Long>()) }
    val poppedRef = remember { mutableStateOf(Store.bubbles) }
    var celebrate by remember { mutableStateOf(false) }
    var boardId by remember { mutableStateOf(0) }
    var t by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) withFrameNanos { t = it }
    }

    LaunchedEffect(brokenRef.value.size) {
        if (brokenRef.value.size == COLS * ROWS) {
            celebrate = true
            SoundEngine.chime()
            delay(1900)
            brokenRef.value = emptySet()
            btRef.value = emptyMap()
            celebrate = false
            boardId++
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(com.qingning.cloudrest.ui.theme.screenBg(Color(0xFFE8F1FB), Color(0xFFF6EFF9)))
    ) {
        Column(Modifier.fillMaxSize()) {
            PlayHeader("捏泡泡纸", "🫧", onBack)
            Row(Modifier.padding(horizontal = 24.dp)) {
                Text("剩余 ${COLS * ROWS - brokenRef.value.size} 个", fontSize = 13.sp, color = SubInk)
                Spacer(Modifier.width(16.dp))
                Text("已捏爆 ${poppedRef.value} 个", fontSize = 13.sp, color = SubInk)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "点哪捏哪，全部捏爆会自动换一张新的",
                fontSize = 12.sp,
                color = SubInk,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp),
            )
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .padding(top = 128.dp)
                .pointerInput(boardId) {
                    detectTapGestures { pos ->
                        val cell = min(size.width / COLS, size.height / ROWS)
                        val ox = (size.width - cell * COLS) / 2f
                        val oy = (size.height - cell * ROWS) / 2f
                        val col = ((pos.x - ox) / cell).toInt()
                        val row = ((pos.y - oy) / cell).toInt()
                        if (col in 0 until COLS && row in 0 until ROWS) {
                            val idx = row * COLS + col
                            if (idx !in brokenRef.value) {
                                brokenRef.value = brokenRef.value + idx
                                btRef.value = btRef.value + (idx to System.nanoTime())
                                poppedRef.value = poppedRef.value + 1
                                Store.bubbles = poppedRef.value
                                SoundEngine.pop()
                                if (AppSettings.hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }
                },
        ) {
            val cell = min(size.width / COLS, size.height / ROWS)
            val ox = (size.width - cell * COLS) / 2f
            val oy = (size.height - cell * ROWS) / 2f
            val r = cell * 0.40f
            val broken = brokenRef.value
            val bts = btRef.value
            for (j in 0 until ROWS) {
                for (i in 0 until COLS) {
                    val idx = j * COLS + i
                    val cx = ox + (i + 0.5f) * cell
                    val cy = oy + (j + 0.5f) * cell
                    val bt = bts[idx]
                    if (bt == null) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFFFFF), Color(0xFFC3DCF6)),
                                center = Offset(cx - r * 0.35f, cy - r * 0.35f),
                                radius = r * 1.8f,
                            ),
                            r,
                            Offset(cx, cy),
                        )
                        drawCircle(Color(0x99FFFFFF), r * 0.20f, Offset(cx - r * 0.35f, cy - r * 0.42f))
                    } else {
                        val age = (t - bt) / 1e9f
                        val p = (age / 0.18f).coerceIn(0f, 1f)
                        if (p < 1f) {
                            drawCircle(Color(0xFF93B8E8).copy(alpha = 1f - p), r * (1f - p), Offset(cx, cy))
                            for (k in 0 until 4) {
                                val ang = (PI.toFloat() / 4f + k * PI.toFloat() / 2f)
                                val d = p * r * 1.1f
                                drawCircle(
                                    Color(0xFF93B8E8).copy(alpha = (1f - p) * 0.8f),
                                    r * 0.12f,
                                    Offset(cx + cos(ang) * d, cy + sin(ang) * d),
                                )
                            }
                        } else {
                            drawCircle(
                                Color(0x2E8A7E90),
                                r * 0.85f,
                                Offset(cx, cy),
                                style = Stroke(width = 2f),
                            )
                        }
                    }
                }
            }
        }

        if (celebrate) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "全部捏爆！🎉",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
        }
    }
}