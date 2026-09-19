package com.qingning.cloudrest.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.TopFade
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.ChipBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.SunsetBrush
import com.qingning.cloudrest.ui.theme.screenBg
import com.qingning.cloudrest.ui.theme.screenTopColor
import java.util.Calendar

/** 功德打卡日历：按天查看功德累计 */
@Composable
fun KarmaCalendarScreen(onBack: () -> Unit) {
    val today = remember { Calendar.getInstance() }
    var year by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(today.get(Calendar.MONTH)) } // 0..11
    val map = Store.karmaMap(year, month + 1)

    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(screenBg(Color(0xFFFBF3EF), Color(0xFFF3E6F4)))
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        PlayHeader("功德日历", "📅", onBack)
        Spacer(Modifier.height(6.dp))

        SoftCard {
            Text(
                "连续打卡 ${Store.karmaStreak()} 天 · 累计 ${Store.karmaDays()} 天 · 总功德 ${Store.karma}",
                fontSize = 14.sp, color = Ink, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("每天至少敲一下木鱼，就会被记上一朵小云 ☁️", fontSize = 12.sp, color = SubInk)
        }

        Spacer(Modifier.height(14.dp))

        SoftCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    fontSize = 22.sp,
                    color = SubInk,
                    modifier = Modifier
                        .clip(CircleShape)
                        .bounceClickable {
                            val c = Calendar.getInstance().apply {
                                set(year, month, 1)
                                add(Calendar.MONTH, -1)
                            }
                            year = c.get(Calendar.YEAR)
                            month = c.get(Calendar.MONTH)
                        }
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
                Text(
                    "${year}年${month + 1}月",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "›",
                    fontSize = 22.sp,
                    color = SubInk,
                    modifier = Modifier
                        .clip(CircleShape)
                        .bounceClickable {
                            val c = Calendar.getInstance().apply {
                                set(year, month, 1)
                                add(Calendar.MONTH, 1)
                            }
                            year = c.get(Calendar.YEAR)
                            month = c.get(Calendar.MONTH)
                        }
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                    Text(
                        d,
                        fontSize = 12.sp,
                        color = SubInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            val first = Calendar.getInstance().apply {
                set(year, month, 1)
                set(Calendar.HOUR_OF_DAY, 12)
            }
            val lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 周一开头
            val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
            val rows = (lead + days + 6) / 7
            repeat(rows) { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    repeat(7) { i ->
                        val idx = r * 7 + i
                        val day = idx - lead + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day in 1..days) {
                                val key = "%04d-%02d-%02d".format(year, month + 1, day)
                                val k = map[key] ?: 0L
                                val isToday = year == today.get(Calendar.YEAR) &&
                                    month == today.get(Calendar.MONTH) &&
                                    day == today.get(Calendar.DAY_OF_MONTH)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(if (k > 0) SunsetBrush else SolidColor(ChipBg)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$day",
                                            fontSize = 12.sp,
                                            color = if (k > 0) Color.White else SubInk,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                    Text(if (k > 0) "$k" else " ", fontSize = 9.sp, color = SubInk)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    TopFade(scroll, Modifier.align(Alignment.TopCenter), color = screenTopColor(Color(0xFFFBF3EF)))
    }
}