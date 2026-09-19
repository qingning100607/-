package com.qingning.cloudrest.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.ChipBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.SunsetBrush
import com.qingning.cloudrest.ui.theme.SunsetBrushSoft
import com.qingning.cloudrest.ui.theme.screenBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 呼吸阶段：名称 / 秒数 / 起止缩放
private data class BreathPhase(val label: String, val secs: Int, val from: Float, val to: Float)

// 呼吸节奏：名称 / 说明 / 阶段序列（多档可选）
private data class BreathRhythm(val name: String, val desc: String, val phases: List<BreathPhase>)

private val RHYTHMS = listOf(
    BreathRhythm(
        "4-7-8 助眠",
        "4-7-8 呼吸法：吸气 4 秒 → 屏住 7 秒 → 呼气 8 秒，一轮约 19 秒。\n做完 2-3 轮，心绪就会慢慢沉下来。",
        listOf(
            BreathPhase("吸气", 4, 0.72f, 1.0f),
            BreathPhase("屏住", 7, 1.0f, 1.0f),
            BreathPhase("呼气", 8, 1.0f, 0.72f),
        ),
    ),
    BreathRhythm(
        "箱式呼吸",
        "箱式呼吸（4-4-4-4）：吸气 4 秒 → 屏住 4 秒 → 呼气 4 秒 → 再屏住 4 秒。\n像沿着方框走一圈，专注又镇定。",
        listOf(
            BreathPhase("吸气", 4, 0.72f, 1.0f),
            BreathPhase("屏住", 4, 1.0f, 1.0f),
            BreathPhase("呼气", 4, 1.0f, 0.72f),
            BreathPhase("屏住", 4, 0.72f, 0.72f),
        ),
    ),
    BreathRhythm(
        "舒缓 4-6",
        "舒缓呼吸（4-6）：吸气 4 秒 → 呼气 6 秒，没有屏息。\n节奏平缓，适合第一次接触呼吸练习。",
        listOf(
            BreathPhase("吸气", 4, 0.72f, 1.0f),
            BreathPhase("呼气", 6, 1.0f, 0.72f),
        ),
    ),
)


/** 呼吸引导：多档节奏可选（4-7-8 / 箱式 / 舒缓），跟着圆圈慢慢呼吸 */
@Composable
fun BreathingScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val compact = LocalConfiguration.current.screenHeightDp < 600
    var rhythmIdx by remember { mutableStateOf(0) }
    val rhythm = RHYTHMS[rhythmIdx]
    val phases = rhythm.phases

    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(0) }
    var remain by remember { mutableStateOf(RHYTHMS[0].phases[0].secs) }
    var cycles by remember { mutableStateOf(0) }
    val scale = remember { Animatable(0.72f) }
    val scope = rememberCoroutineScope()

    // 阶段推进 + 倒计时 + 圆圈动效（同一协程驱动：时长与动画严格一致）
    LaunchedEffect(running, phase, rhythmIdx) {
        if (!running) return@LaunchedEffect
        val ph = phases[phase]
        val secs = ph.secs
        // 暂停恢复时从当前剩余秒继续；阶段切换 / 重置后补满
        if (remain !in 1 until secs) {
            remain = secs
            if (scale.value != ph.from) scale.snapTo(ph.from)
        }
        val total = remain
        val anim = launch {
            if (ph.to != ph.from) {
                scale.animateTo(ph.to, tween(total * 1000, easing = FastOutSlowInEasing))
            }
        }
        while (remain > 0) {
            delay(1000)
            remain -= 1
        }
        anim.join()
        val next = (phase + 1) % phases.size
        if (next == 0) {
            cycles++
            Store.breathRounds += 1
        }
        phase = next
        if (AppSettings.hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (AppSettings.bellGuide) SoundEngine.chime() else SoundEngine.tick()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(screenBg(Color(0xFFEDF6F1), Color(0xFFF3EDF9)))
            .padding(horizontal = 20.dp)
    ) {
        PlayHeader("呼吸引导", "🫁", onBack)

        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RHYTHMS.forEachIndexed { idx, r ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (idx == rhythmIdx) SunsetBrush else SolidColor(ChipBg))
                        .bounceClickable {
                            if (idx != rhythmIdx) {
                                rhythmIdx = idx
                                phase = 0
                                remain = RHYTHMS[idx].phases[0].secs
                                cycles = 0
                                if (!running) scope.launch { scale.snapTo(RHYTHMS[idx].phases[0].from) }
                                SoundEngine.tick()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        r.name,
                        fontSize = 13.sp,
                        color = if (idx == rhythmIdx) Color.White else SubInk,
                        modifier = Modifier.padding(vertical = if (compact) 8.dp else 10.dp),
                    )
                }
            }
        }

        if (compact) Spacer(Modifier.height(12.dp)) else Spacer(Modifier.weight(0.6f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(if (compact) 150.dp else 220.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = 0.35f + 0.65f * scale.value
                    }
                    .clip(CircleShape)
                    .background(SunsetBrushSoft),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        phases[phase].label,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$remain", fontSize = 22.sp, color = Color(0xE6FFFFFF))
                }
            }
        }
        Spacer(Modifier.height(if (compact) 12.dp else 26.dp))
        Text(
            if (running) "第 ${cycles + 1} 轮 · 跟着圆圈慢慢呼吸" else "准备好后，点「开始」",
            fontSize = 14.sp,
            color = SubInk,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (compact) Spacer(Modifier.height(10.dp)) else Spacer(Modifier.weight(1f))

        if (!compact) {
            SoftCard {
                Text(
                    rhythm.desc,
                    fontSize = 13.sp, color = SubInk, lineHeight = 21.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SunsetBrush)
                    .bounceClickable {
                        running = !running
                        SoundEngine.tick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (running) "暂停" else "开始",
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 13.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ChipBg)
                    .bounceClickable {
                        running = false
                        phase = 0
                        remain = phases[0].secs
                        cycles = 0
                        scope.launch { scale.snapTo(phases[0].from) }
                        SoundEngine.tick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "重置",
                    fontSize = 15.sp,
                    color = Ink,
                    modifier = Modifier.padding(vertical = 13.dp),
                )
            }
        }
        Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
    }
}