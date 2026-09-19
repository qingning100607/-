package com.qingning.cloudrest.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("cloud_rest_settings", Context.MODE_PRIVATE)
        refresh()
    }

    /** 重新从本地读取全部设置（导入备份后调用） */
    fun refresh() {
        val p = prefs ?: return
        bgPath = p.getString("bg_path", null)
        blur = p.getFloat("blur", 0.55f)
        dim = p.getFloat("dim", 0.72f)
        hapticsLevel = p.getString("haptics_level", if (p.getBoolean("haptics", true)) "strong" else "off") ?: "strong"
        muyuTone = p.getString("muyu_tone", "crisp") ?: "crisp"
        sfxVolume = p.getFloat("sfx_volume", 1f)
        deepNight = p.getBoolean("deep_night", false)
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
}

/** 转场状态标记：转场动画进行期间冻结动态背景的重绘，避免掉帧 */
object UiTransit {
    @Volatile var busy: Boolean = false
}
