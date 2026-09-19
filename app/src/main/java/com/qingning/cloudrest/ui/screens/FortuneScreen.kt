package com.qingning.cloudrest.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.components.shareFortunePoster
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.RosePink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.SunsetBrush
import com.qingning.cloudrest.ui.theme.SunsetBrushSoft
import com.qingning.cloudrest.ui.theme.screenBg
import kotlinx.coroutines.launch

/** 云签：等级 / 标题 / 签诗 / 解签 / 功德彩头 */
internal data class FortuneSlip(val level: String, val title: String, val poem: String, val note: String, val bonus: Long)

internal val FORTUNE_SLIPS = listOf(
    FortuneSlip("上上签", "云开见月", "拨云见月明，万事渐澄清。", "今天适合把拖着的小事一件件放下，做完一件，轻一分。", 3),
    FortuneSlip("上上签", "晚霞满仓", "落日熔金，暮云合璧。", "好运正在充值，今天记得抬头看看天。", 3),
    FortuneSlip("上吉签", "云朵枕头", "云做枕，风做被，一觉到天明。", "今晚会睡得很沉，早点躺下接住它。", 2),
    FortuneSlip("上吉签", "顺水行舟", "水到渠成，船到桥头自然直。", "别急着用力，顺着节奏走就行。", 2),
    FortuneSlip("上吉签", "小满", "小满胜万全，够用即是圆满。", "今天的收获刚刚好，别嫌它小。", 2),
    FortuneSlip("中吉签", "一盏茶", "一盏清茶慢慢喝，苦尽甘回在舌尖。", "给自己倒杯水，缓上十分钟。", 1),
    FortuneSlip("中吉签", "风歇", "今日风且歇，心闲万事缓。", "事情没那么急，先深呼吸三次。", 1),
    FortuneSlip("中吉签", "猫的尾巴", "好运像猫尾巴，追不到，却一直在附近。", "别盯着结果看，它正悄悄绕回来。", 1),
    FortuneSlip("中吉签", "细雨洗尘", "细雨洗去三寸尘，心事轻了一格。", "允许自己有点小情绪，它会过去的。", 1),
    FortuneSlip("中吉签", "慢云", "云从不赶路，却总能抵达想去的地方。", "按自己的速度来，不比较，不慌张。", 1),
    FortuneSlip("小吉签", "晴转多云", "天有晴有云，心不必一直晴朗。", "多云也没关系，记得给自己带把伞。", 1),
    FortuneSlip("小吉签", "半糖", "生活半糖，苦里也回甘。", "给自己安排一件小小甜甜的事。", 1),
    FortuneSlip("小吉签", "小憩", "困了就歇，月亮也在云后打盹。", "累了就眯十分钟，天塌不下来。", 1),
    FortuneSlip("小吉签", "走路看花", "慢慢走，也别忘了看看路边。", "今天散步时，试着有五分钟不看手机。", 1),
    FortuneSlip("平签", "无事小神仙", "今日无事，正是最好的事。", "平平安安，就值得偷笑一下。", 1),
    FortuneSlip("平签", "静水流深", "水面平静，底下自有力量。", "安静的一天，也是积蓄的一天。", 1),
)

/** 今日云签：一天一签，抽到的签与彩头当日保留 */
@Composable
fun FortuneScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var drawn by remember { mutableStateOf(Store.fortuneOf(Store.todayKey())) }
    var justDrawn by remember { mutableStateOf(false) }
    val pop = remember { Animatable(1f) }

    Column(
        Modifier
            .fillMaxSize()
            .background(screenBg(Color(0xFFFDF6EC), Color(0xFFF3EDF9)))
            .padding(horizontal = 20.dp)
    ) {
        PlayHeader("今日云签", "🎋", onBack)

        Spacer(Modifier.weight(0.5f))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (drawn < 0) {
                // 未抽：签筒
                Box(
                    Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(SunsetBrushSoft)
                        .bounceClickable {
                            val idx = (Math.random() * FORTUNE_SLIPS.size).toInt().coerceIn(0, FORTUNE_SLIPS.size - 1)
                            Store.setFortune(Store.todayKey(), idx)
                            Store.addKarma(FORTUNE_SLIPS[idx].bonus)
                            drawn = idx
                            justDrawn = true
                            scope.launch {
                                pop.snapTo(0.82f)
                                pop.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
                            }
                            if (AppSettings.hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundEngine.chime()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("☁️", fontSize = 40.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("轻点抽签", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("一天一签 · 抽一次就好", fontSize = 12.sp, color = Color(0xE6FFFFFF))
                    }
                }
            } else {
                // 已抽：结果
                val slip = FORTUNE_SLIPS[drawn.coerceIn(0, FORTUNE_SLIPS.size - 1)]
                Box(
                    Modifier.graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                ) {
                    SoftCard {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("「${slip.level}」", fontSize = 14.sp, color = RosePink, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(slip.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Spacer(Modifier.height(12.dp))
                            Text(slip.poem, fontSize = 16.sp, color = Ink, lineHeight = 26.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(slip.note, fontSize = 13.sp, color = SubInk, lineHeight = 21.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            Text("✦ 功德 +${slip.bonus} 已记入功德簿", fontSize = 12.sp, color = RosePink)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(if (drawn < 0) 14.dp else 22.dp))
        Text(
            when {
                drawn < 0 -> "诚恳一点，云会听见 ☁️"
                justDrawn -> "签已收好，好运随身"
                else -> "今天已经抽过啦，明天再来 ☁️"
            },
            fontSize = 14.sp,
            color = SubInk,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.weight(1f))

        if (drawn >= 0) {
            val slip = FORTUNE_SLIPS[drawn.coerceIn(0, FORTUNE_SLIPS.size - 1)]
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SunsetBrushSoft)
                        .bounceClickable {
                            SoundEngine.tick()
                            shareFortunePoster(context, slip)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "📤 分享云签",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 13.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SunsetBrush)
                        .bounceClickable {
                            SoundEngine.tick()
                            onBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "收下这份好运",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 13.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}