package com.qingning.cloudrest

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 崩溃看护：把未捕获异常的堆栈写到本地，下次启动可在设置「诊断」里查看 / 复制 */
object CrashGuard {

    private const val FILE_NAME = "crash_last.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                File(app.filesDir, FILE_NAME).writeText(
                    "time: $stamp\nthread: ${thread.name}\n\n${sw.toString()}"
                )
            } catch (_: Throwable) {
            }
            if (prev != null) {
                prev.uncaughtException(thread, e)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    fun lastCrash(context: Context): String? = try {
        val f = File(context.filesDir, FILE_NAME)
        if (f.exists()) f.readText() else null
    } catch (_: Exception) {
        null
    }

    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (_: Exception) {
        }
    }
}