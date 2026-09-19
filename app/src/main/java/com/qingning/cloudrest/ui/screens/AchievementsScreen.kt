package com.qingning.cloudrest.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.TopFade
import com.qingning.cloudrest.ui.theme.CardSurfaceBrush
import com.qingning.cloudrest.ui.theme.ChipBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.screenBg
import com.qingning.cloudrest.ui.theme.screenTopColor

internal data class Achievement(
    val emoji: String,
    val title: String,
    val desc: String,
    val check: () -> Boolean,
)

internal val ACHIEVEMENTS = listOf(
    Achievement("☁️", "初来乍到", "第一次来到休息室") { true },
    Achievement("🎋", "初试手气", "抽取第一支云签") { Store.fortuneDays() >= 1 },
    Achievement("🎋", "签运常客", "累计抽签 7 天") { Store.fortuneDays() >= 7 },
    Achievement("🐟", "木鱼初响", "敲木鱼 100 次") { Store.muyu >= 100 },
    Achievement("🐟", "木鱼千击", "敲木鱼 1000 次") { Store.muyu >= 1000 },
    Achievement("🫧", "泡泡新手", "捏爆 100 个泡泡") { Store.bubbles >= 100 },
    Achievement("🫧", "泡泡狂魔", "捏爆 1000 个泡泡") { Store.bubbles >= 1000 },
    Achievement("🧊", "破冰者", "敲碎 30 块冰") { Store.iceBroken >= 30 },
    Achievement("🎆", "烟花师", "绽放 20 朵烟花") { Store.fireworks >= 20 },
    Achievement("🗑️", "粉碎大师", "粉碎 20 份烦恼") { Store.shredded >= 20 },
    Achievement("🧘", "深呼吸者", "完成 10 轮呼吸引导") { Store.breathRounds >= 10 },
    Achievement("✨", "小有功德", "累计功德达到 100") { Store.karma >= 100 },
    Achievement("✨", "功德圆满", "累计功德达到 1000") { Store.karma >= 1000 },
    Achievement("✨", "功德无量", "累计功德达到 5000") { Store.karma >= 5000 },
    Achievement("📅", "打卡一周", "累计打卡 7 天") { Store.karmaDays() >= 7 },
    Achievement("📅", "习惯成自然", "累计打卡 30 天") { Store.karmaDays() >= 30 },
    Achievement("💗", "心情记录家", "记录心情 7 天") { Store.moodDays() >= 7 },
)

/** 成就墙：把一路攒下的小里程碑挂满一面墙 */
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val unlocked = ACHIEVEMENTS.count { it.check() }
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(screenBg(Color(0xFFFBF3EF), Color(0xFFF3E6F4)))
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        PlayHeader("成就墙", "🏅", onBack)
        Spacer(Modifier.height(6.dp))
        Text(
            "已点亮 $unlocked / ${ACHIEVEMENTS.size} 枚小徽章",
            fontSize = 14.sp,
            color = SubInk,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(14.dp))
        ACHIEVEMENTS.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { a ->
                    AchievementCard(a, Modifier.weight(1f).padding(end = if (row.size == 2) 12.dp else 0.dp))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
    TopFade(scroll, Modifier.align(Alignment.TopCenter), color = screenTopColor(Color(0xFFFBF3EF)))
    }
}

@Composable
private fun AchievementCard(a: Achievement, modifier: Modifier) {
    val ok = a.check()
    SoftCard(
        modifier = modifier
            .height(128.dp)
            .graphicsLayer { alpha = if (ok) 1f else 0.55f },
        corner = 22.dp,
        brush = if (ok) CardSurfaceBrush else SolidColor(ChipBg),
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(if (ok) a.emoji else "🔒", fontSize = 26.sp)
            Column {
                Text(a.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(2.dp))
                Text(a.desc, fontSize = 11.sp, color = SubInk, lineHeight = 16.sp)
            }
        }
    }
}
