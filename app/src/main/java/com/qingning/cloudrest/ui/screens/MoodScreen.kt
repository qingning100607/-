package com.qingning.cloudrest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.RosePink
import com.qingning.cloudrest.ui.theme.SubInk
import java.util.Calendar

private val MoodColors = listOf(
    Color(0xFF9DB8E8), // 低落
    Color(0xFFE8D47A), // 一般
    Color(0xFF9BD5B8), // 平静
    Color(0xFFFFB37A), // 开心
    Color(0xFFF09BB0), // 超开心
)

private val MoodLabels = listOf("低落", "一般", "平静", "开心", "超开心")

private val MoodReplies = listOf(
    "抱抱你，一切都会好起来的。",
    "没关系，今天也辛苦了。",
    "平静的一天，也不错。",
    "开心就大声笑出来吧！",
    "太棒了！把这份快乐存起来。",
)

/** 心情站：每日心情打卡 + 月历回看 + 今日小纸条 */
@Composable
fun MoodScreen() {
    val today = Store.todayKey()
    var mood by remember { mutableStateOf(Store.moodOf(today)) }
    var quote by remember { mutableStateOf(Store.dailyQuote()) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(26.dp))
        Text("心情站", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text("记录今天的心情，回头看看走过的日子", fontSize = 14.sp, color = SubInk)
        Spacer(Modifier.height(18.dp))

        SoftCard {
            Text("今天感觉怎么样？", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (1..5).forEach { i ->
                    MoodFace(
                        level = i,
                        selected = mood == i,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            mood = i
                            Store.setMood(today, i)
                            SoundEngine.chime()
                        },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (mood > 0) MoodReplies[mood - 1] else "点一个表情，把今天的心情存下来",
                fontSize = 13.sp,
                color = if (mood > 0) RosePink else SubInk,
            )
        }

        Spacer(Modifier.height(14.dp))
        SoftCard {
            Row(Modifier.fillMaxWidth()) {
                StatItem("连续打卡", "${Store.streak()} 天", Modifier.weight(1f))
                StatItem("本月记录", "${Store.monthCount()} 天", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(14.dp))
        MoodTrend()

        Spacer(Modifier.height(14.dp))
        SoftCard {
            MoodCalendar()
        }

        Spacer(Modifier.height(14.dp))
        SoftCard {
            Text("今日小纸条", fontSize = 12.sp, color = SubInk)
            Spacer(Modifier.height(6.dp))
            Text("「 $quote 」", style = MaterialTheme.typography.titleMedium, color = Ink, lineHeight = 26.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "换一句 ↻",
                    fontSize = 12.sp,
                    color = RosePink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x14F0A8BC))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .bounceClickable {
                            quote = Store.randomQuote(quote)
                            SoundEngine.tick()
                        },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = SubInk)
    }
}

@Composable
private fun MoodFace(level: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MoodColors[level - 1]
    Column(modifier.bounceClickable(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = if (selected) 1.15f else 1f
                    scaleY = if (selected) 1.15f else 1f
                }
                .clip(CircleShape)
                .background(c.copy(alpha = 0.22f))
                .border(
                    if (selected) 2.5.dp else 1.dp,
                    if (selected) c else Color(0x22000000),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val w = size.width
                val h = size.height
                val eyeY = h * 0.40f
                val eyeR = w * 0.055f
                val ink = Color(0xFF5A4E60)
                drawCircle(ink, eyeR, Offset(w * 0.34f, eyeY))
                drawCircle(ink, eyeR, Offset(w * 0.66f, eyeY))
                val mouthRect = Rect(w * 0.26f, h * 0.48f, w * 0.74f, h * 0.88f)
                when (level) {
                    1 -> drawArc(ink, 200f, 140f, false, topLeft = mouthRect.topLeft, size = mouthRect.size, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
                    2 -> drawLine(ink, Offset(mouthRect.left, h * 0.62f), Offset(mouthRect.right, h * 0.62f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                    3 -> drawArc(ink, 20f, 140f, false, topLeft = mouthRect.topLeft, size = mouthRect.size, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
                    4 -> {
                        drawArc(ink, 20f, 140f, false, topLeft = mouthRect.topLeft, size = mouthRect.size, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
                        drawArc(ink.copy(alpha = 0.55f), 0f, 180f, true, topLeft = mouthRect.topLeft, size = mouthRect.size)
                    }
                    else -> {
                        drawArc(ink, 0f, 180f, true, topLeft = mouthRect.topLeft, size = mouthRect.size)
                        drawCircle(MoodColors[4].copy(alpha = 0.7f), w * 0.10f, Offset(w * 0.10f, h * 0.56f))
                        drawCircle(MoodColors[4].copy(alpha = 0.7f), w * 0.10f, Offset(w * 0.90f, h * 0.56f))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            MoodLabels[level - 1],
            fontSize = 11.sp,
            color = if (selected) c else SubInk,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun MoodTrend() {
    // 近 14 天（含今天，从左到右）的 (年, 月, 日) 列表
    val slots = ArrayList<Triple<Int, Int, Int>>()
    val c = Calendar.getInstance()
    repeat(14) {
        slots.add(0, Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)))
        c.add(Calendar.DAY_OF_MONTH, -1)
    }
    val merged = HashMap<String, Int>()
    slots.map { it.first to it.second }.distinct().forEach { (y, m) ->
        merged.putAll(Store.moodMap(y, m))
    }
    val moods = slots.map { (y, m, d) -> merged["%04d-%02d-%02d".format(y, m, d)] ?: 0 }
    val recorded = moods.count { it > 0 }

    SoftCard {
        Text("情绪趋势", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            if (recorded > 0) {
                "近 14 天记录 $recorded 天 · 平均 ${"%.1f".format(moods.filter { it > 0 }.average())} 分"
            } else "记录今天的心情，曲线就会长出来",
            fontSize = 12.sp,
            color = SubInk,
        )
        Spacer(Modifier.height(14.dp))

        if (recorded == 0) {
            Text("🌱 还没有近两周的心情记录", fontSize = 13.sp, color = SubInk)
        } else {
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val n = moods.size
                val slotW = size.width / n
                val padY = 10.dp.toPx()
                val usableH = size.height - padY * 2
                fun xOf(i: Int) = slotW * (i + 0.5f)
                fun yOf(m: Int) = padY + usableH * (1f - (m - 1) / 4f)

                // “平静”参考线
                val midY = yOf(3)
                drawLine(Color(0x14000000), Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.dp.toPx())

                val pts = moods.mapIndexedNotNull { i, m ->
                    if (m > 0) Triple(m, xOf(i), yOf(m)) else null
                }
                if (pts.size >= 2) {
                    val path = Path().apply {
                        moveTo(pts.first().second, pts.first().third)
                        pts.drop(1).forEach { lineTo(it.second, it.third) }
                    }
                    drawPath(
                        path,
                        brush = Brush.linearGradient(listOf(Color(0xFF9DB8E8), Color(0xFFF09BB0))),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                pts.forEach { (m, px, py) ->
                    drawCircle(MoodColors[m - 1].copy(alpha = 0.22f), radius = 8.dp.toPx(), center = Offset(px, py))
                    drawCircle(MoodColors[m - 1], radius = 4.dp.toPx(), center = Offset(px, py))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                slots.forEachIndexed { i, (_, _, d) ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(if (i % 2 == 1) "$d" else "", fontSize = 9.sp, color = SubInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCalendar() {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val map = Store.moodMap(year, month + 1)
    val firstDow = Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK)
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val lead = firstDow - 1
    val totalCells = ((lead + days + 6) / 7) * 7

    Text("${year}年${month + 1}月", fontSize = 13.sp, color = SubInk)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { d ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(d, fontSize = 11.sp, color = SubInk)
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    for (r in 0 until totalCells / 7) {
        Row(Modifier.fillMaxWidth()) {
            for (c in 0..6) {
                val dayNum = r * 7 + c - lead + 1
                Box(
                    Modifier.weight(1f).height(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (dayNum in 1..days) {
                        val key = "%04d-%02d-%02d".format(year, month + 1, dayNum)
                        val m = map[key] ?: 0
                        val isToday = dayNum == today
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$dayNum",
                                fontSize = 12.sp,
                                color = if (isToday) RosePink else Ink,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .size(if (isToday) 7.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (m > 0) MoodColors[m - 1] else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}