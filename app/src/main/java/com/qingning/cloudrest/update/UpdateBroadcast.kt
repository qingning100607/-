package com.qingning.cloudrest.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qingning.cloudrest.ui.AppSettings

/**
 * 全局「发现新版本」广播：应用启动后台静默检测一次，主页横幅与设置页共用结果。
 * 已忽略的版本号持久化在 AppSettings.ignoredUpdate。
 */
object UpdateBroadcast {

    /** 最近一次检测到的「有新版本」结果（无则 null） */
    var found by mutableStateOf<UpdateChecker.Result?>(null)
        private set

    /** 是否正在检测 */
    var checking by mutableStateOf(false)
        private set

    private var lastCheckAt = 0L

    /** 是否有比 current 更新、且未被用户忽略的版本 */
    fun hasNew(current: String): Boolean {
        val r = found ?: return false
        if (!r.ok || !r.hasUpdate) return false
        return r.latest != AppSettings.ignoredUpdate
    }

    /** 记录一次手动检测到的新版本（设置页手动检查时调用） */
    fun noteFound(r: UpdateChecker.Result?) {
        if (r != null && r.ok && r.hasUpdate) found = r
    }

    /** 忽略某个版本（主页横幅「忽略」） */
    fun ignore(version: String) {
        AppSettings.updateIgnoredUpdate(version)
    }

    /** 静默检测：同一次启动内只跑一次，且距上次至少 30 分钟 */
    suspend fun checkThrottled(current: String) {
        if (checking || current.isBlank()) return
        val now = System.currentTimeMillis()
        if (lastCheckAt != 0L && now - lastCheckAt < 30 * 60_000L) return
        lastCheckAt = now
        checking = true
        try {
            val r = UpdateChecker.check(current)
            if (r.ok && r.hasUpdate) found = r
        } finally {
            checking = false
        }
    }
}
