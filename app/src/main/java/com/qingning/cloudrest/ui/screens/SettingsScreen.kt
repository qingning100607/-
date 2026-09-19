package com.qingning.cloudrest.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingning.cloudrest.CrashGuard
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.data.Backup
import com.qingning.cloudrest.ui.AppSettings
import com.qingning.cloudrest.ui.components.Hint
import com.qingning.cloudrest.ui.components.PlayHeader
import com.qingning.cloudrest.ui.components.SoftCard
import com.qingning.cloudrest.ui.components.bounceClickable
import com.qingning.cloudrest.ui.components.decodeSampledBitmap
import com.qingning.cloudrest.ui.theme.ChipBg
import com.qingning.cloudrest.ui.theme.Ink
import com.qingning.cloudrest.ui.theme.RosePink
import com.qingning.cloudrest.ui.theme.SubInk
import com.qingning.cloudrest.ui.theme.SunsetBrush
import com.qingning.cloudrest.ui.theme.screenBg
import java.io.File

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val f = File(context.filesDir, "custom_bg.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { output -> input.copyTo(output) }
                }
                AppSettings.updateBackground(f.absolutePath)
                Toast.makeText(context, "背景已更新", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "设置失败，换一张图试试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 导出备份：系统「另存为」到用户选择的位置
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(Backup.exportJson(context).toByteArray())
                }
                Toast.makeText(context, "备份已导出", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "导出失败，换个位置再试试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 还原备份：选择之前导出的备份文件
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }.orEmpty()
                val ok = Backup.importJson(context, text)
                Toast.makeText(
                    context,
                    if (ok) "备份已还原，数据已生效" else "这个文件看起来不是云朵休息室的备份",
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (_: Exception) {
                Toast.makeText(context, "还原失败，文件可能已损坏", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(screenBg(Color(0xFFFBF3EF), Color(0xFFF3E6F4)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        PlayHeader("设置", "⚙️", onBack)
        Spacer(Modifier.height(6.dp))

        // 外观
        SoftCard {
            Text("外观", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("夜间深色模式", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Hint("深夜护眼暗色配色，全局生效")
                }
                Switch(
                    checked = AppSettings.deepNight,
                    onCheckedChange = {
                        AppSettings.updateDeepNight(it)
                        SoundEngine.tick()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("自动夜间", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Hint("22:00 后自动切换深色，早晨自动切回")
                }
                Switch(
                    checked = AppSettings.autoNight,
                    onCheckedChange = {
                        AppSettings.updateAutoNight(it)
                        SoundEngine.tick()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("季节主题", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Spacer(Modifier.height(3.dp))
            Hint("给整个界面换上季节色（春樱 / 夏夜 / 秋桂 / 冬雪）")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                TonePill("默认", AppSettings.season == "off") {
                    AppSettings.updateSeason("off")
                    SoundEngine.tick()
                }
                Spacer(Modifier.width(6.dp))
                TonePill("春樱", AppSettings.season == "spring") {
                    AppSettings.updateSeason("spring")
                    SoundEngine.tick()
                }
                Spacer(Modifier.width(6.dp))
                TonePill("夏夜", AppSettings.season == "summer") {
                    AppSettings.updateSeason("summer")
                    SoundEngine.tick()
                }
                Spacer(Modifier.width(6.dp))
                TonePill("秋桂", AppSettings.season == "autumn") {
                    AppSettings.updateSeason("autumn")
                    SoundEngine.tick()
                }
                Spacer(Modifier.width(6.dp))
                TonePill("冬雪", AppSettings.season == "winter") {
                    AppSettings.updateSeason("winter")
                    SoundEngine.tick()
                }
            }
            Spacer(Modifier.height(14.dp))
            val bg = AppSettings.bgPath
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bg != null) {
                    val bmp = remember(bg) { decodeSampledBitmap(bg, 256)?.asImageBitmap() }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                } else {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x14000000)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🖼", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (bg != null) "已设置自定义背景" else "自定义背景",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                    )
                    Spacer(Modifier.height(3.dp))
                    Hint(if (bg != null) "点右侧可以换一张" else "选一张让心情舒服的图，作为整个 App 的背景")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "选择",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SunsetBrush)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                        .bounceClickable { picker.launch("image/*") },
                )
                if (bg != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "清除",
                        fontSize = 13.sp,
                        color = SubInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x14000000))
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .bounceClickable { AppSettings.updateBackground(null) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("背景模糊 ${(AppSettings.blur * 100).toInt()}%", fontSize = 14.sp, color = Ink)
            Slider(value = AppSettings.blur, onValueChange = { AppSettings.updateBlur(it) })
            Spacer(Modifier.height(6.dp))
            Text("背景遮罩不透明度 ${(AppSettings.dim * 100).toInt()}%", fontSize = 14.sp, color = Ink)
            Text("（越高文字越清晰，越低背景越通透）", fontSize = 11.sp, color = SubInk)
            Slider(
                value = AppSettings.dim,
                onValueChange = { AppSettings.updateDim(it) },
                valueRange = 0.30f..0.95f,
            )
        }

        Spacer(Modifier.height(14.dp))

        // 交互
        SoftCard {
            Text("交互", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text("音效音量 ${(AppSettings.sfxVolume * 100).toInt()}%", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Slider(
                value = AppSettings.sfxVolume,
                onValueChange = { AppSettings.updateSfxVolume(it) },
                onValueChangeFinished = { SoundEngine.tick() },
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("震动强度", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Hint("点击、捏泡泡、敲木鱼时的震动")
                }
                TonePill("关", AppSettings.hapticsLevel == "off") { AppSettings.updateHapticsLevel("off") }
                Spacer(Modifier.width(6.dp))
                TonePill("轻柔", AppSettings.hapticsLevel == "light") {
                    AppSettings.updateHapticsLevel("light")
                    SoundEngine.tick()
                }
                Spacer(Modifier.width(6.dp))
                TonePill("有力", AppSettings.hapticsLevel == "strong") {
                    AppSettings.updateHapticsLevel("strong")
                    SoundEngine.tick()
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("木鱼音色", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Hint("点一下即可试听切换")
                }
                TonePill("清脆", AppSettings.muyuTone == "crisp") {
                    AppSettings.updateMuyuTone("crisp")
                    SoundEngine.muyu()
                }
                Spacer(Modifier.width(6.dp))
                TonePill("浑厚", AppSettings.muyuTone == "warm") {
                    AppSettings.updateMuyuTone("warm")
                    SoundEngine.muyu()
                }
                Spacer(Modifier.width(6.dp))
                TonePill("悠远", AppSettings.muyuTone == "soft") {
                    AppSettings.updateMuyuTone("soft")
                    SoundEngine.muyu()
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("呼吸钵音引导", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Hint("呼吸练习每个阶段切换时轻响一声钵音")
                }
                Switch(
                    checked = AppSettings.bellGuide,
                    onCheckedChange = {
                        AppSettings.updateBellGuide(it)
                        SoundEngine.tick()
                    },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 交流
        SoftCard {
            Text("交流", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("QQ交流群", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Spacer(Modifier.height(3.dp))
                    Hint("群号 948634109，点右侧一键加群")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "加群",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SunsetBrush)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                        .bounceClickable {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(QQ_GROUP_URL))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("qqgroup", QQ_GROUP_URL))
                                Toast.makeText(context, "群链接已复制，可到浏览器或QQ打开", Toast.LENGTH_SHORT).show()
                            }
                        },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 数据
        SoftCard {
            Text("数据", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("备份与还原", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Spacer(Modifier.height(3.dp))
            Hint("把敲木鱼、捏泡泡、心情记录和全部设置导出成一个文件；换手机或误删数据时可以还原")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SunsetBrush)
                        .bounceClickable { exportBackup.launch("cloudrest_backup.json") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "导出备份",
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 11.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ChipBg)
                        .bounceClickable { importBackup.launch(arrayOf("*/*")) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "还原备份",
                        fontSize = 13.sp,
                        color = Ink,
                        modifier = Modifier.padding(vertical = 11.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Hint("提示：还原会覆盖当前数据，建议先导出一份现用备份")
        }

        Spacer(Modifier.height(14.dp))

        // 诊断
        SoftCard {
            Text("诊断", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("运行状态", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Spacer(Modifier.height(3.dp))
            var crash by remember { mutableStateOf(CrashGuard.lastCrash(context)) }
            val c = crash
            if (c == null) {
                Hint("一切正常，没有发现崩溃记录 ☁️")
            } else {
                val firstLine = c.lineSequence().firstOrNull()?.removePrefix("time: ") ?: ""
                Hint("上次异常：$firstLine")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SunsetBrush)
                            .bounceClickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("crash", c))
                                Toast.makeText(context, "日志已复制，可粘贴反馈", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "复制日志",
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 11.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ChipBg)
                            .bounceClickable {
                                CrashGuard.clear(context)
                                crash = null
                                Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "清除记录",
                            fontSize = 13.sp,
                            color = Ink,
                            modifier = Modifier.padding(vertical = 11.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 关于
        SoftCard {
            Text("关于", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(10.dp))
            val version = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1.0"
                } catch (_: Exception) {
                    "1.1.0"
                }
            }
            Text("云朵休息室 v$version", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("作者：青柠不酸只甜", fontSize = 14.sp, color = SubInk)
            Spacer(Modifier.height(6.dp))
            Text(
                "Bug 反馈：QQ 2892546640（点击复制）",
                fontSize = 14.sp,
                color = RosePink,
                modifier = Modifier.bounceClickable {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("qq", "2892546640"))
                    Toast.makeText(context, "QQ 号已复制", Toast.LENGTH_SHORT).show()
                },
            )
            Spacer(Modifier.height(10.dp))
            Hint("自然声与短音效均为真实录音采样（Wikimedia Commons：Mijesty / Luftrum / nille / Glaneur de sons / Jorge Stolfi / nevit / Amada44 / Styroks / Sharelk / Darklanlan / YanikB / Mathieu Kappler / Thore，CC0 · CC BY · CC BY-SA）；新增自然声：溪流（jackthemurray，CC0）、夜虫（Glaneur de sons，CC BY）、雪落（YanikB，CC BY）、咖啡馆（thore / Mathieu Kappler 剪辑合成）；木鱼敲击音为真实采样，音色可在「交互」里切换；Ciallo 语音为网络热门素材（锁车音效分享平台网友分享），仅作学习交流。愿你在这里歇得舒服。")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TonePill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg: Brush = if (selected) SunsetBrush else SolidColor(ChipBg)
    Text(
        text,
        fontSize = 13.sp,
        color = if (selected) Color.White else SubInk,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 13.dp, vertical = 8.dp)
            .bounceClickable(onClick),
    )
}

/** QQ交流群链接（点击跳转） */
private const val QQ_GROUP_URL = "https://qun.qq.com/universal-share/share?ac=1&authKey=b8p514GhQ3%2FPtghR%2F8R5DfZD26lit934hKoCkR3jN8HQ%2FBlaJAHX5gfWMaczugaS&busi_data=eyJncm91cENvZGUiOiI5NDg2MzQxMDkiLCJ0b2tlbiI6Iks1QTZaQ0l4YUY4SW9WNzVqc1JRU25TazExMHRBVk53bEhncFUxL0labmhjWVhueVp1M1ZTNXJZWFNFeXJJL3UiLCJ1aW4iOiIyODkyNTQ2NjQwIn0%3D&data=lgfJc1LlkmRBMX5rhgPzlS7eDk-9lbkdK-XzbAyR6kbQnNuq2dt9DB8tgVNxs9JpHaFkoOkhrbQTe14sKgg8KQ&svctype=4&tempid=h5_group_info"