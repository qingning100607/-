package com.qingning.cloudrest.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.qingning.cloudrest.MainActivity
import com.qingning.cloudrest.R
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.audio.SoundService
import com.qingning.cloudrest.audio.SoundType
import com.qingning.cloudrest.data.Store
import com.qingning.cloudrest.ui.screens.FORTUNE_SLIPS

/** 桌面小组件：今日云签 + 功德数 + 一键雨声 */
class CloudWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_RAIN = "com.qingning.cloudrest.widget.RAIN"

        /** 刷新所有小组件（在开屏、切换声音、抽签后调用） */
        fun refresh(context: Context) {
            val ctx = context.applicationContext
            try {
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, CloudWidget::class.java))
                if (ids.isEmpty()) return
                ids.forEach { mgr.updateAppWidget(it, buildViews(ctx)) }
            } catch (_: Exception) {
            }
        }

        private fun buildViews(ctx: Context): RemoteViews {
            Store.init(ctx)
            val v = RemoteViews(ctx.packageName, R.layout.widget_cloud)
            val fi = Store.fortuneOf(Store.todayKey())
            v.setTextViewText(
                R.id.widget_fortune,
                if (fi >= 0 && fi < FORTUNE_SLIPS.size) "🎋 ${FORTUNE_SLIPS[fi].level} · ${FORTUNE_SLIPS[fi].title}"
                else "🎋 今日云签待抽取",
            )
            v.setTextViewText(R.id.widget_karma, "✨ 功德 ${Store.karma}")
            val rainOn = SoundEngine.channelOn[SoundType.RAIN] == true
            v.setTextViewText(R.id.widget_rain, if (rainOn) "🌧 雨声中 · 点此停" else "🌧 点我开雨声")
            v.setOnClickPendingIntent(
                R.id.widget_rain,
                PendingIntent.getBroadcast(
                    ctx, 1,
                    Intent(ctx, CloudWidget::class.java).setAction(ACTION_RAIN),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            v.setOnClickPendingIntent(
                R.id.widget_fortune,
                PendingIntent.getActivity(
                    ctx, 2,
                    Intent(ctx, MainActivity::class.java).putExtra("open", "fortune"),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            v.setOnClickPendingIntent(
                R.id.widget_karma,
                PendingIntent.getActivity(
                    ctx, 3,
                    Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            v.setOnClickPendingIntent(
                R.id.widget_title,
                PendingIntent.getActivity(
                    ctx, 4,
                    Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            return v
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, buildViews(context)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_RAIN) {
            SoundEngine.init(context.applicationContext)
            SoundEngine.toggle(SoundType.RAIN)
            SoundService.update(context.applicationContext)
            refresh(context)
        }
    }
}