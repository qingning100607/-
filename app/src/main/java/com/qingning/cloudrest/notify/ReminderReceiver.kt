package com.qingning.cloudrest.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.qingning.cloudrest.MainActivity
import com.qingning.cloudrest.R
import com.qingning.cloudrest.ui.AppSettings

/** 每日提醒接收器：到点发通知；开机后自动重新排程 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AppSettings.init(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON" -> {
                // 设备重启后闹钟会失效，这里补回来
                ReminderScheduler.apply(context)
            }
            else -> {
                if (!AppSettings.reminderOn) return
                show(context)
                // setInexactRepeating 在部分 ROM 上会被清理，每次触发后补一次排程
                ReminderScheduler.schedule(context)
            }
        }
    }

    private fun show(context: Context) {
        ReminderScheduler.ensureChannel(context)
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val open = PendingIntent.getActivity(
            context, 11,
            Intent(context, MainActivity::class.java)
                .putExtra("open", "mood")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify_cloud)
            .setContentTitle("今天过得怎么样？")
            .setContentText("花 10 秒记一笔心情吧 ☁️")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            mgr.notify(NOTIFY_ID, n)
        } catch (_: Exception) {
            // 忽略
        }
    }

    companion object {
        const val NOTIFY_ID = 5202
    }
}