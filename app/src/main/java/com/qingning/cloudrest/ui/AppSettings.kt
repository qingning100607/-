package com.qingning.cloudrest.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qingning.cloudrest.data.Prefs

/** 应用设置：自定义背景、模糊、遮罩、触感。全部持久化 + Compose 响应式。 */
object AppSettings {

    private var prefs: SharedPreferences? = null

    /** 自定义背景图片路径（应用私有目录） */
    var bgPath: String? by mutableStateOf(null)
        private set

    /** 背景模糊强度 0..1 → 映射 0..80dp */
    var blur: Float by mutableStateOf(0.55f)
        private set

    /** 背景遮罩不透明度 0.30..0.95（越高越"实"，文字越清晰） */
    var dim: Float by mutableStateOf(0.72f)
        private set

    /** 震动强度：off / light / strong（与音效音量分开控制） */
    var hapticsLevel: String by mutableStateOf("strong")
        private set

    /** 触感反馈开关（由震动强度推导：非 off 即开启） */
    val hapticsOn: Boolean get() = hapticsLevel != "off"

    /** 木鱼音色：crisp=清脆 / warm=浑厚 / soft=悠远 */
    var muyuTone: String by mutableStateOf("crisp")
        private set

    /** 音效音量 0..1（与震动分开控制） */
    var sfxVolume: Float by mutableStateOf(1f)
        private set

    /** 夜间深色模式 */
    var deepNight: Boolean by mutableStateOf(false)
        private set

    /** 季节主题：off / spring春樱 / summer夏夜 / autumn秋桂 / winter冬雪 */
    var season: String by mutableStateOf("off")
        private set

    /** 自动夜间：22:00 后自动切深色，早晨自动切回 */
    var autoNight: Boolean by mutableStateOf(false)
        private set

    /** 呼吸阶段钵音引导 */
    var bellGuide: Boolean by mutableStateOf(true)
        private set

    /** 每日心情提醒开关 */
    var reminderOn: Boolean by mutableStateOf(false)
        private set

    /** 提醒时间：小时 0..23 */
    var reminderHour: Int by mutableStateOf(21)
        private set

    /** 提醒时间：分钟 0..59 */
    var reminderMinute: Int by mutableStateOf(0)
        private set

    /** 用户已忽略的更新版本号（主页横幅点「忽略」后不再提示） */
    var ignoredUpdate: String? by mutableStateOf(null)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("cloud_rest_settings", Context.MODE_PRIVATE)
        refresh()
    }

    /** 重新从本地读取全部设置（导入备份后调用） */
    fun refresh() {
        val p = prefs ?: return
        bgPath = Prefs.getString(p, "bg_path", null)
        blur = Prefs.getFloat(p, "blur", 0.55f)
        dim = Prefs.getFloat(p, "dim", 0.72f)
        hapticsLevel = Prefs.getString(p, "haptics_level", if (Prefs.getBoolean(p, "haptics", true)) "strong" else "off") ?: "strong"
        muyuTone = Prefs.getString(p, "muyu_tone", "crisp") ?: "crisp"
        sfxVolume = Prefs.getFloat(p, "sfx_volume", 1f)
        deepNight = Prefs.getBoolean(p, "deep_night", false)
        season = Prefs.getString(p, "season", "off") ?: "off"
        autoNight = Prefs.getBoolean(p, "auto_night", false)
        bellGuide = Prefs.getBoolean(p, "bell_guide", true)
        reminderOn = Prefs.getBoolean(p, "reminder_on", false)
        reminderHour = Prefs.getInt(p, "reminder_hour", 21).coerceIn(0, 23)
        reminderMinute = Prefs.getInt(p, "reminder_minute", 0).coerceIn(0, 59)
        ignoredUpdate = Prefs.getString(p, "ignored_update", null)
    }

    fun updateBackground(path: String?) {
        bgPath = path
        prefs?.edit()?.apply {
            if (path == null) remove("bg_path") else putString("bg_path", path)
        }?.apply()
    }

    fun updateBlur(v: Float) {
        blur = v
        prefs?.edit()?.putFloat("blur", v)?.apply()
    }

    fun updateDim(v: Float) {
        dim = v
        prefs?.edit()?.putFloat("dim", v)?.apply()
    }

    fun updateHaptics(v: Boolean) {
        updateHapticsLevel(if (v) "strong" else "off")
    }

    fun updateHapticsLevel(v: String) {
        hapticsLevel = v
        prefs?.edit()?.putString("haptics_level", v)?.putBoolean("haptics", v != "off")?.apply()
    }

    fun updateMuyuTone(v: String) {
        muyuTone = v
        prefs?.edit()?.putString("muyu_tone", v)?.apply()
    }

    fun updateSfxVolume(v: Float) {
        sfxVolume = v
        prefs?.edit()?.putFloat("sfx_volume", v)?.apply()
    }

    fun updateDeepNight(v: Boolean) {
        deepNight = v
        prefs?.edit()?.putBoolean("deep_night", v)?.apply()
    }

    fun updateSeason(v: String) {
        season = v
        prefs?.edit()?.putString("season", v)?.apply()
    }

    fun updateAutoNight(v: Boolean) {
        autoNight = v
        prefs?.edit()?.putBoolean("auto_night", v)?.apply()
    }

    fun updateBellGuide(v: Boolean) {
        bellGuide = v
        prefs?.edit()?.putBoolean("bell_guide", v)?.apply()
    }

    fun updateReminderOn(v: Boolean) {
        reminderOn = v
        prefs?.edit()?.putBoolean("reminder_on", v)?.apply()
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        reminderHour = hour.coerceIn(0, 23)
        reminderMinute = minute.coerceIn(0, 59)
        prefs?.edit()
            ?.putInt("reminder_hour", reminderHour)
            ?.putInt("reminder_minute", reminderMinute)
            ?.apply()
    }

    /** 记录/清除已忽略的更新版本 */
    fun updateIgnoredUpdate(v: String?) {
        ignoredUpdate = v
        prefs?.edit()?.apply {
            if (v == null) remove("ignored_update") else putString("ignored_update", v)
        }?.apply()
    }

    /** 自动夜间：跨过 22:00 / 7:00 边界时自动切换一次（窗口内尊重手动调整） */
    fun autoNightTick() {
        if (!autoNight) return
        val p = prefs ?: return
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val want = if (h >= 22 || h < 7) "night" else "day"
        if (Prefs.getString(p, "auto_night_state", "") != want) {
            p.edit().putString("auto_night_state", want).apply()
            if (want == "night" && !deepNight) updateDeepNight(true)
            if (want == "day" && deepNight) updateDeepNight(false)
        }
    }
}

/** 转场状态标记：转场动画进行期间冻结动态背景的重绘，避免掉帧 */
object UiTransit {
    @Volatile var busy: Boolean = false
}
