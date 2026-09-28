package com.qingning.cloudrest.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.audio.SoundType
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.components.CloudPetCard
import com.qingning.cloudrest.ui.components.SectionTitle
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.TopFade
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.components.sharePoster
import com.qingning.cloudrest.ui.theme.BackdropTopColor
import com.qingning.cloudrest.ui.theme.ChipBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.RosePink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.SunsetBrush
import com.qingning.cloudrest.ui.theme.SunsetBrushSoft
import com.qingning.cloudrest.update.UpdateBroadcast
import com.qingning.cloudrest.update.UpdateInstaller
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 休息厅：问候 + 每日一句 + 自然声混音台 */
@Composable
fun HomeScreen(onOpenKarmaCal: () -> Unit, onOpenFortune: () -> Unit, onOpenAchievements: () -> Unit) {
    val scroll = rememberScrollState()
    val ctx = LocalContext.current
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }
    var upDownloading by remember { mutableStateOf(false) }
    var upProgress by remember { mutableStateOf(0) }
    val upScope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(26.dp))
        Text(
            SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date()),
            fontSize = 14.sp,
            color = SubInk,
        )
        Spacer(Modifier.height(6.dp))
        val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
        Text(
            when (hour) {
                in 5..10 -> "早上好，慢慢醒来 ☁️"
                in 11..13 -> "中午好，记得好好吃饭 ☁️"
                in 14..17 -> "下午好，来歇一歇吧 ☁️"
                in 18..22 -> "晚上好，把疲惫放下吧 🌙"
                else -> "夜深了，早点休息哦 ✨"
            },
            style = MaterialTheme.typography.headlineLarge,
            color = Ink,
        )
        if (UpdateBroadcast.hasNew(version)) {
            Spacer(Modifier.height(14.dp))
            UpdateBanner(
                version = UpdateBroadcast.found?.latest.orEmpty(),
                downloading = upDownloading,
                progress = upProgress,
                onUpdate = {
                    val r = UpdateBroadcast.found
                    if (r == null) {
                        // 理论上不会走到，兜底空操作
                    } else if (r.apk.isBlank()) {
                        // 没有 APK 直链（例如仅从 tags 兜底得知新版本）→ 跳浏览器
                        try {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(r.url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            Toast.makeText(ctx, "打不开浏览器，下载页：${r.url}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        upDownloading = true
                        upProgress = 0
                        SoundEngine.tick()
                        upScope.launch {
                            val f = UpdateInstaller.download(ctx, r.apk) { upProgress = it }
                            upDownloading = false
                            if (f != null) {
                                UpdateInstaller.install(ctx, f)
                            } else {
                                Toast.makeText(ctx, "下载没成功，检查一下网络再试试", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onIgnore = { UpdateBroadcast.found?.latest?.let { UpdateBroadcast.ignore(it) } },
            )
        }
        Spacer(Modifier.height(18.dp))
        QuoteCard()
        Spacer(Modifier.height(14.dp))
        FortuneCard(onOpenFortune)
        Spacer(Modifier.height(14.dp))
        CloudPetCard()
        Spacer(Modifier.height(22.dp))
        SectionTitle("自然声")
        Spacer(Modifier.height(10.dp))
        SoundCard()
        Spacer(Modifier.height(10.dp))
        Text(
            "自然声为真实录音素材。建议佩戴耳机。",
            fontSize = 12.sp,
            color = SubInk,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(20.dp))
        SectionTitle("我的小窝")
        Spacer(Modifier.height(10.dp))
        NookCard(onOpenKarmaCal, onOpenAchievements)
        Spacer(Modifier.height(24.dp))
    }
    TopFade(scroll, Modifier.align(Alignment.TopCenter).statusBarsPadding(), color = BackdropTopColor)
    }
}

@Composable
private fun NookCard(onOpenKarmaCal: () -> Unit, onOpenAchievements: () -> Unit) {
    val ctx = LocalContext.current
    SoftCard {
        Text(
            "累计功德 ${Store.karma} · 打卡 ${Store.karmaDays()} 天 · 解压 ${Store.bubbles + Store.iceBroken + Store.fireworks + Store.shredded} 次",
            fontSize = 13.sp,
            color = SubInk,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SunsetBrush)
                    .bounceClickable {
                        SoundEngine.tick()
                        onOpenKarmaCal()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "📅 功德日历",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 11.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SunsetBrushSoft)
                    .bounceClickable {
                        SoundEngine.tick()
                        onOpenAchievements()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "🏅 成就墙",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 11.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SunsetBrushSoft)
                    .bounceClickable {
                        SoundEngine.tick()
                        sharePoster(ctx)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "📤 分享战绩",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 11.dp),
                )
            }
        }
    }
}

@Composable
private fun FortuneCard(onOpenFortune: () -> Unit) {
    val idx = Store.fortuneOf(Store.todayKey())
    val slip = if (idx in FORTUNE_SLIPS.indices) FORTUNE_SLIPS[idx] else null
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SunsetBrushSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎋", fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("今日云签", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (slip == null) "轻点抽一支今天的云签" else "已签「${slip.level} · ${slip.title}」",
                    fontSize = 12.sp,
                    color = SubInk,
                )
            }
            Text(
                if (slip == null) "去抽签" else "查看",
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SunsetBrush)
                    .padding(horizontal = 13.dp, vertical = 8.dp)
                    .bounceClickable {
                        SoundEngine.tick()
                        onOpenFortune()
                    },
            )
        }
    }
}

/** 开启声音前，若系统要求（Android 13+）先申请通知权限，保证后台播放的通知可见 */
private fun ensureNotifyPermission(context: android.content.Context) {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        val act = context as? android.app.Activity ?: return
        if (act.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            act.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 4104)
        }
    }
}

@Composable
private fun QuoteCard() {
    var quote by remember { mutableStateOf(Store.randomQuote()) }
    SoftCard(brush = SunsetBrushSoft) {
        Text(
            "「 $quote 」",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            lineHeight = 26.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "换一句 ↻",
                fontSize = 12.sp,
                color = Color(0xE6FFFFFF),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x26FFFFFF))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .bounceClickable {
                        quote = Store.randomQuote(quote)
                        SoundEngine.tick()
                    },
            )
        }
    }
}

@Composable
private fun SoundCard() {
    var timerOption by remember { mutableStateOf(0) }
    var remainSec by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            remainSec = if (SoundEngine.fadeDeadline > 0L) {
                ((SoundEngine.fadeDeadline - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            } else 0
            delay(1000)
        }
    }

    SoftCard {
        SoundType.entries.forEach { ch ->
            SoundRow(ch)
            Spacer(Modifier.height(10.dp))
        }
        SceneSection(onTimerSet = { timerOption = it })
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("定时关闭", style = MaterialTheme.typography.bodyMedium, color = SubInk)
            Spacer(Modifier.width(12.dp))
            listOf(0 to "不限", 15 to "15分", 30 to "30分", 60 to "60分").forEach { (v, label) ->
                val sel = timerOption == v
                Text(
                    label,
                    fontSize = 12.sp,
                    color = if (sel) Color.White else SubInk,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(CircleShape)
                        .background(if (sel) SunsetBrush else SolidColor(Color.Transparent))
                        .border(1.dp, if (sel) Color.Transparent else SubInk.copy(alpha = 0.42f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .bounceClickable {
                            timerOption = v
                            SoundEngine.scheduleStopAfter(v)
                            SoundEngine.tick()
                        },
                )
            }
        }
        if (remainSec > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "⏳ ${remainSec / 60} 分 ${remainSec % 60} 秒后渐弱停止",
                fontSize = 12.sp,
                color = RosePink,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(ChipBg)
                .bounceClickable {
                    SoundEngine.stopAll()
                    timerOption = 0
                    remainSec = 0
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("全部停止", fontSize = 13.sp, color = Ink, modifier = Modifier.padding(vertical = 10.dp))
        }
    }
}

@Composable
private fun SoundRow(ch: SoundType) {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(SoundEngine.channelOn[ch] == true) }
    var gain by remember { mutableStateOf(SoundEngine.channelGain[ch] ?: 0.7f) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (on) SunsetBrush else SolidColor(ChipBg))
                .bounceClickable {
                    on = !on
                    if (on) ensureNotifyPermission(ctx)
                    SoundEngine.toggle(ch)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(ch.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(ch.label, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(if (on) "播放中" else "点击开启", fontSize = 12.sp, color = SubInk)
        }
        if (on) {
            Slider(
                value = gain,
                onValueChange = {
                    gain = it
                    SoundEngine.setGain(ch, it)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .padding(start = 8.dp),
            )
        }
    }
}

/** 一键音景：睡前模式 / 我的音景 / 存为音景 */
@Composable
private fun SceneSection(onTimerSet: (Int) -> Unit) {
    var scenes by remember { mutableStateOf(Store.scenes()) }
    var editing by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }

    Spacer(Modifier.height(2.dp))
    Text("一键音景", fontSize = 13.sp, color = SubInk, modifier = Modifier.padding(start = 4.dp))
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
        SceneChip("🌙 睡前模式", Modifier.weight(1f)) {
            SoundEngine.applyScene(mapOf(SoundType.RAIN to 0.8f))
            SoundEngine.scheduleStopAfter(30)
            AppSettings.updateDeepNight(true)
            onTimerSet(30)
            hint = "已开启睡前模式：雨声 + 30 分钟后渐弱"
        }
        Spacer(Modifier.width(8.dp))
        SceneChip(if (editing) "✅ 完成" else "✏️ 管理", Modifier.weight(1f)) {
            editing = !editing
            hint = if (editing) "点一下音景即可删除" else ""
        }
    }
    scenes.chunked(2).forEach { rowItems ->
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            rowItems.forEach { (n, spec) ->
                SceneChip(if (editing) "✕ $n" else "🎐 $n", Modifier.weight(1f)) {
                    if (editing) {
                        Store.deleteScene(n)
                        scenes = Store.scenes()
                        hint = "已删除「$n」"
                    } else {
                        SoundEngine.applyScene(SoundEngine.decodeScene(spec))
                        hint = "已切换到「$n」"
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(8.dp))
    SceneChip("＋ 把当前混音存为音景", Modifier.fillMaxWidth()) {
        if (!SoundEngine.anyOn()) {
            hint = "先开启至少一个声音，再保存音景哦"
        } else {
            showSave = true
        }
    }
    if (hint.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(hint, fontSize = 11.sp, color = RosePink, modifier = Modifier.padding(start = 4.dp))
    }
    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("命名音景") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("如：雨夜书房") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val nm = name.trim().ifEmpty { "我的音景" }
                    Store.saveScene(nm, SoundEngine.encodeScene())
                    scenes = Store.scenes()
                    name = ""
                    showSave = false
                    hint = "已保存「$nm」"
                    SoundEngine.tick()
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSave = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SceneChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ChipBg)
            .bounceClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, color = Ink, maxLines = 1)
    }
}

/** 主页「发现新版本」提示：一键应用内下载安装，或忽略该版本 */
@Composable
private fun UpdateBanner(
    version: String,
    downloading: Boolean,
    progress: Int,
    onUpdate: () -> Unit,
    onIgnore: () -> Unit,
) {
    SoftCard(brush = SunsetBrush) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎉", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "发现新版本 v$version",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (downloading) "正在下载 $progress%" else "新版本已准备好，点右侧一键更新",
                    fontSize = 12.sp,
                    color = Color(0xE6FFFFFF),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (downloading) "$progress%" else "更新",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RosePink,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .bounceClickable { if (!downloading) onUpdate() },
            )
        }
        if (downloading) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = Color(0x40FFFFFF),
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "忽略此版本",
                    fontSize = 11.sp,
                    color = Color(0xB3FFFFFF),
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .bounceClickable(onIgnore),
                )
            }
        }
    }
}