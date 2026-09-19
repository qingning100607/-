package com.qingning.cloudrest.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.qingning.cloudrest.MainActivity
import com.qingning.cloudrest.R

/** 前台服务：锁屏/后台持续播放自然声 + 通知栏控制 */
class SoundService : Service() {

    companion object {
        private const val CHANNEL_ID = "cloudrest_playback"
        private const val NOTIFY_ID = 4104
        const val ACTION_STOP = "com.qingning.cloudrest.action.STOP"

        /** 根据播放状态启动/刷新或停止前台服务 */
        fun update(context: Context) {
            val ctx = context.applicationContext
            val intent = Intent(ctx, SoundService::class.java)
            if (SoundEngine.anyOn()) {
                try {
                    if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(intent)
                    else ctx.startService(intent)
                } catch (_: Exception) {
                }
            } else {
                try {
                    ctx.stopService(intent)
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            SoundEngine.stopAll()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!SoundEngine.anyOn()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFY_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFY_ID, n)
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "自然声播放", NotificationManager.IMPORTANCE_LOW)
            ch.description = "在通知栏显示自然声播放状态，可一键停止"
            mgr.createNotificationChannel(ch)
        }
        val labels = SoundEngine.activeLabels()
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, SoundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify_cloud)
            .setContentTitle("云朵休息室 · 自然声")
            .setContentText("正在播放：" + labels.joinToString(" · "))
            .setContentIntent(open)
            .addAction(0, "全部停止", stop)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
