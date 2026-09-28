package com.qingning.cloudrest.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qingning.cloudrest.ui.AppSettings
import java.util.Calendar

/**
 * 每日心情提醒：用 AlarmManager 每天定时唤醒一次，发一条本地通知。
 * 用 setInexactRepeating 而不是精确闹钟 —— 不需要 SCHEDULE_EXACT_ALARM 权限，
 * 对「每天提醒一下」这种场景完全够用，也省电。
 */
object ReminderScheduler {

    private const val REQ = 5201
    private const val ACTION_REMIND = "com.qingning.cloudrest.action.REMIND"

    /** 根据当前设置应用/取消提醒 */
    fun apply(context: Context) {
        if (AppSettings.reminderOn) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        ensureChannel(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, AppSettings.reminderHour)
            set(Calendar.MINUTE, AppSettings.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        try {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending(context))
        } catch (_: Exception) {
            // 个别 ROM 限制后台闹钟，失败也不影响 App 使用
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            am.cancel(pending(context))
        } catch (_: Exception) {
            // 忽略
        }
    }

    /** 提前建好通知渠道，保证用户在系统设置里能看到它 */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "每日心情提醒", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "每天提醒你记一笔今天的心情"
            mgr.createNotificationChannel(ch)
        } catch (_: Exception) {
            // 忽略
        }
    }

    private fun pending(context: Context): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context, REQ, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    const val CHANNEL_ID = "cloudrest_reminder"
}