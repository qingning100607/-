package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.MuyuGlyph
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.ChipBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.screenBg
import kotlin.math.exp
import kotlin.random.Random

private data class HitText(val text: String, val x: Float, val y: Float, val born: Long, val big: Boolean)

/** 电子木鱼：敲一下，功德+1 */
@Composable
fun MuyuScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val rnd = remember { Random(System.currentTimeMillis()) }
    var karma by remember { mutableStateOf(Store.karma) }
    var lastHit by remember { mutableStateOf(0L) }
    var lastClick by remember { mutableStateOf(0L) }
    var combo by remember { mutableStateOf(0) }
    val hitsRef = remember { mutableStateOf(listOf<HitText>()) }
    var t by remember { mutableStateOf(0L) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        while (true) withFrameNanos { t = it }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(screenBg(Color(0xFFFBF3E8), Color(0xFFF7E8E2)))
    ) {
        // 注意：Canvas 在下方且全屏捕获手势，这里必须抬高层级，保证返回键可点
        Column(Modifier.fillMaxSize().zIndex(1f)) {
            PlayHeader("电子木鱼", null, onBack) { MuyuGlyph(Modifier.size(30.dp)) }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("功德 ", fontSize = 18.sp, color = SubInk)
                Text("$karma", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9A227))
                Spacer(Modifier.width(14.dp))
                Text(
                    "清零",
                    fontSize = 12.sp,
                    color = SubInk,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ChipBg)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .bounceClickable {
                            karma = 0
                            Store.karma = 0
                            SoundEngine.tick()
                        },
                )
            }
            if (combo >= 6 && (t - lastClick) / 1e9f < 1f) {
                Text(
                    "手速起飞！🔥",
                    fontSize = 14.sp,
                    color = Color(0xFFE86A6A),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val now = System.nanoTime()
                        karma++
                        Store.addKarma(1)
                        Store.muyu += 1
                        SoundEngine.muyu()
                        if (AppSettings.hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastHit = now
                        val big = rnd.nextFloat() < 0.05f
                        combo = if (now - lastClick < 400_000_000L) combo + 1 else 1
                        lastClick = now
                        val text = if (big) "功德 +999！" else "功德 +1"
                        hitsRef.value = (
                            hitsRef.value.filter { (now - it.born) / 1e9f < 1.2f } +
                                HitText(text, pos.x + rnd.nextFloat() * 60f - 30f, pos.y - 60f, now, big)
                            ).takeLast(24)
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.58f
            val rx = w * 0.30f
            val ry = rx * 0.88f

            val hitAge = if (lastHit == 0L) 10f else (t - lastHit) / 1e9f
            val press = 0.10f * exp(-hitAge * 7f)
            val squish = 1f - press

            // 垫布
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0x5CD9B08C), Color(0x2ED9B08C))),
                topLeft = Offset(cx - rx * 1.30f, cy + ry * 0.72f),
                size = androidx.compose.ui.geometry.Size(rx * 2.6f, ry * 0.95f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f),
            )
            // 底部软阴影
            drawOval(
                Color(0x1F000000),
                topLeft = Offset(cx - rx * 0.86f, cy + ry * 0.80f),
                size = androidx.compose.ui.geometry.Size(rx * 1.72f, ry * 0.30f),
            )

            // 顶部提环（先画，下半被鱼身遮住）
            val ringR = rx * 0.17f
            val ringCy = cy - ry + 6f
            drawArc(
                Color(0xFF4E2F18),
                160f, 260f, false,
                topLeft = Offset(cx - ringR, ringCy - ringR),
                size = androidx.compose.ui.geometry.Size(ringR * 2f, ringR * 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = rx * 0.115f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
            drawArc(
                Color(0xFF8A5A3B),
                160f, 260f, false,
                topLeft = Offset(cx - ringR, ringCy - ringR),
                size = androidx.compose.ui.geometry.Size(ringR * 2f, ringR * 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = rx * 0.078f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )

            // 木鱼身
            drawOval(
                Brush.radialGradient(
                    listOf(Color(0xFFB5834F), Color(0xFF82522B), Color(0xFF573219)),
                    center = Offset(cx - rx * 0.30f, cy - ry * 0.34f),
                    radius = rx * 1.55f,
                ),
                topLeft = Offset(cx - rx, cy - ry * squish),
                size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f * squish),
            )
            // 轮廓
            drawOval(
                Color(0x40321E0E),
                topLeft = Offset(cx - rx, cy - ry * squish),
                size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f * squish),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f),
            )
            // 木纹（两道淡弧）
            drawArc(
                Color(0x143E2A18),
                205f, 75f, false,
                topLeft = Offset(cx - rx * 0.80f, cy - ry * 0.66f),
                size = androidx.compose.ui.geometry.Size(rx * 1.60f, ry * 1.28f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
            )
            drawArc(
                Color(0x0F3E2A18),
                20f, 55f, false,
                topLeft = Offset(cx - rx * 0.74f, cy - ry * 0.40f),
                size = androidx.compose.ui.geometry.Size(rx * 1.48f, ry * 1.20f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
            )
            // 高光
            drawArc(
                Color(0x2EFFFFFF),
                205f, 58f, false,
                topLeft = Offset(cx - rx * 0.70f, cy - ry * 0.62f),
                size = androidx.compose.ui.geometry.Size(rx * 1.40f, ry * 1.06f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 10f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )

            // 正面开口（月牙形）
            drawArc(
                Color(0x991D0E04),
                46.5f, 87f, false,
                topLeft = Offset(cx - rx * 0.85f, cy - ry * 1.08f),
                size = androidx.compose.ui.geometry.Size(rx * 1.7f, ry * 1.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = ry * 0.185f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
            drawArc(
                Color(0xFF2B1809),
                46.5f, 87f, false,
                topLeft = Offset(cx - rx * 0.85f, cy - ry * 1.08f),
                size = androidx.compose.ui.geometry.Size(rx * 1.7f, ry * 1.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = ry * 0.155f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
            // 开口下缘反光
            drawArc(
                Color(0x55C89B6B),
                46.5f, 87f, false,
                topLeft = Offset(cx - rx * 0.85f, cy - ry * 0.98f),
                size = androidx.compose.ui.geometry.Size(rx * 1.7f, ry * 1.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = ry * 0.03f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )

            // 木槌
            val stickAng = -0.30f + 0.46f * exp(-hitAge * 6f)
            val pivot = Offset(cx + rx * 0.80f, cy - ry * 1.58f)
            rotate(degrees = stickAng * 180f / Math.PI.toFloat(), pivot = pivot) {
                val headC = Offset(cx + rx * 0.80f, cy - ry * 0.52f)
                drawLine(
                    Color(0xFF6E4522),
                    pivot,
                    headC,
                    strokeWidth = rx * 0.064f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    Color(0xFFC89B6B),
                    pivot,
                    headC,
                    strokeWidth = rx * 0.046f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    Color(0xFF6E4522),
                    Offset(headC.x - rx * 0.155f, headC.y),
                    Offset(headC.x + rx * 0.155f, headC.y),
                    strokeWidth = rx * 0.14f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    Color(0xFFD9B084),
                    Offset(headC.x - rx * 0.14f, headC.y),
                    Offset(headC.x + rx * 0.14f, headC.y),
                    strokeWidth = rx * 0.108f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }

            // 敲击涟漪
            if (hitAge < 0.45f) {
                val k = hitAge / 0.45f
                drawCircle(
                    Color(0xFFB5834F).copy(alpha = (1f - k) * 0.35f),
                    radius = rx * 0.06f + k * rx * 0.46f,
                    center = Offset(cx + rx * 0.58f, cy - ry * 0.60f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f),
                )
            }

            // 飘字
            hitsRef.value.forEach { ht ->
                val age = (t - ht.born) / 1e9f
                if (age in 0f..1.2f) {
                    val y = ht.y - age * 90f
                    val alpha = (1f - age / 1.2f).coerceIn(0f, 1f)
                    val layout = textMeasurer.measure(
                        ht.text,
                        TextStyle(
                            fontSize = if (ht.big) 20.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC9A227).copy(alpha = alpha),
                        ),
                    )
                    drawText(layout, topLeft = Offset(ht.x - layout.size.width / 2f, y))
                }
            }
        }

        Text(
            "轻点木鱼，功德 +1，烦恼 -1",
            fontSize = 12.sp,
            color = SubInk,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
        )
    }
}