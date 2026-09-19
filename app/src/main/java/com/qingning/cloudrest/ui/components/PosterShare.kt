package com.qingning.cloudrest.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.qingning.cloudrest.data.Store
import java.io.File

/** 生成"治愈战绩"分享海报并调起系统分享 */
fun sharePoster(context: Context) {
    try {
        val bmp = buildPosterBitmap()
        val dir = File(context.cacheDir, "posters").apply { mkdirs() }
        val f = File(dir, "cloudrest_poster.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "云朵休息室 · 我的治愈战绩 ☁️")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "分享战绩").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        Toast.makeText(context, "海报生成失败，稍后再试", Toast.LENGTH_SHORT).show()
    }
}

private fun buildPosterBitmap(): Bitmap {
    val w = 1080
    val h = 1580
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)

    // 晚霞渐变背景
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFFA78BDA.toInt(), 0xFFF0A8BC.toInt(), 0xFFFFC9A3.toInt()),
            null, Shader.TileMode.CLAMP,
        )
    }
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    // 白色圆角卡片
    val card = Paint().apply { color = 0xF7FFFFFF.toInt() }
    c.drawRoundRect(RectF(70f, 230f, (w - 70).toFloat(), (h - 210).toFloat()), 56f, 56f, card)

    val cx = w / 2f
    fun text(s: String, size: Float, color: Int, y: Float, bold: Boolean = false) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        c.drawText(s, cx, y, p)
    }

    text("☁️", 110f, 0xFF4A3F4F.toInt(), 430f)
    text("云朵休息室", 84f, 0xFF4A3F4F.toInt(), 565f, bold = true)
    text("· 我的治愈战绩 ·", 40f, 0xFF8A7E90.toInt(), 635f)

    text("累计功德", 36f, 0xFF8A7E90.toInt(), 790f)
    text("${Store.karma}", 76f, 0xFFC9A227.toInt(), 880f, bold = true)

    text("打卡天数", 32f, 0xFF8A7E90.toInt(), 995f)
    text("${Store.karmaDays()} 天", 54f, 0xFF4A3F4F.toInt(), 1060f, bold = true)

    text("解压次数", 32f, 0xFF8A7E90.toInt(), 1155f)
    text("${Store.bubbles + Store.iceBroken + Store.fireworks + Store.shredded} 次", 54f, 0xFF4A3F4F.toInt(), 1220f, bold = true)

    text("心情记录 ${Store.moodDays()} 天 · 慢慢来，比较快", 36f, 0xFF8A7E90.toInt(), 1290f)
    text("— 青柠不酸只甜 —", 32f, 0xFFA78BDA.toInt(), 1350f)

    return bmp
}