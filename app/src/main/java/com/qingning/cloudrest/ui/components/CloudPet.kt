package com.qingning.cloudrest.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.theme.ChipBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import java.util.Calendar
import kotlinx.coroutines.launch

private val PET_FORMS = listOf(
    Triple("云宝宝", 0L, Color(0xFFF6CBD9)),
    Triple("小云朵", 100L, Color(0xFFF3C2D3)),
    Triple("云团团", 300L, Color(0xFFEFB7C9)),
    Triple("云舒舒", 800L, Color(0xFFE8A9BE)),
    Triple("云仙人", 2000L, Color(0xFFDF9AB2)),
)

private val PET_LINES = listOf(
    "戳轻一点啦～", "今天也有好好呼吸吗？", "云会替你把烦恼吹走的", "嘿嘿", "记得喝水哦",
    "没事的，慢慢来", "我在这里陪你", "要不要听听雨声？", "你今天已经很棒了", "(*^▽^*)",
    "跟你讲个秘密……我也不会", "呼——飘起来一点", "困了就早点睡哦", "抱一下", "明天见，不见不散", "要多笑呀",
)

/** 云朵小精灵：陪着你的一小朵云，随状态变化表情 */
@Composable
fun CloudPetCard() {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lastSeen = remember { Store.petLastSeen() }
    LaunchedEffect(Unit) { Store.setPetLastSeen(System.currentTimeMillis()) }
    val karmaTotal = Store.karma
    val karmaToday = Store.karmaOf(Store.todayKey())
    val fortuneToday = Store.fortuneOf(Store.todayKey()) >= 0
    val form = petForm(karmaTotal)
    val next = petNextForm(karmaTotal)
    val mood = petMood(karmaToday, lastSeen, fortuneToday)
    var speech by remember { mutableStateOf(defaultSpeech(mood)) }
    val scale = remember { Animatable(1f) }
    val idle = rememberInfiniteTransition(label = "pet")
    val idleScale by idle.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle",
    )

    fun tap() {
        speech = PET_LINES[(Math.random() * PET_LINES.size).toInt().coerceIn(0, PET_LINES.size - 1)]
        scope.launch {
            scale.snapTo(0.86f)
            scale.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        if (AppSettings.hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        SoundEngine.pop()
    }

    SoftCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(86.dp)
                        .graphicsLayer {
                            scaleX = scale.value * idleScale
                            scaleY = scale.value * idleScale
                        }
                        .bounceClickable { tap() },
                ) {
                    CloudBody(Modifier.fillMaxSize(), form.third, mood)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("云朵小精灵", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Text("${form.first} · ${moodText(mood)}", fontSize = 12.sp, color = SubInk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (next != null) "再攒 ${next.second - karmaTotal} 功德，变成「${next.first}」"
                        else "已经长成最大的云啦 ☁️",
                        fontSize = 11.sp,
                        color = SubInk.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ChipBg)
                    .bounceClickable { tap() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("「 $speech 」", fontSize = 13.sp, color = SubInk, lineHeight = 19.sp)
            }
        }
    }
}

/** 用纯代码画出的一朵软糯小云：三团圆 + 底座，表情随情绪变化 */
@Composable
private fun CloudBody(modifier: Modifier, body: Color, mood: String) {
    val face = Color(0xFF6B5B6E)
    Canvas(modifier) {
        val u = size.minDimension / 100f
        val ox = (size.width - 100f * u) / 2f
        val oy = (size.height - 100f * u) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)

        drawRoundRect(
            body,
            topLeft = p(16f, 54f),
            size = Size(68f * u, 20f * u),
            cornerRadius = CornerRadius(10f * u),
        )
        drawCircle(body, 18f * u, p(32f, 54f))
        drawCircle(body, 15f * u, p(70f, 58f))
        drawCircle(body, 17f * u, p(50f, 38f))
        drawCircle(Color(0x40FFFFFF), 7f * u, p(42f, 32f))
        drawCircle(Color(0x55F0A8BC), 4.5f * u, p(30f, 66f))
        drawCircle(Color(0x55F0A8BC), 4.5f * u, p(70f, 66f))

        val eyeY = 56f
        if (mood == "sleepy") {
            drawLine(face, p(38f, eyeY), p(45f, eyeY), strokeWidth = 2.4f * u)
            drawLine(face, p(55f, eyeY), p(62f, eyeY), strokeWidth = 2.4f * u)
        } else {
            drawCircle(face, 2.8f * u, p(41f, eyeY))
            drawCircle(face, 2.8f * u, p(59f, eyeY))
        }
        when (mood) {
            "happy", "miss" -> drawArc(
                color = face,
                startAngle = 30f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = p(44f, 58f),
                size = Size(12f * u, 8f * u),
                style = Stroke(width = 2.2f * u),
            )
            "sleepy" -> drawCircle(face, 2.2f * u, p(50f, 64f))
            else -> drawLine(face, p(46f, 63f), p(54f, 63f), strokeWidth = 2.2f * u)
        }
    }
}

private fun petForm(k: Long): Triple<String, Long, Color> =
    PET_FORMS.lastOrNull { k >= it.second } ?: PET_FORMS.first()

private fun petNextForm(k: Long): Triple<String, Long, Color>? =
    PET_FORMS.firstOrNull { k < it.second }

private fun petMood(karmaToday: Long, lastSeen: Long, fortuneToday: Boolean): String {
    val now = System.currentTimeMillis()
    if (lastSeen > 0L && now - lastSeen > 2L * 24 * 3600 * 1000) return "miss"
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    if (hour >= 23 || hour < 6) return "sleepy"
    if (karmaToday > 0 || fortuneToday) return "happy"
    return "calm"
}

private fun moodText(m: String): String = when (m) {
    "miss" -> "想你了，好久不见"
    "sleepy" -> "有点困了…呼…"
    "happy" -> "今天心情很好"
    else -> "今天安安静静"
}

private fun defaultSpeech(m: String): String = when (m) {
    "miss" -> "你终于回来啦！我数了好多朵云等你"
    "sleepy" -> "……呼……（已经睡着了）"
    "happy" -> "今天也很棒哦，继续保持～"
    else -> "我在这里，慢慢来 ☁️"
}