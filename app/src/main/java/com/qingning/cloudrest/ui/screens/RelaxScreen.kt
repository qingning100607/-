package com.qingning.cloudrest.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.PlayPage
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.components.MuyuGlyph
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.TopFade
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.theme.BackdropTopColor
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.SubInk

/** 解压铺：八个小玩法入口 + 战绩 */
@Composable
fun RelaxScreen(onOpen: (PlayPage) -> Unit) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(26.dp))
        Text("解压铺", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text("选一样，把压力放掉", fontSize = 14.sp, color = SubInk)
        Spacer(Modifier.height(18.dp))

        val games = listOf(
            Game("🫧", "捏泡泡纸", "噼啪作响，捏爆一整张", PlayPage.BUBBLE, Brush.linearGradient(listOf(Color(0xFFB8D8F0), Color(0xFFC9B8F0)))),
            Game("🪷", "电子木鱼", "功德+1，烦恼-1", PlayPage.MUYU, Brush.linearGradient(listOf(Color(0xFFB5E0C8), Color(0xFF9BD5B8))), icon = { MuyuGlyph(Modifier.size(34.dp)) }),
            Game("🎆", "烟花手账", "指尖绽放一整片夜空", PlayPage.FIREWORK, Brush.linearGradient(listOf(Color(0xFF7B6CA8), Color(0xFFA78BDA)))),
            Game("🧊", "敲冰块", "咔啦一声，烦恼碎裂", PlayPage.ICE, Brush.linearGradient(listOf(Color(0xFFA8C8F0), Color(0xFFC9E4F5)))),
            Game("🗑️", "烦恼粉碎机", "写下来，亲手粉碎它", PlayPage.SHRED, Brush.linearGradient(listOf(Color(0xFFF0A8BC), Color(0xFFFFC9A3)))),
            Game("🌫️", "雾窗画", "擦一擦，窗外是温柔的晚霞", PlayPage.FOG, Brush.linearGradient(listOf(Color(0xFF8FA8C8), Color(0xFFB8D0E0)))),
            Game("🌼", "吹蒲公英", "轻轻一点，烦恼飘向远方", PlayPage.DANDELION, Brush.linearGradient(listOf(Color(0xFFA8D8B0), Color(0xFFD8E8A8)))),
            Game("🌊", "打水漂", "看准落水那一下，跳！", PlayPage.STONE, Brush.linearGradient(listOf(Color(0xFF6FB8C8), Color(0xFFA8D8E0)))),
        )

        games.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth()) {
                rowItems.forEach { g ->
                    GameCard(
                        g,
                        Modifier
                            .weight(1f)
                            .padding(end = if (rowItems.size == 2) 12.dp else 0.dp),
                        onClick = { onOpen(g.page) },
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))
        BreathingBanner { onOpen(PlayPage.BREATH) }

        Spacer(Modifier.height(12.dp))
        StatsCard()

        Spacer(Modifier.height(12.dp))
        ComingSoonCard()
        Spacer(Modifier.height(24.dp))
    }
    TopFade(scroll, Modifier.align(Alignment.TopCenter).statusBarsPadding(), color = BackdropTopColor)
    }
}

@Composable
private fun ComingSoonCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x59FFFFFF))
            .border(1.dp, Color(0x33A78BDA), RoundedCornerShape(24.dp))
            .padding(vertical = 22.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🔮", fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text("更多解压玩法正在赶来的路上", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(3.dp))
        Text("敬请期待 ✨", fontSize = 13.sp, color = SubInk)
    }
}

private data class Game(
    val emoji: String,
    val title: String,
    val desc: String,
    val page: PlayPage,
    val brush: Brush,
    val icon: (@Composable () -> Unit)? = null,
)

@Composable
private fun GameCard(g: Game, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(g.brush)
            .bounceClickable(onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val ic = g.icon
        if (ic != null) {
            ic()
        } else {
            Text(g.emoji, fontSize = 34.sp)
        }
        Column {
            Text(g.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(g.desc, fontSize = 12.sp, color = Color(0xE6FFFFFF), lineHeight = 17.sp)
        }
    }
}

@Composable
private fun BreathingBanner(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF8E7BD8), Color(0xFF6FB7D9))))
            .bounceClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🌙", fontSize = 30.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("呼吸引导", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text("4-7-8 助眠 / 箱式 / 舒缓 4-6 · 三档节奏任选", fontSize = 12.sp, color = Color(0xE6FFFFFF))
        }
        Text("→", fontSize = 18.sp, color = Color(0xE6FFFFFF))
    }
}

@Composable
private fun StatsCard() {
    SoftCard {
        Text("解压战绩", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        val stats = remember { Store }
        Text(
            "已捏爆泡泡 ${stats.bubbles} 个 · 功德 ${stats.karma} · 敲碎冰块 ${stats.iceBroken} 块\n" +
                "放烟花 ${stats.fireworks} 朵 · 粉碎烦恼 ${stats.shredded} 件",
            fontSize = 13.sp,
            color = SubInk,
            lineHeight = 21.sp,
        )
    }
}