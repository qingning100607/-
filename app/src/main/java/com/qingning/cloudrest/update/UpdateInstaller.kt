package com.qingning.cloudrest.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 应用内下载并安装新版 APK：设置页与主页提示共用同一套逻辑 */
object UpdateInstaller {

    /** 下载到 cacheDir/update/cloudrest-update.apk；返回文件（失败返回 null） */
    suspend fun download(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "update").apply { mkdirs() }
            val out = File(dir, "cloudrest-update.apk")
            if (out.exists()) out.delete()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "CloudRest-Android")
            }
            conn.connect()
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { os ->
                    val buf = ByteArray(64 * 1024)
                    var got = 0L
                    var n = input.read(buf)
                    while (n > 0) {
                        os.write(buf, 0, n)
                        got += n
                        if (total > 0) onProgress((got * 100 / total).toInt().coerceIn(0, 100))
                        n = input.read(buf)
                    }
                    os.flush()
                }
            }
            conn.disconnect()
            if (out.exists() && out.length() > 100 * 1024) out else null
        } catch (_: Exception) {
            null
        }
    }

    /** 拉起系统安装器；未授权「安装未知应用」时先引导去设置 */
    fun install(context: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(context, "请先允许「安装未知应用」再回来点一次", Toast.LENGTH_LONG).show()
            try {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                // 忽略
            }
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (_: Exception) {
            Toast.makeText(context, "没找到安装器，可以到浏览器手动安装", Toast.LENGTH_SHORT).show()
        }
    }
}
